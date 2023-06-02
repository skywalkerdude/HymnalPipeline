package com.hymnsmobile.pipeline.songbase;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.storage.DatabaseWriter.DATABASE_VERSION;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.models.SongReference;
import dagger.Lazy;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;

public class SongbaseDiffer {

  private static final String SONGBASE_DATABASE_PATH = "jdbc:sqlite:storage/songbase/songbasedb-v3.sqlite";

  private final FileReadWriter fileReadWriter;

  private final Lazy<File> outputDirectory;

  @Inject
  public SongbaseDiffer(FileReadWriter fileReadWriter, Lazy<File> outputDirectory) {
    this.fileReadWriter = fileReadWriter;
    this.outputDirectory = outputDirectory;
  }

  /**
   * Goes through the songbase db and generates a diff report between that and the current Hymns
   * database.
   * </p>
   * This is needed due to a combination of factors: 1) Songbase, at some point, re-indexed all
   * their songs, so all the songs between songbasedb-v3 and the current Hymns database are
   * different. This messed up a lot of our favorites, recents, and tags, since they are keyed by
   * hymn type and hymn number. The way to solve this was to perform an exact match on the title and
   * try to infer the new hymn number. However... 2) There was a bug in iOS where tag titles weren't
   * being written, meaning we no longer have a title on which to perform the exact match.
   * </p>
   * Here, we are going through the entire songbase db and performing an exact match for every title
   * to see which songs out to be mapped to which song. Then, we will take the diff output and apply
   * it in the client, simplifying client logic.
   */
  public void diffSongbaseV3WithHymns() throws ClassNotFoundException, SQLException {
    Multimap<String, Integer> songbaseMap = parseSongbase();
    Multimap<String, SongReference> hymnsMap = parseHymns();

    List<Pair<String, Integer>> notFound = new ArrayList<>();
    List<Pair<String, Integer>> unchanged = new ArrayList<>();
    Multimap<Integer, Pair<SongReference, String>> changed = LinkedListMultimap.create();
    songbaseMap.forEach((title, songbaseNumber) -> {
      ImmutableList<SongReference> hymnsWithSameTitle =
          hymnsMap.entries().stream()
              .filter(hymnEntry -> titlesEqual(hymnEntry.getKey(), title))
              .map(Entry::getValue)
              .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getHymnNumber())))
              .collect(toImmutableList());

      Optional<SongReference> firstHymnWithSameTitle = hymnsWithSameTitle.stream().findFirst();
      if (firstHymnWithSameTitle.isEmpty()) {
        notFound.add(Pair.of(title, songbaseNumber));
        return;
      }

      if (firstHymnWithSameTitle.get().equals(
          SongReference.newBuilder().setHymnType("sb").setHymnNumber(String.valueOf(songbaseNumber)).build())) {
        unchanged.add(Pair.of(title, songbaseNumber));
        return;
      }
      changed.put(songbaseNumber, Pair.of(firstHymnWithSameTitle.get(), title));
    });
    System.out.println(notFound.size() + " dropped");
    notFound.forEach(pair -> System.out.println(pair.getValue()));

    System.out.println(unchanged.size() + " unchanged");
    unchanged.forEach(pair -> System.out.println(pair.getValue()));

    System.out.println(changed.size() + " changed");
    changed.entries().forEach(entry -> {
      System.out.println(
          String.join("|",
              String.valueOf(entry.getKey()),
              entry.getValue().getKey().getHymnType(),
              entry.getValue().getKey().getHymnNumber(),
              entry.getValue().getValue()));
    });
  }

  private boolean titlesEqual(String title1, String title2) {
    String sanitizedTitle1 = title1.toLowerCase().replaceAll("[^\\w\\s]", "");
    String sanitizedTitle2 = title2.toLowerCase().replaceAll("[^\\w\\s]", "");
    return sanitizedTitle1.equals(sanitizedTitle2);
  }

  private Multimap<String, Integer> parseSongbase() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    Connection connection = DriverManager.getConnection(SONGBASE_DATABASE_PATH);

    // book_index = 1 --> blue songbook (songbase) songs in songbsaedb-v3
    ResultSet resultSet = connection.createStatement()
        .executeQuery("SELECT book_index, title FROM songs WHERE book_id = 1");
    if (resultSet == null) {
      throw new IllegalArgumentException("songbase query returned null");
    }

    Multimap<String, Integer> titleNumberMap = ArrayListMultimap.create();
    while (resultSet.next()) {
      int bookIndex = resultSet.getInt(1);
      String title = resultSet.getString(2);
      titleNumberMap.put(title, bookIndex);
    }
    connection.close();
    return titleNumberMap;
  }

  private Multimap<String, SongReference> parseHymns() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    Optional<File> mostRecentFile = fileReadWriter.readLargestFile("storage/output",
        Optional.of("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_PDT"));
    // No file to read
    if (mostRecentFile.isEmpty()) {
      throw new IllegalStateException("No Hymns db to read from");
    }

    Connection connection = DriverManager.getConnection( // hymnaldb-v23.sqlite
        "jdbc:sqlite:" + mostRecentFile.get().getPath() + "/hymnaldb-v" + DATABASE_VERSION + ".sqlite");
    ResultSet resultSet = connection.createStatement().executeQuery(
        "SELECT HYMN_TYPE, HYMN_NUMBER, SONG_TITLE FROM SONG_DATA JOIN SONG_IDS ON SONG_DATA.ID = SONG_IDS.SONG_ID WHERE HYMN_TYPE = 'sb' OR HYMN_TYPE = 'sbx'");
    if (resultSet == null) {
      throw new IllegalArgumentException("hymns query returned null");
    }

    Multimap<String, SongReference> titleReferenceMap = ArrayListMultimap.create();
    while (resultSet.next()) {
      String hymnType = resultSet.getString(1);
      String hymnNumber = resultSet.getString(2);
      String songTitle = resultSet.getString(3);
      titleReferenceMap.put(songTitle,
          SongReference.newBuilder().setHymnType(hymnType).setHymnNumber(hymnNumber).build());
    }
    connection.close();
    return titleReferenceMap;
  }

  public static void main(String[] args) throws SQLException, ClassNotFoundException {
    DaggerPipelineComponent.create().songbasePipelineComponent().build().differ()
        .diffSongbaseV3WithHymns();
  }
}
