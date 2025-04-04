package com.hymnsmobile.pipeline.storage;

import com.hymnsmobile.pipeline.storage.dagger.StorageScope;
import com.hymnsmobile.pipeline.storage.models.HymnEntity;
import com.hymnsmobile.pipeline.storage.models.HymnIdentifierEntity;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.*;
import java.time.ZonedDateTime;

import static com.hymnsmobile.pipeline.utils.TextUtil.join;

@StorageScope
public class DatabaseWriter {

  private final ZonedDateTime currentTime;

  @Inject
  public DatabaseWriter(ZonedDateTime currentTime) {
    this.currentTime = currentTime;
  }

  Connection createDatabase(String databasePath, boolean writeBinaryProtos) {
    String protoType = writeBinaryProtos ? "BLOB" : "TEXT";

    try {
      Class.forName("org.sqlite.JDBC");
      Connection connection = DriverManager.getConnection(databasePath);
      connection.createStatement()
          .execute(String.format("PRAGMA user_version = %d", StoragePipeline.DATABASE_VERSION));

      // SONG_IDS table
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS `SONG_IDS`("
              + "`HYMN_TYPE` INTEGER NOT NULL, "
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
              + "`SONG_LYRICS` " + protoType + ", "
              + "`INLINE_CHORDS` " + protoType + ", "
              + "`SONG_META_DATA_CATEGORY` TEXT, "
              + "`SONG_META_DATA_SUBCATEGORY` TEXT, "
              + "`SONG_META_DATA_AUTHOR` TEXT, "
              + "`SONG_META_DATA_COMPOSER` TEXT, "
              + "`SONG_META_DATA_KEY` TEXT, "
              + "`SONG_META_DATA_TIME` TEXT, "
              + "`SONG_META_DATA_METER` TEXT, "
              + "`SONG_META_DATA_SCRIPTURES` TEXT, "
              + "`SONG_META_DATA_HYMN_CODE` TEXT, "
              + "`SONG_META_DATA_MUSIC` " + protoType + ", "
              + "`SONG_META_DATA_SVG_SHEET_MUSIC` " + protoType + ", "
              + "`SONG_META_DATA_PDF_SHEET_MUSIC` " + protoType + ", "
              + "`SONG_META_DATA_LANGUAGES` " + protoType + ", "
              + "`SONG_META_DATA_RELEVANTS` " + protoType + ", "
              + "`FLATTENED_LYRICS` TEXT, "
              + "`SONG_LANGUAGE` INTEGER)");
      connection.createStatement().execute("CREATE INDEX IF NOT EXISTS `index_SONG_DATA_ID` ON `SONG_DATA` (`ID`)");

      // SEARCH_VIRTUAL_SONG_DATA table
      connection.createStatement().execute(
          "CREATE VIRTUAL TABLE IF NOT EXISTS `SEARCH_VIRTUAL_SONG_DATA` "
              + "USING FTS4(`SONG_TITLE` TEXT, `FLATTENED_LYRICS` TEXT NOT NULL, "
              + "tokenize=simple, content=`SONG_DATA`)");

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
          "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8d5d38c742c904aa7de4ba863b13dd78')");

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

  void writeHymn(Connection connection, HymnEntity hymn, boolean writeBinaryProtos)
      throws SQLException, IOException {
    PreparedStatement insertStatement = connection.prepareStatement(
        "INSERT INTO SONG_DATA ("
            + "ID, SONG_TITLE, SONG_LYRICS, INLINE_CHORDS, SONG_META_DATA_CATEGORY, "
            + "SONG_META_DATA_SUBCATEGORY, SONG_META_DATA_AUTHOR, SONG_META_DATA_COMPOSER, "
            + "SONG_META_DATA_KEY, SONG_META_DATA_TIME, SONG_META_DATA_METER, "
            + "SONG_META_DATA_SCRIPTURES, SONG_META_DATA_HYMN_CODE, SONG_META_DATA_MUSIC, "
            + "SONG_META_DATA_SVG_SHEET_MUSIC, SONG_META_DATA_PDF_SHEET_MUSIC, "
            + "SONG_META_DATA_LANGUAGES, SONG_META_DATA_RELEVANTS, FLATTENED_LYRICS, SONG_LANGUAGE) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);
    insertStatement.setLong(1, hymn.getId());
    insertStatement.setString(2, stripHymnColon(hymn.getTitle()));
    if (writeBinaryProtos) {
      insertStatement.setBytes(3, hymn.getLyrics().toByteArray());
      insertStatement.setBytes(4, hymn.getInlineChords().toByteArray());
    } else {
      insertStatement.setString(3, hymn.getLyrics().toString());
      insertStatement.setString(4, hymn.getInlineChords().toString());
    }
    insertStatement.setString(5, join(hymn.getCategoryList()));
    insertStatement.setString(6, join(hymn.getSubcategoryList()));
    insertStatement.setString(7, join(hymn.getAuthorList()));
    insertStatement.setString(8, join(hymn.getComposerList()));
    insertStatement.setString(9, join(hymn.getKeyList()));
    insertStatement.setString(10, join(hymn.getTimeList()));
    insertStatement.setString(11, join(hymn.getMeterList()));
    insertStatement.setString(12, join(hymn.getScripturesList()));
    insertStatement.setString(13, join(hymn.getHymnCodeList()));
    if (writeBinaryProtos) {
      insertStatement.setBytes(14, hymn.getMusic().toByteArray());
      insertStatement.setBytes(15, hymn.getSvgSheet().toByteArray());
      insertStatement.setBytes(16, hymn.getPdfSheet().toByteArray());
      insertStatement.setBytes(17, hymn.getLanguages().toByteArray());
      insertStatement.setBytes(18, hymn.getRelevants().toByteArray());
    } else {
      insertStatement.setString(14, hymn.getMusic().toString());
      insertStatement.setString(15, hymn.getSvgSheet().toString());
      insertStatement.setString(16, hymn.getPdfSheet().toString());
      insertStatement.setString(17, hymn.getLanguages().toString());
      insertStatement.setString(18, hymn.getRelevants().toString());
    }
    insertStatement.setString(19, hymn.getFlattenedLyrics());
    insertStatement.setInt(20, hymn.getLanguage().getNumber());
    insertStatement.execute();

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
      for (HymnIdentifierEntity hymnIdentifier : hymn.getReferencesList()) {
        if (writeBinaryProtos) {
          songIdInsert.setInt(1, hymnIdentifier.getHymnType().getNumber());
        } else {
          songIdInsert.setString(1, hymnIdentifier.getHymnType().name());
        }
        songIdInsert.setString(2, hymnIdentifier.getHymnNumber());
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
