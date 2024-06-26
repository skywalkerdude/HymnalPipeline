package com.hymnsmobile.pipeline;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hymnsmobile.pipeline.models.Line;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileReadWriterTest {

  private static final String RESOURCE_DIR_PATH = "src/test/resources/common";

  private FileReadWriter target;

  @BeforeEach
  public void setUp() {
    this.target = new FileReadWriter();
  }

  @Test
  public void readLargestFilePath__noFilesFound__throwsException() {
    assertThatThrownBy(() -> target.readLargestFilePath("invalid/path", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid file path: invalid/path");
  }

  @Test
  public void readLargestFilePath__noFileMask__returnsLargestFile() {
    assertThat(
        target.readLargestFilePath(
            RESOURCE_DIR_PATH,
            Optional.empty()))
        .hasValueSatisfying(file -> assertThat(file.getPath()).isEqualTo("src/test/resources/common/file_empty"));
  }

  @Test
  public void readLargestFilePath__withFileMask__returnsLargestFileMatchingFileMask() {
    assertThat(
        target.readLargestFilePath(
            RESOURCE_DIR_PATH,
            Optional.of("dir_\\d")))
        .hasValueSatisfying(file -> assertThat(file.getPath()).isEqualTo("src/test/resources/common/dir_1"));
  }

  @Test
  public void readLargestFilePath__withFileMask_noMatchingFiles__returnsEmpty() {
    assertThat(target.readLargestFilePath(RESOURCE_DIR_PATH, Optional.of("no_match")))
        .isEmpty();
  }

  @Test
  public void readLatestOutput__noFilesFound__returnsEmpty() {
    assertThat(target.readLatestOutput(RESOURCE_DIR_PATH, Optional.of("no_match"), Line.parser())).isEmpty();
  }

  @Test
  public void readLatestOutput__notADirectory__throwsException() {
    assertThatThrownBy(() ->
        target.readLatestOutput(
            RESOURCE_DIR_PATH,
            Optional.of("file_empty"),
            Line.parser()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Not a directory: src/test/resources/common/file_empty");
  }

  @Test
  public void readLatestOutput__emptyDirectory__throwsException() {
    assertThatThrownBy(() ->
        target.readLatestOutput(
            RESOURCE_DIR_PATH,
            Optional.of("dir_empty"),
            Line.parser()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasCauseExactlyInstanceOf(FileNotFoundException.class)
        .hasMessageContaining("Exception occurred while parsing: src/test/resources/common/dir_empty/output.binaryproto");
  }

  @Test
  public void readLatestOutput__invalidProtobuf__throwsException() {
    assertThatThrownBy(() ->
        target.readLatestOutput(
            RESOURCE_DIR_PATH,
            Optional.of("dir_malformed"),
            Line.parser()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasCauseExactlyInstanceOf(InvalidProtocolBufferException.class)
        .hasMessageContaining("Exception occurred while parsing: src/test/resources/common/dir_malformed/output.binaryproto");
  }

  @Test
  public void readLatestOutput__fileFound__parsesOutput() {
    assertThat(target.readLatestOutput(RESOURCE_DIR_PATH, Optional.of("dir_1"), Line.parser()))
        .hasValue(Line.getDefaultInstance());
  }

  @Test
  public void writeProto__writesBinaryProto_writesTextProto() throws IOException {
    String outputDirectory = RESOURCE_DIR_PATH + "/output";
    try {
      assertThat(new File(outputDirectory)).doesNotExist();
      target.writeProto(outputDirectory, Line.newBuilder().setLineContent("dummy content").build());
      assertThat(new File(outputDirectory)).exists();
      assertThat(new File(outputDirectory)).isDirectory();
      assertThat(Line.parser().parseFrom(new FileInputStream(outputDirectory + "/output.binaryproto")))
          .isEqualTo(Line.newBuilder().setLineContent("dummy content").build());
      assertThat(Files.readString(Paths.get(outputDirectory + "/output.textproto"), StandardCharsets.UTF_8))
          .isEqualTo("line_content: \"dummy content\"\n");
    } finally {
      // Clean up files no matter happens to the test.
      assertThat(FileUtils.deleteQuietly(new File(outputDirectory))).isTrue();
    }
  }
}
