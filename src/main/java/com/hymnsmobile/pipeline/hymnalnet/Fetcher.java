package com.hymnsmobile.pipeline.hymnalnet;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.SongReference;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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

  @Inject
  public Fetcher(HttpClient client, Converter converter) {
    this.client = client;
    this.converter = converter;
  }

  public Hymn fetchHymn(SongReference songReference)
      throws IOException, InterruptedException, URISyntaxException {
    HymnalDbKey key = converter.toHymnalDbKey(songReference);
    HymnType hymnType = key.hymnType;
    String hymnNumber = key.hymnNumber;

    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
            .uri(buildUri(key))
            .build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException(
          String.format("%s returned with status code: %d", buildUri(key),
              response.statusCode()));
    }

    HymnalNetJson.Builder builder = HymnalNetJson.newBuilder();
    try {
      JsonFormat.parser().merge(response.body(), builder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(
          String.format("Unable to parse %s/%s: %s", hymnType, hymnNumber, response.body()), e);
    }
    LOGGER.info(String.format("%s successfully fetched", key));
    return converter.toHymn(key, builder.build());
  }

  /**
   * Slightly hacky way to form the uri. Basically, this assumes that 'queryParams' is of the form
   * "?gb=1". To remove the "?", we need to take the substring. Otherwise, it will look like
   * "...v2/hymn/ts/330??check_exists=true".
   */
  private URI buildUri(HymnalDbKey key) throws URISyntaxException {
    return new URI(SCHEME, AUTHORITY, String.format(PATH, key.hymnType, key.hymnNumber),
        key.queryParams.map(
            queryParam -> queryParam.substring(1) + "&" + CHECK_EXISTS).orElse(CHECK_EXISTS),
        null);
  }
}
