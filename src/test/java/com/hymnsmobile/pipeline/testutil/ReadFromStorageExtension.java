package com.hymnsmobile.pipeline.testutil;

import com.hymnsmobile.pipeline.dagger.PipelineTestModule;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.doAnswer;

public class ReadFromStorageExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws IOException {
    ReadFromStorage methodAnnotation = context.getRequiredTestMethod().getAnnotation(
        ReadFromStorage.class);
    if (methodAnnotation != null) {
      HymnalNet c60 =
          TestUtils.readTextProto("src/test/resources/hymnalnet/storage/c60.textproto", HymnalNet.newBuilder());
      doAnswer(invocation -> Optional.of(c60))
          .when(PipelineTestModule.MOCK_FILE_WRITER)
          .readLatestOutput(
              "storage/hymnalnet",
              Optional.of("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_[A-Z]{3}"),
              com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.parser());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Mockito.reset(PipelineTestModule.MOCK_FILE_WRITER);
  }
}
