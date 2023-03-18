package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.hymnalnet.BlockList.BLOCK_LIST;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class Fetcher {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final String SCHEME = "https";
  private static final String AUTHORITY = "hymnalnetapi.herokuapp.com";
  private static final String PATH = "/v2/hymn/%s/%s";
  private static final String CHECK_EXISTS = "check_exists=true";

  private final HttpClient client;
  private final Converter converter;
  private final ImmutableList<HymnalNetKey> songsToFetch;

  private final Set<Hymn> hymns;
  private final Set<HymnalNetJson> hymnalNetJsons;

  @Inject
  public Fetcher(HttpClient client, Converter converter,
      ImmutableList<HymnalNetKey> songsToFetch,
      @HymnalNet Set<Hymn> hymns,
      @HymnalNet Set<HymnalNetJson> hymnalNetJsons) {
    this.client = client;
    this.converter = converter;
    this.hymns = hymns;
    this.hymnalNetJsons = hymnalNetJsons;
    this.songsToFetch = songsToFetch;
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  public void fetchHymns() throws InterruptedException, IOException, URISyntaxException {
    for (HymnalNetKey key : songsToFetch) {
      HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
      Optional<SongReference> songReferenceOptional = converter.toSongReference(key);
      if (songReferenceOptional.isEmpty()) {
        continue;
      }
      SongReference songReference = songReferenceOptional.get();
      fetchHymn(songReference);
      if (hymnType == HymnType.CHINESE) {
        fetchHymn(songReference.toBuilder()
            .setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED).build());
      }
      if (hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
        fetchHymn(songReference.toBuilder()
            .setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED)
            .build());
      }
    }
  }

  private void fetchHymn(SongReference songReference)
      throws InterruptedException, IOException, URISyntaxException {
    LOGGER.info(String.format("Fetching %s", songReference));
    if (hymns.stream().anyMatch(hymn -> hymn.getReference().equals(songReference))) {
      LOGGER.info(String.format("%s already exists. Skipping...", songReference));
      return;
    }

    if (BLOCK_LIST.contains(songReference)) {
      LOGGER.info(String.format("%s contained in block list. Skipping...", songReference));
      return;
    }

    Optional<HymnalNetJson> hymnalNetJson = getHymnalNet(songReference);
    if (hymnalNetJson.isEmpty()) {
      LOGGER.warning(String.format("Fetching %s was unsuccessful", songReference));
      return;
    }
    this.hymnalNetJsons.add(hymnalNetJson.get());
    Optional<Hymn> hymnOptional = converter.toHymn(hymnalNetJson.get());
    if (hymnOptional.isEmpty()) {
      return;
    }
    Hymn hymn = hymnOptional.get();
    this.hymns.add(hymn);

    // Also fetch all related songs
    List<SongReference> relatedSongs = ImmutableList.<SongReference>builder()
        .addAll(
            hymn.getLanguagesList().stream().map(SongLink::getReference).collect(toImmutableList()))
        .addAll(
            hymn.getRelevantsList().stream().map(SongLink::getReference).collect(toImmutableList()))
        .build();
    LOGGER.info(String.format("Found %d related songs: %s", relatedSongs.size(), relatedSongs));
    for (SongReference relatedSong : relatedSongs) {
      fetchHymn(relatedSong);
    }
  }

  /**
   * Fetches the hymn referenced by the song, unless it doesn't exist, in which case, return
   * {@link Optional#empty()}.
   */
  private Optional<HymnalNetJson> getHymnalNet(SongReference songReference)
      throws IOException, InterruptedException, URISyntaxException {
    HymnalNetKey key = converter.toHymnalNetKey(songReference);
    HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
    String hymnNumber = key.getHymnNumber();

    HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(buildUri(key)).build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      LOGGER.warning(
          String.format("%s returned with status code: %d", buildUri(key), response.statusCode()));
      return Optional.empty();
    }

    HymnalNetJson.Builder builder = HymnalNetJson.newBuilder()
        .setKey(converter.toHymnalNetKey(songReference));
    try {
      JsonFormat.parser().merge(response.body(), builder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(
          String.format("Unable to parse %s/%s: %s", hymnType, hymnNumber, response.body()), e);
    }
    LOGGER.info(String.format("%s successfully fetched", key));
    return Optional.of(builder.build());
  }

  /**
   * Slightly hacky way to form the uri. Basically, this assumes that 'queryParams' is of the form
   * "?gb=1". To remove the "?", we need to take the substring. Otherwise, it will look like
   * "...v2/hymn/ts/330??check_exists=true".
   */
  private URI buildUri(HymnalNetKey key) throws URISyntaxException {
    return new URI(SCHEME, AUTHORITY, String.format(PATH, key.getHymnType(), key.getHymnNumber()),
        key.hasQueryParams() ? key.getQueryParams().substring(1) + "&" + CHECK_EXISTS
            : CHECK_EXISTS, null);
  }
}
