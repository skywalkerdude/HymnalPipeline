package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.utils.ProtoUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

  public void run() throws IOException, NoSuchAlgorithmException {
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

  private void writeHymns() throws IOException, NoSuchAlgorithmException {
    String directoryPath = String.format("storage/hymnalnet/%s",
        currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.info(String.format("Writing hymns to %s", directoryPath));

    List<HymnalNetJson> newList = hymnalNetJsons.stream().sorted((hymnalNetJson1, hymnalNetJson2) -> {
      HymnalNetKey key1 = hymnalNetJson1.getKey();
      HymnalNetKey key2 = hymnalNetJson2.getKey();

      // Sort first by hymn type.
      int hymnTypeCompare = key1.getHymnType().compareTo(key2.getHymnType());
      if (hymnTypeCompare != 0) {
        return hymnTypeCompare;
      }

      // Then sort by hymn number.
      int hymnNumberCompare = key1.getHymnNumber().compareTo(key2.getHymnNumber());
      if (hymnNumberCompare != 0) {
        return hymnNumberCompare;
      }

      // Sort by query params last.
      return key1.getQueryParams().compareTo(key2.getQueryParams());
    }).toList();
    com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.Builder builder =
            com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.newBuilder()
                    .addAllHymnanlNetJson(newList)
                    .addAllErrors(errors);
    ByteString sha256 = ByteString.copyFrom(ProtoUtils.hashProto(builder.build()));
    fileReadWriter.writeProto(directoryPath, builder.setSha256(sha256).build());
  }

  public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
    DaggerPipelineComponent.create().hymnalNetComponent().build().pipeline().run();
  }
}
