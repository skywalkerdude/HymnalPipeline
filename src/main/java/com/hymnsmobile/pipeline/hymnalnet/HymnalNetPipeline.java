package com.hymnsmobile.pipeline.hymnalnet;

import static com.hymnsmobile.pipeline.hymnalnet.BlockList.BLOCK_LIST;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class HymnalNetPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Converter converter;
  private final Fetcher fetcher;

  private final Set<Hymn> hymns;
  private final Set<PipelineError> errors;

  @Inject
  public HymnalNetPipeline(
      Converter converter,
      Fetcher fetcher,
      @HymnalNet Set<Hymn> hymns,
      @HymnalNet Set<PipelineError> errors) {
    this.converter = converter;
    this.fetcher = fetcher;
    this.hymns = hymns;
    this.errors = errors;
  }

  public void run() throws IOException, InterruptedException, URISyntaxException {
    readFile();
    fetchHymns();
    writeHymns();
  }

  private void readFile() throws IOException {
    File storageDirectory = new File("storage/hymnalnet");
    LOGGER.info(String.format("Reading files from %s", storageDirectory.getName()));
    File[] hymnalNetFiles = storageDirectory.listFiles();
    if (hymnalNetFiles == null) {
      throw new RuntimeException("file storage empty");
    }

    Optional<File> mostRecentFile = ImmutableList.copyOf(hymnalNetFiles).stream().filter(
            file -> file.getName().matches("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_PST.txt"))
        .max(Comparator.comparing(File::getName));
    // No file to read
    if (mostRecentFile.isEmpty()) {
      return;
    }

    LOGGER.info(String.format("Reading file %s", mostRecentFile.get()));
    com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet hymnalNet = com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.parseFrom(
        new FileInputStream(mostRecentFile.get()));
    this.hymns.addAll(hymnalNet.getHymnsList());
    this.errors.addAll(hymnalNet.getErrorsList());
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  private void fetchHymns() throws InterruptedException, IOException, URISyntaxException {
    for (HymnType hymnType : HymnType.values()) {
      for (int hymnNumber = 1; hymnNumber < hymnType.maxNumber.orElse(1000); hymnNumber++) {
        HymnalDbKey key = HymnalDbKey.create(hymnType, String.valueOf(hymnNumber));
        SongReference songReference = converter.toSongReference(key);
        fetchHymn(songReference);
        if (hymnType == HymnType.CHINESE) {
          fetchHymn(songReference.toBuilder()
              .setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED).build());
        }
        if (hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
          fetchHymn(songReference.toBuilder()
              .setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED)
              .build());
        }
      }
    }
  }

  private void fetchHymn(SongReference songReference)
      throws InterruptedException, IOException, URISyntaxException {
    LOGGER.info(String.format("Fetching %s", songReference));
    if (hymns.stream().anyMatch(hymn -> hymn.getReference().equals(songReference))) {
      LOGGER.info(String.format("%s already exists. Skipping...", songReference));
      return;
    }

    if (BLOCK_LIST.contains(songReference)) {
      LOGGER.info(String.format("%s contained in block list. Skipping...", songReference));
      return;
    }

    Optional<Hymn> hymn = fetcher.fetchHymn(songReference);
    if (hymn.isEmpty()) {
      LOGGER.warning(String.format("Fetching %s was unsuccessful", songReference));
      return;
    }
    this.hymns.add(hymn.get());

    // Also fetch all related songs
    List<SongReference> relatedSongs = ImmutableList.<SongReference>builder()
        .addAll(hymn.get().getLanguagesMap().values()).addAll(hymn.get().getRelevantsMap().values())
        .build();
    LOGGER.info(String.format("Found %d related songs: %s", relatedSongs.size(), relatedSongs));
    for (SongReference relatedSong : relatedSongs) {
      fetchHymn(relatedSong);
    }
  }

  /**
   * Write hymns to a dated storage file.
   */
  private void writeHymns() throws IOException {
    String fileName = String.format("storage/hymnalnet/%s.txt",
        LocalDateTime.now().atZone(ZoneId.of("America/Los_Angeles"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.info(String.format("Writing hymns to %s", fileName));
    try (FileOutputStream output = new FileOutputStream(fileName)) {
      com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet.newBuilder().addAllHymns(hymns).build()
          .writeTo(output);
    }
  }
}
