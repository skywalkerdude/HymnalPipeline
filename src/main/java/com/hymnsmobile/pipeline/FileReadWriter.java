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

  public Optional<File> readLargestFilePath(String path, Optional<String> fileMask) {
    File directory = new File(path);
    LOGGER.fine(String.format("Reading files from %s", directory.getName()));
    File[] files = directory.listFiles();
    if (files == null) {
      throw new IllegalArgumentException(String.format("invalid file path: %s", path));
    }
    return ImmutableList.copyOf(files).stream()
        .filter(file -> fileMask.map(mask -> file.getName().matches(mask)).orElse(true))
        .max(Comparator.comparing(File::getName));
  }

  public <M extends Message> Optional<M> readLatestOutput(String path, Optional<String> fileMask, Parser<M> parser) {
    Optional<File> largestFilePath = readLargestFilePath(path, fileMask);
    if (largestFilePath.isEmpty()) {
      return Optional.empty();
    }
    return largestFilePath.flatMap(directory -> {
      LOGGER.fine(String.format("Reading from %s", directory.getName()));
      File[] files = directory.listFiles();
      if (files == null) {
        throw new IllegalArgumentException(String.format("Not a directory: %s", directory));
      }
      String latestOutputFile = directory + "/output.binaryproto";
      try {
        M parsed = parser.parseFrom(new FileInputStream(latestOutputFile));
        return Optional.of(parsed);
      } catch (InvalidProtocolBufferException | FileNotFoundException e) {
        throw new IllegalArgumentException(String.format("Exception occurred while parsing: %s", latestOutputFile), e);
      }
    });
  }

  public <M extends Message> void writeProto(String directoryPath, M message) throws IOException {
    if (!new File(directoryPath).mkdirs()) {
      throw new IllegalArgumentException(String.format("Unable to create %s", directoryPath));
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
