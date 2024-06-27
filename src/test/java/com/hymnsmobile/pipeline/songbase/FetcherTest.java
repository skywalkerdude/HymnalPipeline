package com.hymnsmobile.pipeline.songbase;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.hymnsmobile.pipeline.songbase.Fetcher.SONGBASE_DB_DUMP_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FetcherTest {

  private static final String SUCCESSFUL_RESPONSE_STRING = "Successful response string";

  @Mock private HttpClient client;
  @Mock private HttpResponse<String> errorResponse;
  @Mock private HttpResponse<String> successfulResponse;

  private Fetcher target;

  @BeforeEach
  public void setUp() throws IOException {
    lenient().doReturn(500).when(errorResponse).statusCode();

    lenient().doReturn(200).when(successfulResponse).statusCode();
    lenient().doReturn(SUCCESSFUL_RESPONSE_STRING).when(successfulResponse).body();

    this.target = new Fetcher(client);
  }

  @Test
  public void fetchHymns__fetchErrorCode__throwsException() throws IOException, InterruptedException {
    doReturn(errorResponse)
        .when(client)
        .send(HttpRequest.newBuilder().uri(URI.create(SONGBASE_DB_DUMP_URL)).build(),
            HttpResponse.BodyHandlers.ofString());

    assertThatThrownBy(() -> target.fetch())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("https://songbase.life/api/v2/app_data returned with status code: 500");
  }

  @Test
  public void fetchHymns__fetchSuccessful__fetchesRelatedSongs_onlyAddErrorsForFetchExceptions() throws IOException, InterruptedException {
    doReturn(successfulResponse)
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create(SONGBASE_DB_DUMP_URL)).build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(target.fetch()).isEqualTo(SUCCESSFUL_RESPONSE_STRING);
  }
}