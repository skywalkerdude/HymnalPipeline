package com.hymnsmobile.pipeline.storage.dagger;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Module
interface StorageModule {

  @StorageScope
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
