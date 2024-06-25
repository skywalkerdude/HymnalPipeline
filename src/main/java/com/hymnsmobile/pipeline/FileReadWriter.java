package com.hymnsmobile.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.hymnsmobile.pipeline.dagger.PipelineScope;

import javax.inject.Inject;
import java.io.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Generic class that reads from and writes to files.
 */
@PipelineScope
public class FileReadWriter {

  private static final Logger LOGGER = Logger.getGlobal();

  @Inject
  public FileReadWriter() {
  }

  public <M extends Message> Optional<M> readLatestOutput(String path, Optional<String> fileMask, Parser<M> parser) {
    Optional<File> largestFilePath = readLargestFilePath(path, fileMask);
    return largestFilePath.flatMap(directory -> {
      LOGGER.fine(String.format("Reading from %s", directory.getName()));
      File[] files = directory.listFiles();
      if (files == null) {
        throw new RuntimeException("file storage not found");
      }
      try {
        M parsed = parser.parseFrom(new FileInputStream(directory + "/output.binaryproto"));
        if (parsed == null) {
          LOGGER.severe("Latest output contained a malformed message.");
          return Optional.empty();
        }
        return Optional.of(parsed);
      } catch (InvalidProtocolBufferException | FileNotFoundException e) {
        LOGGER.severe(e.getMessage());
        return Optional.empty();
      }
    });
  }

  public Optional<File> readLargestFilePath(String path, Optional<String> fileMask) {
    File directory = new File(path);
    LOGGER.fine(String.format("Reading files from %s", directory.getName()));
    File[] hymnalNetFiles = directory.listFiles();
    if (hymnalNetFiles == null) {
      throw new RuntimeException("file storage not found");
    }
    return ImmutableList.copyOf(hymnalNetFiles).stream()
        .filter(file -> fileMask.map(mask -> file.getName().matches(mask)).orElse(true))
        .max(Comparator.comparing(File::getName));
  }

  public <M extends Message> void writeProto(String directoryPath, M message) throws IOException {
    if (!new File(directoryPath).mkdirs()) {
      LOGGER.severe(String.format("Unable to create %s", directoryPath));
      return;
    }
    try (FileOutputStream output = new FileOutputStream(directoryPath + "/output.binaryproto")) {
      message.writeTo(output);
    }
    writeString(directoryPath + "/output.textproto", message.toString());
  }

  public void writeString(String fileName, String content) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
    writer.write(content);
    writer.close();
  }
}
