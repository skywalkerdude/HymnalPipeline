package com.hymnsmobile.pipeline.songbase;

import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import javax.inject.Inject;

@SongbasePipelineScope
public class Fetcher {

  private static final String SONGBASE_DB_DUMP_URL = "https://songbase.life/api/v1/app_data";

  private final HttpClient client;

  @Inject
  public Fetcher(
      HttpClient client) {
    this.client = client;
  }

  public String fetch() throws IOException, InterruptedException {
    HttpResponse<String> response = client.send(
        HttpRequest.newBuilder().uri(URI.create(SONGBASE_DB_DUMP_URL)).build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          String.format("%s returned with status code: %d", SONGBASE_DB_DUMP_URL,
              response.statusCode()));
    }
    return response.body();
  }
}
