package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.hymnalnet.models.MetaDatum;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.hymnalnet.Converter.extractFromPath;

@HymnalNetPipelineScope
public class Fetcher {

  private enum FetchResult {
    /**
     * Fetch completed successfully.
     */
    SUCCESS,
    /**
     * Already fetched this song.
     */
    ALREADY_FETCHED,
    /**
     * Fetch returned non-200 response.
     */
    NON_200_RESPONSE,
    /**
     * Exception thrown during fetch.
     */
    FETCH_EXCEPTION
  }

  private static final Logger LOGGER = Logger.getGlobal();

  private static final String SCHEME = "https";
  private static final String AUTHORITY = "hymnalnetapi.herokuapp.com";
  private static final String PATH = "/v2/hymn/%s/%s";
  private static final String CHECK_EXISTS = "check_exists=true";

  private final HttpClient client;
  private final ImmutableList<HymnalNetKey> songsToFetch;

  private final Set<HymnalNetJson> hymnalNetJsons;
  private final Set<PipelineError> errors;

  @Inject
  public Fetcher(HttpClient client,
      ImmutableList<HymnalNetKey> songsToFetch,
      Set<HymnalNetJson> hymnalNetJsons,
      @HymnalNet Set<PipelineError> errors) {
    this.client = client;
    this.hymnalNetJsons = hymnalNetJsons;
    this.songsToFetch = songsToFetch;
    this.errors = errors;
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  public void fetchHymns() {
    LOGGER.info("Starting fetch...");
    for (HymnalNetKey key : songsToFetch) {
      HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
      Fetcher.FetchResult fetchResult = fetchHymn(key);
      if (fetchResult == FetchResult.FETCH_EXCEPTION) {
        this.errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.ERROR)
                .setErrorType(ErrorType.FETCH_EXCEPTION)
                .addMessages(String.format("Exception thrown during fetch: %s", key))
                .build());
        return;
      }

      if (fetchResult == FetchResult.NON_200_RESPONSE && hymnType.maxNumber.isPresent()) {
        // If there is a max number, that means the song *should* be continuous meaning a missing
        // song should theoretically be an error.
        //
        // The error is probably on Hymnal.net's end though, so not much we can do here other than
        // log it and monitor it.
        this.errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.ERROR)
                .setErrorType(ErrorType.FETCH_ERROR)
                .addMessages(String.format("Failed to fetch: %s", key))
                .build());
      }
    }
    LOGGER.info("Fetch Complete");
  }

  private FetchResult fetchHymn(HymnalNetKey key) {
    LOGGER.fine(String.format("Fetching %s", key));
    if (hymnalNetJsons.stream().anyMatch(hymn -> hymn.getKey().equals(key))) {
      LOGGER.fine(String.format("%s already exists. Skipping...", key));
      return FetchResult.ALREADY_FETCHED;
    }

    final Optional<HymnalNetJson> hymn;
    try {
      hymn = getHymnalNet(key);
    } catch (IOException | URISyntaxException | InterruptedException e) {
      return FetchResult.FETCH_EXCEPTION;
    }
    if (hymn.isEmpty()) {
      LOGGER.warning(String.format("Unable to fetch %s", key));
      return FetchResult.NON_200_RESPONSE;
    }

    // Need to add the hymn here first as a terminal case
    this.hymnalNetJsons.add(hymn.get());

    // Go through ahd try to fetch the related songs, removing them from the list if the fetch
    // for some reason doesn't succeed.
    HymnalNetJson.Builder builder = hymn.get().toBuilder();
    for (int i = 0; i < builder.getMetaDataCount(); i++) {
      MetaDatum metaDatum = builder.getMetaDataList().get(i);
      String name = metaDatum.getName();
      if (MetaDatumType.LANGUAGES.jsonKeys.contains(name) ||
          MetaDatumType.RELEVANT.jsonKeys.contains(name)) {
        MetaDatum successfulFetches = fetchAndReturnSuccesses(metaDatum, key);
        if (successfulFetches.getDataList().isEmpty()) {
          builder.removeMetaData(i);
        } else {
          builder.setMetaData(i, successfulFetches);
        }
      }
    }

    // Basically this happens when there were songs that weren't able to be fetched, so those songs
    // were removed from the related songs list. Thus, we need to update the song to the new version
    if (!builder.build().equals(hymn.get())) {
      this.hymnalNetJsons.remove(hymn.get());
      this.hymnalNetJsons.add(builder.build());
    }
    return FetchResult.SUCCESS;
  }

  /**
   * Fetches the hymn referenced by the song, unless it doesn't exist, in which case, return
   * {@link Optional#empty()}.
   *
   * @param key key to fetch
   */
  private Optional<HymnalNetJson> getHymnalNet(HymnalNetKey key)
      throws IOException, URISyntaxException, InterruptedException {
    HttpResponse<String> response =
        client.send(HttpRequest.newBuilder().uri(buildUri(key)).build(), BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      return Optional.empty();
    }

    HymnalNetJson.Builder builder = HymnalNetJson.newBuilder().setKey(key);
    JsonFormat.parser().merge(response.body(), builder);
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

  private MetaDatum fetchAndReturnSuccesses(MetaDatum metaDatum, HymnalNetKey parent) {
    return metaDatum.toBuilder().clearData()
        .addAllData(
            metaDatum.getDataList().stream().filter(datum ->
                    extractFromPath(datum.getPath(), parent, errors)
                        .filter(relatedSong -> {
                          FetchResult fetchResult = fetchHymn(relatedSong);
                          if (fetchResult == FetchResult.FETCH_EXCEPTION) {
                            this.errors.add(
                                PipelineError.newBuilder()
                                    .setSeverity(Severity.ERROR)
                                    .setErrorType(ErrorType.FETCH_EXCEPTION)
                                    .addMessages(
                                        String.format("Exception thrown during fetch: %s, a related song of %s",
                                            relatedSong, parent))
                                    .build());
                            return false;
                          }
                          if (fetchResult == FetchResult.NON_200_RESPONSE) {
                            LOGGER.warning(
                                String.format("Failed to fetch: %s, a related song of %s", relatedSong,
                                    parent));
                            return false;
                          }
                          return true;
                        }).isPresent())
                .collect(toImmutableList()))
        .build();
  }
}
