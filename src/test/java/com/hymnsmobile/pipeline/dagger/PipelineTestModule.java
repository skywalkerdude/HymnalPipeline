package com.hymnsmobile.pipeline.dagger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineTestComponent;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.testutil.MockHttpResponse;
import dagger.Module;
import dagger.Provides;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Module(subcomponents = HymnalNetPipelineTestComponent.class)
public interface PipelineTestModule {

  FileReadWriter MOCK_FILE_WRITER = mock(FileReadWriter.class);

  private static String readInputStreamAsString(File file) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int result = bis.read();
    while (result != -1) {
      byte b = (byte) result;
      buf.write(b);
      result = bis.read();
    }
    return buf.toString();
  }

  @Provides
  static HttpClient httpClient() {
    HttpClient httpClient = mock(HttpClient.class);
    try {
      when(httpClient.send(any(HttpRequest.class), eq(BodyHandlers.ofString()))).thenAnswer(
          invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String filename = request.uri().getPath().replace("/", "_");

            File responseDirectory = new File("src/test/resources/hymnalnet/input");
            File[] hymnalNetResponses = responseDirectory.listFiles();
            if (hymnalNetResponses == null) {
              throw new RuntimeException("file storage not found");
            }

            Optional<File> response = ImmutableList.copyOf(hymnalNetResponses).stream()
                .filter(file -> file.getName().equals(filename)).findFirst();
            if (response.isEmpty()) {
              return new MockHttpResponse(404, "Not found");
            }
            return new MockHttpResponse(200, readInputStreamAsString(response.get()));
          });
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return httpClient;
  }

  @Provides
  static FileReadWriter fileWriter() {
    return MOCK_FILE_WRITER;
  }

  @Provides
  static ZonedDateTime currentTime() {
    return LocalDateTime.of(1993, 7, 17, 10, 10, 0).atZone(ZoneId.of("America/Los_Angeles"));
  }

  @PipelineScope
  @Provides
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}
