package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Module(subcomponents = HymnalNetPipelineComponent.class)
public interface PipelineModule {

  @PipelineScope
  @Provides
  static HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }

  @PipelineScope
  @Provides
  static ZonedDateTime currentTime() {
    return LocalDateTime.now().atZone(ZoneId.of("America/Los_Angeles"));
  }

  @PipelineScope
  @Provides
  static File outputDirectory(ZonedDateTime currentTime) {
    String outputDirectoryPath = String.format("storage/output/%s",
        currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    File outputDirectory = new File(outputDirectoryPath);
    if (!outputDirectory.mkdir()) {
      throw new IllegalStateException("Unable to create directory to write errors");
    }
    return outputDirectory;
  }
}
