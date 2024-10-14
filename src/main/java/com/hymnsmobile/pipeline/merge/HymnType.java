package com.hymnsmobile.pipeline.merge;

import com.hymnsmobile.pipeline.models.Language;

/**
 * Represents the type of hymn
 */
public enum HymnType {
  CLASSIC_HYMN("h", Language.ENGLISH),
  NEW_TUNE("nt", Language.ENGLISH),
  NEW_SONG("ns", Language.ENGLISH),
  CHILDREN_SONG("c", Language.ENGLISH),
  HOWARD_HIGASHI("lb", Language.ENGLISH),
  DUTCH("hd", Language.DUTCH),
  GERMAN("de", Language.GERMAN),
  CHINESE("ch", Language.CHINESE_TRADITIONAL),
  CHINESE_SIMPLIFIED("chx", Language.CHINESE_SIMPLIFIED),
  CHINESE_SUPPLEMENTAL("ts", Language.CHINESE_TRADITIONAL),
  CHINESE_SUPPLEMENTAL_SIMPLIFIED("tsx", Language.CHINESE_SIMPLIFIED),
  CEBUANO("cb", Language.CEBUANO),
  TAGALOG("ht", Language.TAGALOG),
  FRENCH("hf", Language.FRENCH),
  SPANISH("S", Language.SPANISH),
  KOREAN("K", Language.KOREAN),
  JAPANESE("J", Language.JAPANESE),
  INDONESIAN("I", Language.INDONESIAN),
  FARSI("F", Language.FARSI),
  RUSSIAN("R", Language.RUSSIAN),
  PORTUGUESE("pt", Language.PORTUGUESE),
  BE_FILLED("bf", Language.ENGLISH),
  LIEDERBUCH("lde", Language.GERMAN),
  HINOS("pt", Language.PORTUGUESE),
  HEBREW("he", Language.HEBREW),
  SLOVAK("sk", Language.SLOVAK),
  ESTONIAN("et", Language.ESTONIAN),
  ARABIC("ar", Language.ARABIC),
  BLUE_SONGBOOK("sb", Language.ENGLISH),
  LIEDBOEK("lbk", Language.DUTCH), // Dutch Hymnal
  // Uncategorized songbase songs.
  SONGBASE_OTHER("sbx", Language.UNKNOWN);

  /**
   * abbreviated value (eg: h, nt, ns, etc)
   */
  public final String abbreviatedValue;

  public final Language language;

  HymnType(String abbreviatedValue, Language language) {
    this.abbreviatedValue = abbreviatedValue;
    this.language = language;
  }

  public static HymnType fromString(String str) {
    for (HymnType hymnType : HymnType.values()) {
      if (hymnType.abbreviatedValue.equals(str)) {
        return hymnType;
      }
    }
    throw new IllegalArgumentException("Hymn type not found: " + str);
  }
}
