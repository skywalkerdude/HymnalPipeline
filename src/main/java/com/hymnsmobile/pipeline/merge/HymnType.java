package com.hymnsmobile.pipeline.merge;

/**
 * Represents the type of hymn
 */
public enum HymnType {
  CLASSIC_HYMN("h", HymnLanguage.ENGLISH),
  NEW_TUNE("nt", HymnLanguage.ENGLISH),
  NEW_SONG("ns", HymnLanguage.ENGLISH),
  CHILDREN_SONG("c", HymnLanguage.ENGLISH),
  HOWARD_HIGASHI("lb", HymnLanguage.ENGLISH),
  DUTCH("hd", HymnLanguage.DUTCH),
  GERMAN("de", HymnLanguage.GERMAN),
  CHINESE("ch", HymnLanguage.CHINESE_TRADITIONAL),
  CHINESE_SIMPLIFIED("chx", HymnLanguage.CHINESE_SIMPLIFIED),
  CHINESE_SUPPLEMENTAL("ts", HymnLanguage.CHINESE_TRADITIONAL),
  CHINESE_SUPPLEMENTAL_SIMPLIFIED("tsx", HymnLanguage.CHINESE_SIMPLIFIED),
  CEBUANO("cb", HymnLanguage.CEBUANO),
  TAGALOG("ht", HymnLanguage.TAGALOG),
  FRENCH("hf", HymnLanguage.FRENCH),
  SPANISH("S", HymnLanguage.SPANISH),
  KOREAN("K", HymnLanguage.KOREAN),
  JAPANESE("J", HymnLanguage.JAPANESE),
  INDONESIAN("I", HymnLanguage.INDONESIAN),
  FARSI("F", HymnLanguage.FARSI),
  RUSSIAN("R", HymnLanguage.RUSSIAN),
  PORTUGUESE("pt", HymnLanguage.PORTUGUESE),
  BE_FILLED("bf", HymnLanguage.ENGLISH),
  LIEDERBUCH("lde", HymnLanguage.GERMAN),
  HINOS("pt", HymnLanguage.PORTUGUESE),
  HEBREW("he", HymnLanguage.HEBREW),
  SLOVAK("sk", HymnLanguage.SLOVAK),
  ESTONIAN("et", HymnLanguage.ESTONIAN),
  ARABIC("ar", HymnLanguage.ARABIC),
  BLUE_SONGBOOK("sb", HymnLanguage.ENGLISH),
  LIEDBOEK("lbk", HymnLanguage.DUTCH), // Dutch Hymnal
  // Uncategorized songbase songs.
  SONGBASE_OTHER("sbx", HymnLanguage.UNKNOWN);

  /**
   * abbreviated value (eg: h, nt, ns, etc)
   */
  public final String abbreviatedValue;

  public final HymnLanguage language;

  HymnType(String abbreviatedValue, HymnLanguage language) {
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
