package com.hymnsmobile.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.hymnsmobile.pipeline.dagger.PipelineScope;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * Generic class that reads from and writes to files.
 */
@PipelineScope
public class FileReadWriter {

  private static final Logger LOGGER = Logger.getGlobal();

  @Inject
  public FileReadWriter() {
  }

  public Optional<File> readLargestFile(String path, Optional<String> fileMask) {
    File directory = new File(path);
    LOGGER.info(String.format("Reading files from %s", directory.getName()));
    File[] hymnalNetFiles = directory.listFiles();
    if (hymnalNetFiles == null) {
      throw new RuntimeException("file storage not found");
    }

    return ImmutableList.copyOf(hymnalNetFiles).stream()
        .filter(file -> fileMask.map(mask -> file.getName().matches(mask)).orElse(true))
        .max(Comparator.comparing(File::getName));
  }

  public <M extends Message> void writeProto(String fileName, M message) throws IOException {
    try (FileOutputStream output = new FileOutputStream(fileName)) {
      message.writeTo(output);
    }
  }
}
