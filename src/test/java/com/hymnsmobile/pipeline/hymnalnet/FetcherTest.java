package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetcherTest {

  @Mock private HttpClient client;
  @Mock private HttpResponse<String> errorResponse;
  @Mock private HttpResponse<String> h1;
  @Mock private HttpResponse<String> ht1;

  private Fetcher target;

  @BeforeEach
  public void setUp() throws IOException {
    lenient().doReturn(500).when(errorResponse).statusCode();
    lenient().doReturn("error response found!").when(errorResponse).body();

    lenient().doReturn(200).when(h1).statusCode();
    lenient().doReturn(TestUtils.readText("src/test/resources/hymnalnet/input/_v2_hymn_h_1")).when(h1).body();

    lenient().doReturn(200).when(ht1).statusCode();
    lenient().doReturn(TestUtils.readText("src/test/resources/hymnalnet/input/_v2_hymn_ht_1")).when(ht1).body();
  }

  @Test
  public void fetchHymns__alreadyFetched__returnsFetchedList() {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    // Already fetched list
    Set<HymnalNetJson> hymnalNetJsons =
        new HashSet<>(Set.of(
            HymnalNetJson.newBuilder()
                .setKey(
                    HymnalNetKey.newBuilder()
                        .setHymnType("h")
                        .setHymnNumber("1"))
                .build()));

    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).containsExactlyElementsOf(hymnalNetJsons);
    assertThat(errors).isEmpty();
  }

  @Test
  public void fetchHymns__fetchErrorCode__addsErrorToList() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    lenient().doReturn(null).when(errorResponse).body();
    doReturn(errorResponse)
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/h/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).isEmpty();
    assertThat(errors).containsExactlyInAnyOrder(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.FETCH_ERROR)
            .addMessages("Failed to fetch: h/1")
            .build(),
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.FETCH_ERROR_NON_200)
            .addMessages("h/1")
            .addMessages("500")
            .build());
  }

  @Test
  public void fetchHymns__fetchErrorCode_noMaxNumber__doNotAddToErrorToList() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("nt").setHymnNumber("1").build());

    doReturn(errorResponse)
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/nt/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).isEmpty();
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setErrorType(PipelineError.ErrorType.FETCH_ERROR_NON_200)
            .addMessages("nt/1")
            .addMessages("500")
            .addMessages("error response found!")
            .build()
    );
  }

  @Test
  public void fetchHymns__fetchIOException__addsErrorToList() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    doThrow(new IOException())
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/h/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).isEmpty();
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.FETCH_EXCEPTION)
            .addMessages("Exception thrown during fetch: hymn_type: \"h\"\nhymn_number: \"1\"\n")
            .build());
  }

  @Test
  public void fetchHymns__fetchURISyntaxException__addsErrorToList() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    doAnswer(invocationOnMock -> {
      throw new URISyntaxException("dummy", "dummy");
    }).when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/h/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).isEmpty();
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.FETCH_EXCEPTION)
            .addMessages("Exception thrown during fetch: hymn_type: \"h\"\nhymn_number: \"1\"\n")
            .build());
  }

  @Test
  public void fetchHymns__fetchInterruptedException__addsErrorToList() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    doThrow(new InterruptedException())
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/h/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    assertThat(hymnalNetJsons).isEmpty();
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.FETCH_EXCEPTION)
            .addMessages("Exception thrown during fetch: hymn_type: \"h\"\nhymn_number: \"1\"\n")
            .build());
  }

  /**
   * Tests the fetcher's success case end-to-end. This test attempts to fetch:
   * - The initial song to fetch (success)        <-- Add to response list
   * - One related song (success)                 <-- Add to response list
   * - Another related song (exception thrown)    <-- Add to error list
   * - All other related songs (non-200 response) <-- Do not add to error list
   */
  @Test
  public void fetchHymns__fetchSuccessful__fetchesRelatedSongs_onlyAddErrorsForFetchExceptions() throws IOException, InterruptedException {
    ImmutableList<HymnalNetKey> songsToFetch =
        ImmutableList.of(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1").build());

    Set<HymnalNetJson> hymnalNetJsons = new HashSet<>();
    doReturn(errorResponse).when(client).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    doReturn(h1)
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/h/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());
    doReturn(ht1)
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/ht/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());
    doThrow(new IOException())
        .when(client)
        .send(
            HttpRequest.newBuilder().uri(URI.create("https://hymnalnetapi.herokuapp.com/v2/hymn/cb/1?check_exists=true")).build(),
            HttpResponse.BodyHandlers.ofString());

    Set<PipelineError> errors = new HashSet<>();

    target = new Fetcher(client, songsToFetch, hymnalNetJsons, errors);
    target.fetchHymns();

    HymnalNet.Builder expected = HymnalNet.newBuilder();
    TestUtils.readTextProto("src/test/resources/hymnalnet/output/fetcher_test_fetchSuccessful.textproto", expected);

    assertThat(hymnalNetJsons).containsExactlyInAnyOrderElementsOf(expected.getHymnanlNetJsonList());
    assertThat(errors).containsExactlyInAnyOrderElementsOf(expected.getErrorsList());
  }
}
