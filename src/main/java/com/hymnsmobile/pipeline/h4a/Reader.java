package com.hymnsmobile.pipeline.h4a;

import static com.hymnsmobile.pipeline.h4a.BlockList.shouldBlock;

import com.hymnsmobile.pipeline.h4a.dagger.H4a;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.h4a.models.Youtube;
import com.hymnsmobile.pipeline.models.Line;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.Verse;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

@H4aPipelineScope
public class Reader {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final String DATABASE_PATH = "jdbc:sqlite:storage/h4a/h4a-piano-v4_6.sqlite";

  /**
   * Use a custom escape sequence, since Gson will auto-escape strings and screw everything up.
   * Right before we save the value, we will undo the custom escape character and replace it with
   * the standard double-quote (").
   */
  private static final String CUSTOM_ESCAPE = "$CUSESP$";

  private final Connection connection;
  private final Converter converter;
  private final Set<PipelineError> errors;
  private final Set<H4aHymn> h4aHymns;

  @Inject
  public Reader(Converter converter, @H4a Set<PipelineError> errors, Set<H4aHymn> h4aHymns) {
    try {
      Class.forName("org.sqlite.JDBC");
      this.connection = DriverManager.getConnection(DATABASE_PATH);
    } catch (ClassNotFoundException | SQLException e) {
      throw new RuntimeException("Unable to connect to h4a database", e);
    }
    this.converter = converter;
    this.errors = errors;
    this.h4aHymns = h4aHymns;
  }

  public void readDb() throws SQLException, BadHanyuPinyinOutputFormatCombination {
    ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM hymns");
    if (resultSet == null) {
      throw new IllegalArgumentException("h4a query returned null");
    }

    while (resultSet.next()) {
      H4aHymn.Builder hymn = H4aHymn.newBuilder();

      String id = resultSet.getString(1);
      H4aKey key = converter.toKey(id);
      hymn.setId(key);

      if (shouldBlock(key)) {
        LOGGER.fine(String.format("%s contained in block list. Skipping...", key));
        continue;
      }

      String author = resultSet.getString(2);
      if (!TextUtil.isEmpty(author) && author.equals("*")) {
        author = "LSM";
      }
      setIfNotEmpty(hymn, "author", author);

      String composer = resultSet.getString(3);
      // Some composers (e.g. 1151) have "Arranged by" before the real composer. This filters out that part.
      if (!TextUtil.isEmpty(composer)) {
        composer = composer.replace("Arranged by ", "");
      }
      setIfNotEmpty(hymn, "composer", composer);

      // Column 4 is 'first_chorus_line', which we don't care about.

      setIfNotEmpty(hymn, "first_stanza_line", resultSet.getString(5));

      // Column 6 is 'hymn_group', which is already captured in the id.

      setIfNotEmpty(hymn, "key", resultSet.getString(7));

      String mainCategory = resultSet.getString(8);
      setIfNotEmpty(hymn, "main_category", mainCategory);

      setIfNotEmpty(hymn, "meter", resultSet.getString(9));

      // Column 10 is 'no', which we don't care about.

      String subCategory = resultSet.getString(11);
      if (!TextUtil.isEmpty(subCategory) && !subCategory.equalsIgnoreCase(mainCategory)) {
        // If main category and sub category are the same, then we ignore sub category.
        hymn.setSubCategory(subCategory);
      }

      setIfNotEmpty(hymn, "time", resultSet.getString(12));

      setIfNotEmpty(hymn, "hymn_code", resultSet.getString(13));

      String parentHymn = resultSet.getString(14);
      if (!TextUtil.isEmpty(parentHymn)) {
        hymn.setParentHymn(converter.toKey(parentHymn));
      }

      String sheetMusicLink = resultSet.getString(15);
      if (!TextUtil.isEmpty(sheetMusicLink)) {
        hymn.setPianoSvg(sheetMusicLink.replace("_g.svg", "_p.svg"));
        hymn.setGuitarSvg(sheetMusicLink.replace("_p.svg", "_g.svg"));
      }

      String verse = resultSet.getString(16);
      if (!TextUtil.isEmpty(verse)) {
        StringBuilder scriptures = new StringBuilder();
        String[] verseReferences = verse.split(",");
        for (String verseReference : verseReferences) {
          if (!TextUtil.isEmpty(verseReference)) {
            continue;
          }
          scriptures.append(verseReference).append(";");
        }
        setIfNotEmpty(hymn, "scriptures", scriptures.toString());
      }

      String related = resultSet.getString(17);
      if (!TextUtil.isEmpty(related)) {
        for (String relatedSong : related.split(",")) {
          if (TextUtil.isEmpty(relatedSong)) {
            continue;
          }
          H4aKey relatedSongKey = converter.toKey(relatedSong);
          if (shouldBlock(relatedSongKey)) {
            continue;
          }

          HymnType relatedSongType = HymnType.fromString(relatedSongKey.getType()).orElseThrow();
          if (relatedSongType == HymnType.SPANISH_MISTYPED) {
            relatedSongKey = H4aKey.newBuilder().setType(HymnType.SPANISH.abbreviation)
                .setNumber(relatedSongKey.getNumber()).build();
          }
          hymn.addRelated(relatedSongKey);
        }
      }

      ResultSet stanzas = connection.createStatement().executeQuery(
          String.format("SELECT * FROM stanza WHERE parent_hymn='%s' ORDER BY n_order", id));
      if (stanzas == null) {
        throw new IllegalArgumentException("h4a stanzas query returned null");
      }
      while (stanzas.next()) {
        String stanzaNumber = stanzas.getString(2);
        String stanzaText = stanzas.getString(3);
        hymn.addVerses(buildVerse(key, stanzaNumber, stanzaText));
      }

      ResultSet tunes = connection.createStatement()
          .executeQuery(String.format("SELECT * FROM tune WHERE _id='%s'", id));
      if (tunes != null && tunes.next()) {
        hymn.addYoutube(
            Youtube.newBuilder().setComment(tunes.getString(2)).setVideoId(tunes.getString(3)));
      }
      h4aHymns.add(hymn.build());
    }
  }

  private Verse.Builder buildVerse(H4aKey key, String stanzaNumber, String stanzaText)
      throws BadHanyuPinyinOutputFormatCombination {
    HymnType type = HymnType.fromString(key.getType()).orElseThrow();
    Verse.Builder verse = Verse.newBuilder();

    if ("chorus".equals(stanzaNumber)) {
      verse.setVerseType("chorus");
    } else {
      verse.setVerseType("verse");
    }

    String[] lines = stanzaText.split("<br/>");
    for (String lineContent : lines) {
      if (TextUtil.isEmpty(lineContent)) {
        continue;
      }

      Line.Builder line = Line.newBuilder();
      // Use a custom escape method, since GSON will auto-escape strings and screw everything up. Right
      // before we save the value, we will undo the custom escape character and replace it with the
      // standard \".
      lineContent = lineContent.replace("\"", CUSTOM_ESCAPE + "\"");
      line.setLineContent(lineContent);
      if (type.isTransliterable()) {
        StringBuilder transliteratedLine = new StringBuilder();
        char[] transliterableChars = lineContent.toCharArray();
        for (char transliterableChar : transliterableChars) {
          HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
          format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
          format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
          format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
          String[] transliteratedArray
              = PinyinHelper.toHanyuPinyinStringArray(transliterableChar, format);
          if (transliteratedArray == null || transliteratedArray.length == 0) {
            // No transliteration exists, meaning it's probably some type of punctuation or
            // something.
            transliteratedLine.append(transliterableChar);
            continue;
          }
          String transliterated = transliteratedArray[0];
          if (transliterated.contains("none")) {
            throw new IllegalArgumentException(
                transliterableChar + " was not able to be transliterated");
          }
          transliteratedLine.append(transliterated);
        }
        line.setTransliteration(transliteratedLine.toString());
      }
      verse.addLines(line);
    }
    return verse;
  }

  private void setIfNotEmpty(H4aHymn.Builder builder, String field, String attribute) {
    if (!TextUtil.isEmpty(attribute)) {
      builder.setField(H4aHymn.getDescriptor().findFieldByName(field), attribute.trim());
    }
  }
}