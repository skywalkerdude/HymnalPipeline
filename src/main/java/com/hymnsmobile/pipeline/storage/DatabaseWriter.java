package com.hymnsmobile.pipeline.storage;

import static com.hymnsmobile.pipeline.utils.TextUtil.join;
import static com.hymnsmobile.pipeline.utils.TextUtil.strMapToJson;
import static com.hymnsmobile.pipeline.utils.TextUtil.toJson;

import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.storage.dagger.StorageScope;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.inject.Inject;

@StorageScope
public class DatabaseWriter {

  private static final String DATABASE_PATH = "jdbc:sqlite:storage/hymnaldb-v%d.sqlite";
  private static final int DATABASE_VERSION = 20;

  private final Connection connection;
  private final Converter converter;

  @Inject
  public DatabaseWriter(Converter converter) {
    this.converter = converter;
    try {
      Class.forName("org.sqlite.JDBC");
      this.connection = DriverManager.getConnection(String.format(DATABASE_PATH, DATABASE_VERSION));
    } catch (ClassNotFoundException | SQLException e) {
      throw new RuntimeException("Unable to connect to database", e);
    }
  }

  void createDatabase() throws SQLException {
    connection.createStatement().execute(String.format("PRAGMA user_version = %d", DATABASE_VERSION));

    // SONG_DATA table
    connection.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS `SONG_DATA` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `HYMN_TYPE` TEXT NOT NULL, `HYMN_NUMBER` TEXT NOT NULL, `SONG_TITLE` TEXT NOT NULL, `SONG_LYRICS` TEXT, `SONG_META_DATA_CATEGORY` TEXT, `SONG_META_DATA_SUBCATEGORY` TEXT, `SONG_META_DATA_AUTHOR` TEXT, `SONG_META_DATA_COMPOSER` TEXT, `SONG_META_DATA_KEY` TEXT, `SONG_META_DATA_TIME` TEXT, `SONG_META_DATA_METER` TEXT, `SONG_META_DATA_SCRIPTURES` TEXT, `SONG_META_DATA_HYMN_CODE` TEXT, `SONG_META_DATA_MUSIC` TEXT, `SONG_META_DATA_SVG_SHEET_MUSIC` TEXT, `SONG_META_DATA_PDF_SHEET_MUSIC` TEXT, `SONG_META_DATA_LANGUAGES` TEXT, `SONG_META_DATA_RELEVANT` TEXT)");

    // SEARCH_VIRTUAL_SONG_DATA table
    connection.createStatement().execute("CREATE VIRTUAL TABLE IF NOT EXISTS `SEARCH_VIRTUAL_SONG_DATA` USING FTS4(`SONG_TITLE` TEXT NOT NULL, `SONG_LYRICS` TEXT NOT NULL, tokenize=porter, content=`SONG_DATA`)");
    connection.createStatement().execute("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_BEFORE_UPDATE BEFORE UPDATE ON `SONG_DATA` BEGIN DELETE FROM `SEARCH_VIRTUAL_SONG_DATA` WHERE `docid`=OLD.`rowid`; END");
    connection.createStatement().execute("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_BEFORE_DELETE BEFORE DELETE ON `SONG_DATA` BEGIN DELETE FROM `SEARCH_VIRTUAL_SONG_DATA` WHERE `docid`=OLD.`rowid`; END");
    connection.createStatement().execute("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_AFTER_UPDATE AFTER UPDATE ON `SONG_DATA` BEGIN INSERT INTO `SEARCH_VIRTUAL_SONG_DATA`(`docid`, `SONG_TITLE`, `SONG_LYRICS`) VALUES (NEW.`rowid`, NEW.`SONG_TITLE`, NEW.`SONG_LYRICS`); END");
    connection.createStatement().execute("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_SEARCH_VIRTUAL_SONG_DATA_AFTER_INSERT AFTER INSERT ON `SONG_DATA` BEGIN INSERT INTO `SEARCH_VIRTUAL_SONG_DATA`(`docid`, `SONG_TITLE`, `SONG_LYRICS`) VALUES (NEW.`rowid`, NEW.`SONG_TITLE`, NEW.`SONG_LYRICS`); END");

    // Setup queries
    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
    connection.createStatement().execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4aae818e141e45c24592189c326515c1')");
  }

  void closeDatabase() throws SQLException {
    connection.close();
  }

  boolean writeHymn(Hymn hymn) throws SQLException, IOException {
    PreparedStatement insertStatement = connection.prepareStatement(
        "INSERT INTO SONG_DATA ("
            + "HYMN_TYPE, HYMN_NUMBER, SONG_TITLE, SONG_LYRICS, SONG_META_DATA_CATEGORY, "
            + "SONG_META_DATA_SUBCATEGORY, SONG_META_DATA_AUTHOR, SONG_META_DATA_COMPOSER, "
            + "SONG_META_DATA_KEY, SONG_META_DATA_TIME, SONG_META_DATA_METER, "
            + "SONG_META_DATA_SCRIPTURES, SONG_META_DATA_HYMN_CODE, SONG_META_DATA_MUSIC, "
            + "SONG_META_DATA_SVG_SHEET_MUSIC, SONG_META_DATA_PDF_SHEET_MUSIC, "
            + "SONG_META_DATA_LANGUAGES, SONG_META_DATA_RELEVANT)"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    insertStatement.setString(1, converter.serialize(hymn.getReference().getType()));
    insertStatement.setString(2, hymn.getReference().getNumber());
    insertStatement.setString(3, hymn.getTitle());
    insertStatement.setString(4, toJson(hymn.getLyricsList()));
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
    return insertStatement.execute();
  }
}
