package com.hymnsmobile.pipeline.hymnalnet;

import static com.hymnsmobile.pipeline.hymnalnet.BlockList.BLOCK_LIST;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
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

  private final Set<HymnalNetJson> hymnalNetJsons;
  private final Set<PipelineError> errors;

  @Inject
  public Fetcher(HttpClient client, Converter converter,
      ImmutableList<HymnalNetKey> songsToFetch,
      Set<HymnalNetJson> hymnalNetJsons,
      @HymnalNet Set<PipelineError> errors) {
    this.client = client;
    this.converter = converter;
    this.hymnalNetJsons = hymnalNetJsons;
    this.songsToFetch = songsToFetch;
    this.errors = errors;
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  public void fetchHymns() throws InterruptedException, IOException, URISyntaxException {
    for (HymnalNetKey key : songsToFetch) {
      fetchHymn(key);
    }
  }

  private void fetchHymn(HymnalNetKey key)
      throws InterruptedException, IOException, URISyntaxException {
    LOGGER.fine(String.format("Fetching %s", key));
    if (hymnalNetJsons.stream().anyMatch(hymn -> hymn.getKey().equals(key))) {
      LOGGER.fine(String.format("%s already exists. Skipping...", key));
      return;
    }

    if (BLOCK_LIST.contains(key)) {
      LOGGER.fine(String.format("%s contained in block list. Skipping...", key));
      return;
    }

    Optional<HymnalNetJson> hymn = getHymnalNet(key);
    if (hymn.isEmpty()) {
      errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING)
          .setMessage(String.format("Fetching %s was unsuccessful", key)).build());
      return;
    }
    this.hymnalNetJsons.add(hymn.get());

    // Also fetch all related songs
    List<HymnalNetKey> relatedSongs = ImmutableList.<HymnalNetKey>builder()
        .addAll(converter.getRelated(MetaDatumType.LANGUAGES.jsonKey, hymn.get()))
        .addAll(converter.getRelated(MetaDatumType.RELEVANT.jsonKey, hymn.get()))
        .build();
    LOGGER.fine(String.format("Found %d related songs: %s", relatedSongs.size(), relatedSongs));
    for (HymnalNetKey relatedSong : relatedSongs) {
      fetchHymn(relatedSong);
    }
  }

  /**
   * Fetches the hymn referenced by the song, unless it doesn't exist, in which case, return
   * {@link Optional#empty()}.
   */
  private Optional<HymnalNetJson> getHymnalNet(HymnalNetKey key)
      throws IOException, InterruptedException, URISyntaxException {
    HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
    String hymnNumber = key.getHymnNumber();

    HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(buildUri(key)).build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      LOGGER.warning(
          String.format("%s returned with status code: %d", buildUri(key), response.statusCode()));
      return Optional.empty();
    }

    HymnalNetJson.Builder builder = HymnalNetJson.newBuilder().setKey(key);
    try {
      JsonFormat.parser().merge(response.body(), builder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(
          String.format("Unable to parse %s/%s: %s", hymnType, hymnNumber, response.body()), e);
    }
    LOGGER.fine(String.format("%s successfully fetched", key));
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
