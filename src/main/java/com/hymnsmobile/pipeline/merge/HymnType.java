package com.hymnsmobile.pipeline.merge;

/**
 * Represents the type of hymn
 */
public enum HymnType {
  CLASSIC_HYMN("h"),
  NEW_TUNE("nt"),
  NEW_SONG("ns"),
  CHILDREN_SONG("c"),
  HOWARD_HIGASHI("lb"),
  DUTCH("hd"),
  GERMAN("de"),
  CHINESE("ch"),
  CHINESE_SIMPLIFIED("chx"),
  CHINESE_SUPPLEMENTAL("ts"),
  CHINESE_SUPPLEMENTAL_SIMPLIFIED("tsx"),
  CEBUANO("cb"),
  TAGALOG("ht"),
  FRENCH("hf"),
  SPANISH("S"),
  KOREAN("K"),
  JAPANESE("J"),
  INDONESIAN("I"),
  FARSI("F"),
  RUSSIAN("R"),
  PORTUGUESE("pt"),
  BE_FILLED("bf"),
  LIEDERBUCH("lde"),
  HINOS("pt"),
  HEBREW("he"),
  BLUE_SONGBOOK("sb"),
  // Uncategorized songbase songs.
  SONGBASE_OTHER("sbx");

  /**
   * abbreviated value (eg: h, nt, ns, etc)
   */
  public final String abbreviatedValue;

  HymnType(String abbreviatedValue) {
    this.abbreviatedValue = abbreviatedValue;
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
