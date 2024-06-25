package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.models.PipelineError;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

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

  public void run() throws IOException {
    LOGGER.info("Hymnal.net pipeline starting");
    readFile();
    fetcher.fetchHymns();
    writeHymns();
    LOGGER.info("Hymnal.net pipeline finished");
  }

  private void readFile() {
    Optional<com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet> mostRecentOutput =
        fileReadWriter.readLatestOutput(
            "storage/hymnalnet",
            Optional.of("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_[A-Z]{3}"),
            com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.parser());
    mostRecentOutput.ifPresent(hymnalNet -> {
      this.hymnalNetJsons.addAll(hymnalNet.getHymnanlNetJsonList());
      this.errors.addAll(hymnalNet.getErrorsList());
      LOGGER.info(String.format("Reading file with %d songs and %d errors",
          hymnalNet.getHymnanlNetJsonCount(), hymnalNet.getErrorsCount()));
    });
  }

  private void writeHymns() throws IOException {
    String directoryPath = String.format("storage/hymnalnet/%s",
        currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.info(String.format("Writing hymns to %s", directoryPath));
    fileReadWriter.writeProto(directoryPath,
        com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.newBuilder()
            .addAllHymnanlNetJson(hymnalNetJsons)
            .addAllErrors(errors)
            .build());
  }

  public static void main(String[] args) throws IOException {
    DaggerPipelineComponent.create().hymnalNetComponent().build().pipeline().run();
  }
}
