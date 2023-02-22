package com.hymnsmobile.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hymnsmobile.common.dagger.PipelineScope;
import com.hymnsmobile.common.models.PipelineError;
import com.hymnsmobile.common.models.PipelineError.Severity;
import com.hymnsmobile.hymnalnet.models.Hymn;
import com.hymnsmobile.hymnalnet.models.HymnalNet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;

@PipelineScope
public class HymnalNetPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final HymnalNetFetcher fetcher;

  private final ImmutableMap.Builder<HymnalDbKey, Hymn> hymns;
  private final ImmutableList.Builder<PipelineError> errors;

  @Inject
  public HymnalNetPipeline(HymnalNetFetcher fetcher) {
    this.fetcher = fetcher;
    this.hymns = ImmutableMap.builder();
    this.errors = ImmutableList.builder();
  }

  public ImmutableMap<HymnalDbKey, Hymn> hymns() {
    return hymns.build();
  }

  public ImmutableList<PipelineError> errors() {
    return errors.build();
  }

  public void run() throws InterruptedException, IOException {
    // Read stored file
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

    Optional<File> mostRecentFile = ImmutableList.copyOf(hymnalNetFiles).stream()
        .filter(
            file -> file.getName()
                .matches("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_PST.txt"))
        .max(Comparator.comparing(File::getName));
    // No file to read
    if (mostRecentFile.isEmpty()) {
      return;
    }

    LOGGER.info(String.format("Reading file %s", mostRecentFile.get()));
    HymnalNet.parseFrom(new FileInputStream(mostRecentFile.get()))
        .getHymnsMap()
        .forEach((key, value) -> {
          Optional<HymnalDbKey> hymnalDbKey = HymnalDbKey.fromString(key);
          if (hymnalDbKey.isEmpty()) {
            throw new RuntimeException(
                String.format("Invalid hymnal db key found: %s", hymnalDbKey));
          }
          hymns.put(hymnalDbKey.get(), value);
        });
  }

  /**
   * Fetch hymns afresh from Hymnal.net.
   */
  private void fetchHymns() throws InterruptedException, IOException {
    for (HymnType hymnType : ImmutableList.of(HymnType.CHILDREN_SONG, HymnType.HOWARD_HIGASHI,
        HymnType.DUTCH, HymnType.SPANISH, HymnType.FRENCH)) {
      if (hymnType.maxNumber.isPresent()) {
        for (int hymnNumber = 1; hymnNumber < hymnType.maxNumber.get(); hymnNumber++) {
          HymnalDbKey key = HymnalDbKey.create(hymnType, String.valueOf(hymnNumber));
          fetchHymn(key);
          if (hymnType == HymnType.CHINESE || hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
            HymnalDbKey simplifiedKey = HymnalDbKey.create(hymnType, String.valueOf(hymnNumber),
                "gb=1");
            fetchHymn(simplifiedKey);
          }
        }
      }
    }
  }

  private void fetchHymn(HymnalDbKey key) throws InterruptedException, IOException {
    LOGGER.info(String.format("Fetching %s", key));
    if (hymns.build().containsKey(key)) {
      LOGGER.info(String.format("%s already exists. Skipping...", key));
      return;
    }

    Hymn hymn = fetcher.fetchHymn(key);
    ImmutableList<PipelineError> errors = sanitizeHymn(key, hymn);
    LOGGER.info(String.format("%s fetched with %d errors.", key, errors.size()));
    this.errors.addAll(errors);
    this.hymns.put(key, hymn);

    List<HymnalDbKey> relatedSongKeys = hymn.getMetaDataList().stream()
        .filter(
            metaDatum -> metaDatum.getName().equals("Languages") || metaDatum.getName()
                .equals("Relevant"))
        .flatMap(metaDatum -> metaDatum.getDataList().stream())
        .map(
            datum -> {
              Optional<HymnalDbKey> relatedSongKey = HymnalDbKey.extractFromPath(datum.getPath());
              if (relatedSongKey.isEmpty()) {
                HymnalNetPipeline.this.errors.add(
                    PipelineError.newBuilder().setSeverity(Severity.ERROR)
                        .setMessage(String.format("Hymn %s had an invalid related hymn: %s", key,
                            datum.getPath())).build());
              }
              return relatedSongKey;
            })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
    LOGGER.info(
        String.format("Found %d related songs: %s", relatedSongKeys.size(), relatedSongKeys));
    for (HymnalDbKey relatedSongKey : relatedSongKeys) {
      fetchHymn(relatedSongKey);
    }
  }

  private ImmutableList<PipelineError> sanitizeHymn(HymnalDbKey key, Hymn hymn) {
    return ensureTransliterationHasSameNumberOfLinesAsVerse(key, hymn);
  }

  private ImmutableList<PipelineError> ensureTransliterationHasSameNumberOfLinesAsVerse(
      HymnalDbKey key, Hymn hymn) {
    ImmutableList.Builder<PipelineError> errors = ImmutableList.builder();
    hymn.getLyricsList().forEach(verse -> {
      if (verse.getTransliterationCount() == 0
          || verse.getTransliterationCount() == verse.getVerseContentCount()) {
        return;
      }
      errors.add(PipelineError.newBuilder()
          .setMessage(String.format("%s has %s transliteration lines and %s verse lines", key,
              verse.getTransliterationCount(), verse.getVerseContentCount()))
          .setSeverity(Severity.WARNING).build());
    });
    return errors.build();
  }

  /**
   * Write hymns to a dated storage file.
   */
  private void writeHymns() throws IOException {
    String fileName = String.format("storage/hymnalnet/%s.txt",
        LocalDateTime.now().atZone(ZoneId.of("America/Los_Angeles")).format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_z")));
    LOGGER.info(String.format("Writing hymns to %s", fileName));
    try (FileOutputStream output = new FileOutputStream(fileName)) {
      ImmutableMap.Builder<String, Hymn> serializedMap = ImmutableMap.builder();
      for (Entry<HymnalDbKey, Hymn> entry : hymns.build().entrySet()) {
        serializedMap.put(entry.getKey().toString(), entry.getValue());
      }
      HymnalNet.Builder builder = HymnalNet.newBuilder();
      hymns.build().forEach((key, value) -> builder.putHymns(key.toString(), value));
      builder.addAllErrors(errors.build());
      builder.build().writeTo(output);
    }
  }
}
