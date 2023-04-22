package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.models.PipelineError;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class HymnalNetPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Set<PipelineError> errors;
  private final Fetcher fetcher;
  private final FileReadWriter fileReadWriter;
  private final Set<HymnalNetJson> hymnalNetJsons;
  private final ZonedDateTime currentTime;

  @Inject
  public HymnalNetPipeline(
      Converter converter,
      Fetcher fetcher,
      FileReadWriter fileReadWriter,
      @HymnalNet Set<PipelineError> errors,
      ZonedDateTime currentTime,
      Set<HymnalNetJson> hymnalNetJsons) {
    this.errors = errors;
    this.fetcher = fetcher;
    this.fileReadWriter = fileReadWriter;
    this.hymnalNetJsons = hymnalNetJsons;
    this.currentTime = currentTime;
  }

  public ImmutableList<HymnalNetJson> getHymnalNetJsons() {
    return ImmutableList.copyOf(hymnalNetJsons);
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void run() throws IOException, InterruptedException, URISyntaxException {
    LOGGER.fine("Hymnal.net pipeline starting");
    readFile();
    fetcher.fetchHymns();
    writeAllHymns();
    LOGGER.fine("Hymnal.net pipeline finished");
  }

  private void readFile() throws IOException {
    Optional<File> mostRecentFile = fileReadWriter.readLargestFile("storage/hymnalnet",
        Optional.of("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_PDT.txt"));
    // No file to read
    if (mostRecentFile.isEmpty()) {
      return;
    }

    LOGGER.fine(String.format("Reading file %s", mostRecentFile.get()));
    com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet hymnalNet =
        com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.parseFrom(
            new FileInputStream(mostRecentFile.get()));
    this.hymnalNetJsons.addAll(hymnalNet.getHymnanlNetJsonList());
    this.errors.addAll(hymnalNet.getErrorsList());
  }

  private void writeAllHymns() throws IOException {
    String fileName = String.format("storage/hymnalnet/%s.txt",
        currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.fine(String.format("Writing hymns to %s", fileName));
    fileReadWriter.writeProto(fileName,
        com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.newBuilder()
            .addAllHymnanlNetJson(hymnalNetJsons).addAllErrors(errors).build());
  }
}
