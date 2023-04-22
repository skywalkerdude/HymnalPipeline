package com.hymnsmobile.pipeline.songbase;

import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * Writes the raw songbase response into a file
 */
@SongbasePipelineScope
public class Writer {

  private static final Logger LOGGER = Logger.getGlobal();

  private final FileReadWriter fileReadWriter;
  private final ZonedDateTime currentTime;

  @Inject
  Writer(FileReadWriter fileReadWriter, ZonedDateTime currentTime) {
    this.fileReadWriter = fileReadWriter;
    this.currentTime = currentTime;
  }

  public void write(String responseBody) throws IOException {
    String fileName = String.format("storage/songbase/%s.txt",
        currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.fine(String.format("Writing songbase to %s", fileName));
    fileReadWriter.writeString(fileName, responseBody);
  }
}
