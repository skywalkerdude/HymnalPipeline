package com.hymnsmobile.pipeline.hymnalnet;

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
      fetchHymn(key, true);
    }
  }

  private void fetchHymn(HymnalNetKey key, boolean isRoot)
      throws InterruptedException, IOException, URISyntaxException {
    LOGGER.fine(String.format("Fetching %s", key));
    if (hymnalNetJsons.stream().anyMatch(hymn -> hymn.getKey().equals(key))) {
      LOGGER.fine(String.format("%s already exists. Skipping...", key));
      return;
    }

    Optional<HymnalNetJson> hymn = getHymnalNet(key, isRoot);
    if (hymn.isEmpty()) {
      LOGGER.warning(String.format("Unable to fetch %s", key));
      return;
    }
    this.hymnalNetJsons.add(hymn.get());

    // Also fetch all related songs
    List<HymnalNetKey> relatedSongs = ImmutableList.<HymnalNetKey>builder()
        .addAll(converter.getRelated(MetaDatumType.LANGUAGES, hymn.get(), errors))
        .addAll(converter.getRelated(MetaDatumType.RELEVANT, hymn.get(), errors))
        .build();
    LOGGER.fine(String.format("Found %d related songs: %s", relatedSongs.size(), relatedSongs));
    for (HymnalNetKey relatedSong : relatedSongs) {
      fetchHymn(relatedSong, false);
    }
  }

  /**
   * Fetches the hymn referenced by the song, unless it doesn't exist, in which case, return
   * {@link Optional#empty()}.
   * @param key key to fetch
   * @param isRoot whether it's a root request (versus a linked request), which is when the song
   *               that's being fetched is linked (language/relevant) from another song during the
   *               graph traversal.
   */
  private Optional<HymnalNetJson> getHymnalNet(HymnalNetKey key, boolean isRoot)
      throws IOException, InterruptedException, URISyntaxException {
    // Ensuring that hymn type parses correctly should already be done at this point.
    HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
    String hymnNumber = key.getHymnNumber();

    HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(buildUri(key)).build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      // If there is a max number, that means the song *should* be continuous meaning a missing song
      // should theoretically be an error.
      //
      // Another case where we log an error is if the request is a branch request (i.e. not root).
      // This means that the request originated not from the pipeline, but rather is linked from
      // another song, meaning it's a language and/or relevant song of that song. In this case, we
      // expect the song to exist, and when it doesn't we log an error.
      //
      // In both cases, the error is probably on Hymnal.net's end though, so not much we can do here
      // other than log it and monitor it.
      if (hymnType.maxNumber.isPresent() || !isRoot) {
        errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING)
            .setMessage(String.format("%s returned with status code: %d", buildUri(key),
                response.statusCode())).build());
      }
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
