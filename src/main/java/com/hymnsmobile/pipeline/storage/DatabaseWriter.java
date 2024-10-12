package com.hymnsmobile.pipeline.storage;

import static com.hymnsmobile.pipeline.utils.TextUtil.join;
import static com.hymnsmobile.pipeline.utils.TextUtil.strMapToJson;
import static com.hymnsmobile.pipeline.utils.TextUtil.toJson;

import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.storage.dagger.StorageScope;
import dagger.Lazy;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import javax.inject.Inject;

@StorageScope
public class DatabaseWriter {

  private static final String DATABASE_PATH_FORMAT = "jdbc:sqlite:%s/hymnaldb-v%d.sqlite";
  public static final int DATABASE_VERSION = 28;

  private final Lazy<File> outputDirectory;
  private final ZonedDateTime currentTime;

  @Inject
  public DatabaseWriter(Lazy<File> outputDirectory, ZonedDateTime currentTime) {
    this.currentTime = currentTime;
    this.outputDirectory = outputDirectory;
  }

  Connection createDatabase() {
    try {
      Class.forName("org.sqlite.JDBC");
      Connection connection = DriverManager.getConnection(
          String.format(DATABASE_PATH_FORMAT, outputDirectory.get().getPath(), DATABASE_VERSION));
      connection.createStatement()
          .execute(String.format("PRAGMA user_version = %d", DATABASE_VERSION));

      // SONG_IDS table
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS `SONG_IDS`("
              + "`HYMN_TYPE` TEXT NOT NULL, "
              + "`HYMN_NUMBER` TEXT NOT NULL, "
              + "`SONG_ID` INTEGER NOT NULL, "
              + "PRIMARY KEY (`HYMN_TYPE`, `HYMN_NUMBER`), "
              + "FOREIGN KEY(`SONG_ID`) REFERENCES `SONG_DATA`(`ID`)"
              + "ON UPDATE NO ACTION ON DELETE CASCADE)");
      connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS `index_SONG_IDS_HYMN_TYPE_HYMN_NUMBER` ON `SONG_IDS` (`HYMN_TYPE`, `HYMN_NUMBER`)");
      connection.createStatement().execute("CREATE INDEX IF NOT EXISTS `index_SONG_IDS_SONG_ID` ON `SONG_IDS` (`SONG_ID`)");

      // SONG_DATA table
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS `SONG_DATA` ("
              + "`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
              + "`SONG_TITLE` TEXT, "
              + "`SONG_LYRICS` TEXT, "
              + "`INLINE_CHORDS` TEXT, "
              + "`SONG_META_DATA_CATEGORY` TEXT, "
              + "`SONG_META_DATA_SUBCATEGORY` TEXT, "
              + "`SONG_META_DATA_AUTHOR` TEXT, "
              + "`SONG_META_DATA_COMPOSER` TEXT, "
              + "`SONG_META_DATA_KEY` TEXT, "
              + "`SONG_META_DATA_TIME` TEXT, "
              + "`SONG_META_DATA_METER` TEXT, "
              + "`SONG_META_DATA_SCRIPTURES` TEXT, "
              + "`SONG_META_DATA_HYMN_CODE` TEXT, "
              + "`SONG_META_DATA_MUSIC` TEXT, "
              + "`SONG_META_DATA_SVG_SHEET_MUSIC` TEXT, "
              + "`SONG_META_DATA_PDF_SHEET_MUSIC` TEXT, "
              + "`SONG_META_DATA_LANGUAGES` TEXT, "
              + "`SONG_META_DATA_RELEVANT` TEXT,"
              + "`FLATTENED_LYRICS` TEXT,"
              + "`LANGUAGE` TEXT)");
      connection.createStatement().execute("CREATE INDEX IF NOT EXISTS `index_SONG_DATA_ID` ON `SONG_DATA` (`ID`)");

      // SEARCH_VIRTUAL_SONG_DATA table
      connection.createStatement().execute(
          "CREATE VIRTUAL TABLE IF NOT EXISTS `SEARCH_VIRTUAL_SONG_DATA` "
              + "USING FTS4(`SONG_TITLE` TEXT, `FLATTENED_LYRICS` TEXT NOT NULL, "
              + "tokenize=porter, content=`SONG_DATA`)");

      connection.createStatement().execute(
          "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_BEFORE_UPDATE BEFORE UPDATE ON `SONG_DATA` BEGIN DELETE FROM `SEARCH_VIRTUAL_SONG_DATA` WHERE `docid`=OLD.`rowid`; END");
      connection.createStatement().execute(
          "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_BEFORE_DELETE BEFORE DELETE ON `SONG_DATA` BEGIN DELETE FROM `SEARCH_VIRTUAL_SONG_DATA` WHERE `docid`=OLD.`rowid`; END");
      connection.createStatement().execute(
          "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_AFTER_UPDATE AFTER UPDATE ON `SONG_DATA` BEGIN INSERT INTO `SEARCH_VIRTUAL_SONG_DATA`(`docid`, `SONG_TITLE`, `FLATTENED_LYRICS`) VALUES (NEW.`rowid`, NEW.`SONG_TITLE`, NEW.`FLATTENED_LYRICS`); END");
      connection.createStatement().execute(
          "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_AFTER_INSERT AFTER INSERT ON `SONG_DATA` BEGIN INSERT INTO `SEARCH_VIRTUAL_SONG_DATA`(`docid`, `SONG_TITLE`, `FLATTENED_LYRICS`) VALUES (NEW.`rowid`, NEW.`SONG_TITLE`, NEW.`FLATTENED_LYRICS`); END");

      // Setup queries
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
      connection.createStatement().execute(
          "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a5ef7790f4ea35ca75baeef7eea7a216')");

      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS misc_meta_data (metadata_key TEXT, metadata_value TEXT)");
      PreparedStatement insertStatement = connection.prepareStatement(
          "INSERT INTO misc_meta_data (metadata_key, metadata_value) VALUES (?, ?)");
      insertStatement.setString(1, "time_generated");
      insertStatement.setString(2, String.valueOf(currentTime.toInstant().toEpochMilli()));
      insertStatement.execute();

      return connection;
    } catch (ClassNotFoundException | SQLException e) {
      throw new RuntimeException("Unable to connect to database", e);
    }
  }

  void closeDatabase(Connection connection) throws SQLException {
    connection.close();
  }

  void writeHymn(Connection connection, Hymn hymn)
      throws SQLException, IOException {
    PreparedStatement insertStatement = connection.prepareStatement(
        "INSERT INTO SONG_DATA ("
            + "ID, SONG_TITLE, SONG_LYRICS, INLINE_CHORDS, SONG_META_DATA_CATEGORY, "
            + "SONG_META_DATA_SUBCATEGORY, SONG_META_DATA_AUTHOR, SONG_META_DATA_COMPOSER, "
            + "SONG_META_DATA_KEY, SONG_META_DATA_TIME, SONG_META_DATA_METER, "
            + "SONG_META_DATA_SCRIPTURES, SONG_META_DATA_HYMN_CODE, SONG_META_DATA_MUSIC, "
            + "SONG_META_DATA_SVG_SHEET_MUSIC, SONG_META_DATA_PDF_SHEET_MUSIC, "
            + "SONG_META_DATA_LANGUAGES, SONG_META_DATA_RELEVANT, FLATTENED_LYRICS, LANGUAGE)"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);
    insertStatement.setInt(1, hymn.getId());
    insertStatement.setString(2, stripHymnColon(hymn.getTitle()));
    insertStatement.setString(3, toJson(hymn.getLyricsList()));
    insertStatement.setString(4, toJson(hymn.getInlineChordsList()));
    insertStatement.setString(5, join(hymn.getCategoryList()));
    insertStatement.setString(6, join(hymn.getSubCategoryList()));
    insertStatement.setString(7, join(hymn.getAuthorList()));
    insertStatement.setString(8, join(hymn.getComposerList()));
    insertStatement.setString(9, join(hymn.getKeyList()));
    insertStatement.setString(10, join(hymn.getTimeList()));
    insertStatement.setString(11, join(hymn.getMeterList()));
    insertStatement.setString(12, join(hymn.getScripturesList()));
    insertStatement.setString(13, join(hymn.getHymnCodeList()));
    insertStatement.setString(14, strMapToJson(hymn.getMusicMap()));
    insertStatement.setString(15, strMapToJson(hymn.getSvgSheetMap()));
    insertStatement.setString(16, strMapToJson(hymn.getPdfSheetMap()));
    insertStatement.setString(17, toJson(hymn.getLanguagesList()));
    insertStatement.setString(18, toJson(hymn.getRelevantsList()));
    insertStatement.setString(19, hymn.getFlattenedLyrics());
    insertStatement.setString(20, hymn.getLanguage());

    try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
      if (!generatedKeys.next()) {
        throw new SQLException("Unable to obtain ID from recently inserted hymn.");
      }
      long id = generatedKeys.getLong(1);
      if ((int) id != hymn.getId()) {
        throw new IllegalStateException("id not equal to hymn id");
      }
      PreparedStatement songIdInsert = connection.prepareStatement(
          "INSERT INTO SONG_IDS (HYMN_TYPE, HYMN_NUMBER, SONG_ID) VALUES (?, ?, ?)");
      for (SongReference songReference : hymn.getReferencesList()) {
        songIdInsert.setString(1, songReference.getHymnType());
        songIdInsert.setString(2, songReference.getHymnNumber());
        songIdInsert.setLong(3, id);
        songIdInsert.execute();
        songIdInsert.clearParameters();
      }
    }
  }

  /**
   * Many hymn titles prepend "Hymn: " to the title. It is unnecessary and takes up screen space, so
   * we strip it out whenever possible.
   */
  public static String stripHymnColon(String title) {
    if (title == null || title.isEmpty()) {
      return null;
    }
    return title.replace("Hymn: ", "");
  }
}
