package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.hymnalnet.models.MetaDatum;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.utils.TextUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.hymnalnet.Converter.extractFromPath;

@HymnalNetPipelineScope
public class Fetcher {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final String SCHEME = "https";
  private static final String AUTHORITY = "hymnalnetapi.herokuapp.com";
  private static final String PATH = "/v2/hymn/%s/%s";
  private static final String CHECK_EXISTS = "check_exists=true";

  private final HttpClient client;
  private final ImmutableList<HymnalNetKey> songsToFetch;

  private final Set<HymnalNetJson> hymnalNetJsons;
  private final Set<PipelineError> errors;
  private final Set<HymnalNetKey> processed;

  @Inject
  public Fetcher(HttpClient client,
      ImmutableList<HymnalNetKey> songsToFetch,
      Set<HymnalNetJson> hymnalNetJsons,
      @HymnalNet Set<PipelineError> errors) {
    this.client = client;
    this.hymnalNetJsons = hymnalNetJsons;
    this.songsToFetch = songsToFetch;
    this.errors = errors;
    this.processed = new HashSet<>();
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  public void fetchHymns() {
    LOGGER.info("Starting fetch...");
    for (HymnalNetKey key : songsToFetch) {
      String keyString = key.hasQueryParams() ?
          String.format("%s/%s%s", key.getHymnType(), key.getHymnNumber(), key.getQueryParams()) :
          String.format("%s/%s", key.getHymnType(), key.getHymnNumber());

      HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
      FetchResult fetchResult = fetchHymn(key);

      if (fetchResult instanceof FetchResult.FetchException) {
        PipelineError.Builder error =
            PipelineError.newBuilder()
                .setSource(PipelineError.Source.HYMNAL_NET)
                 .setSeverity(Severity.ERROR)
                 .setErrorType(ErrorType.FETCH_EXCEPTION)
                 .addMessages(keyString);
        if (!TextUtil.isEmpty(((FetchResult.FetchException) fetchResult).exception.getMessage())) {
          error.addMessages(((FetchResult.FetchException) fetchResult).exception.getMessage());
        }
        this.errors.add(error.build());
        return;
      }

      if (fetchResult instanceof FetchResult.FetchFailure) {
        PipelineError.Builder error =
            PipelineError.newBuilder()
                .setSource(PipelineError.Source.HYMNAL_NET)
                .setSeverity(Severity.ERROR)
                .setErrorType(ErrorType.FETCH_ERROR)
                .addMessages(keyString)
                .addMessages(String.valueOf(((FetchResult.FetchFailure) fetchResult).responseCode));
        if (!TextUtil.isEmpty(((FetchResult.FetchFailure) fetchResult).responseBody)) {
          error.addMessages(((FetchResult.FetchFailure) fetchResult).responseBody);
        }
        this.errors.add(error.build());
        return;
      }

      if (fetchResult instanceof FetchResult.FetchNotFound && hymnType.maxNumber.isPresent()) {
        // If there is a max number, that means the song *should* be continuous meaning a missing
        // song should theoretically be an error.
        //
        // The error is probably on Hymnal.net's end though, so not much we can do here other than
        // log it and monitor it.
        PipelineError.Builder error =
            PipelineError.newBuilder()
                .setSource(PipelineError.Source.HYMNAL_NET)
                .setSeverity(Severity.ERROR)
                .setErrorType(ErrorType.FETCH_ERROR)
                .addMessages(keyString)
                .addMessages("404");
        this.errors.add(error.build());
      }
    }
    LOGGER.info("Fetch Complete");
  }

  private FetchResult fetchHymn(HymnalNetKey key) {
    LOGGER.fine(String.format("Fetching %s", key));

    List<HymnalNetJson> existing = hymnalNetJsons.stream().filter(hymn -> hymn.getKey().equals(key)).toList();
    if (existing.size() > 1) {
      throw new IllegalStateException("List too big. This shouldn't happen, as it indicates a code error.");
    }

    if (processed.contains(key) && !existing.isEmpty()) {
      return new FetchResult.AlreadySeen();
    }
    processed.add(key);

    if (!existing.isEmpty()) {
      LOGGER.fine(String.format("%s already exists in database. Not re-fetching, but re-fetching related songs", key));
      fetchRelated(key, existing.get(0));
      return new FetchResult.AlreadyStored();
    }

    HttpResponse<String> response;
    try {
      response = client.send(HttpRequest.newBuilder().uri(buildUri(key)).build(), BodyHandlers.ofString());
    } catch (IOException | InterruptedException | URISyntaxException e) {
      return new FetchResult.FetchException(e);
    }

    if (response.statusCode() == 400) {
      return new FetchResult.FetchNotFound();
    }

    if (response.statusCode() != 200) {
      return new FetchResult.FetchFailure(response.statusCode(), response.body());
    }

    HymnalNetJson.Builder builder = HymnalNetJson.newBuilder().setKey(key);
    try {
      JsonFormat.parser().merge(response.body(), builder);
    } catch (InvalidProtocolBufferException e) {
      return new FetchResult.FetchException(e);
    }
    LOGGER.fine(String.format("%s successfully fetched", key));
    HymnalNetJson hymnalNetJson = builder.build();

    // Need to add the hymn here first as a terminal case
    this.hymnalNetJsons.add(hymnalNetJson);

    fetchRelated(key, hymnalNetJson);

    return new FetchResult.FetchSuccess(builder.build());
  }

  private void fetchRelated(HymnalNetKey key, HymnalNetJson hymnalNetJson) {
    HymnalNetJson.Builder builder = hymnalNetJson.toBuilder();

    // Go through ahd try to fetch the related songs, removing them from the list if the fetch
    // for some reason doesn't succeed.
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
    if (!hymnalNetJson.equals(builder.build())) {
      this.hymnalNetJsons.remove(hymnalNetJson);
      this.hymnalNetJsons.add(builder.build());
    }
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
            metaDatum.getDataList().stream().filter(datum -> {
                    Optional<HymnalNetKey> key = extractFromPath(datum.getPath(), parent, errors)
                        .filter(relatedSong -> {
                          FetchResult fetchResult = fetchHymn(relatedSong);
                          if (fetchResult instanceof FetchResult.FetchException) {
                            PipelineError.Builder error =
                                PipelineError.newBuilder()
                                    .setSource(PipelineError.Source.HYMNAL_NET)
                                    .setSeverity(Severity.ERROR)
                                    .setErrorType(ErrorType.FETCH_EXCEPTION)
                                    .addMessages(
                                        String.format("Exception thrown during fetch: %s, a related song of %s",
                                                      relatedSong, parent));
                            if (!TextUtil.isEmpty(((FetchResult.FetchException) fetchResult).exception.getMessage())) {
                              error.addMessages(((FetchResult.FetchException) fetchResult).exception.getMessage());
                            }
                            this.errors.add(error.build());
                            return false;
                          }
                          if (fetchResult instanceof FetchResult.FetchFailure) {
                            PipelineError.Builder error =
                                PipelineError.newBuilder()
                                    .setSource(PipelineError.Source.HYMNAL_NET)
                                    .setSeverity(Severity.ERROR)
                                    .setErrorType(ErrorType.FETCH_EXCEPTION)
                                    .addMessages(String.format("Failed to fetch fetch: %s, a related song of %s",
                                                                        relatedSong, parent))
                                    .addMessages(String.valueOf(((FetchResult.FetchFailure) fetchResult).responseCode));
                            if (!TextUtil.isEmpty(((FetchResult.FetchFailure) fetchResult).responseBody)) {
                              error.addMessages(((FetchResult.FetchFailure) fetchResult).responseBody);
                            }
                            this.errors.add(error.build());
                            return false;
                          }

                          return !(fetchResult instanceof FetchResult.FetchNotFound);
                        });
                    return key.isPresent();
            })
                .collect(toImmutableList()))
        .build();
  }
}
