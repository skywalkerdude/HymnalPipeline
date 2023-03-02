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

  @Inject
  public DatabaseWriter() {
    try {
      Class.forName("org.sqlite.JDBC");
      this.connection = DriverManager.getConnection(String.format(DATABASE_PATH, DATABASE_VERSION));
    } catch (ClassNotFoundException | SQLException e) {
      throw new RuntimeException("Unable to connect to database", e);
    }
  }

  void createDatabase() throws SQLException {
    connection.createStatement().execute(String.format("PRAGMA user_version = %d", DATABASE_VERSION));
    connection.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS SONG_DATA (ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, HYMN_TYPE TEXT NOT NULL, HYMN_NUMBER TEXT NOT NULL, SONG_TITLE TEXT NOT NULL, SONG_LYRICS TEXT, SONG_META_DATA_CATEGORY TEXT, SONG_META_DATA_SUBCATEGORY TEXT, SONG_META_DATA_AUTHOR TEXT, SONG_META_DATA_COMPOSER TEXT, SONG_META_DATA_KEY TEXT, SONG_META_DATA_TIME TEXT, SONG_META_DATA_METER TEXT, SONG_META_DATA_SCRIPTURES TEXT, SONG_META_DATA_HYMN_CODE TEXT, SONG_META_DATA_MUSIC TEXT, SONG_META_DATA_SVG_SHEET_MUSIC TEXT, SONG_META_DATA_PDF_SHEET_MUSIC TEXT, SONG_META_DATA_LANGUAGES TEXT, SONG_META_DATA_RELEVANT TEXT)");
    connection.createStatement().execute(
        "CREATE VIRTUAL TABLE IF NOT EXISTS SEARCH_VIRTUAL_SONG_DATA USING FTS4(SONG_TITLE TEXT NOT NULL, SONG_LYRICS TEXT NOT NULL,  tokenize=porter,  content=SONG_DATA)");
    connection.createStatement().execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_SONG_DATA_HYMN_TYPE_HYMN_NUMBER ON SONG_DATA(HYMN_TYPE, HYMN_NUMBER)");
    connection.createStatement()
        .execute("CREATE INDEX IF NOT EXISTS index_SONG_DATA_ID ON SONG_DATA(ID)");
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
    insertStatement.setString(1, hymn.getReference().getType().name()); // TODO fix
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
    insertStatement.setString(17, toJson(hymn.getLanguagesMap()));
    insertStatement.setString(18, toJson(hymn.getRelevantsMap()));
    return insertStatement.execute();
  }
}
