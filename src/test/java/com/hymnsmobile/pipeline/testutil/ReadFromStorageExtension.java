package com.hymnsmobile.pipeline.testutil;

import static com.hymnsmobile.pipeline.dagger.PipelineTestModule.MOCK_FILE_WRITER;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

public class ReadFromStorageExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    ReadFromStorage methodAnnotation = context.getRequiredTestMethod().getAnnotation(
        ReadFromStorage.class);
    if (methodAnnotation != null) {
      when(MOCK_FILE_WRITER.readLargestFile("storage/hymnalnet",
          Optional.of("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_PST.txt"))).thenAnswer(
          invocation -> Optional.of(new File("src/test/resources/hymnalnet/storage/c60.txt")));
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Mockito.reset(MOCK_FILE_WRITER);
  }
}
