package com.hymnsmobile.pipeline.h4a;

import java.util.Optional;

/**
 * All the hymn types currently on Hymnal.net
 */
public enum HymnType {
  CLASSIC_HYMN("E"),
  NEW_SONG("NS"),
  CHILDREN_SONG("CH"),
  HOWARD_HIGASHI("HH"),
  BE_FILLED("BF"),
  GERMAN("G"),
  CHINESE("C"),
  CHINESE_SIMPLIFIED("Z"),
  CHINESE_SUPPLEMENTAL("CS"),
  CHINESE_SUPPLEMENTAL_SIMPLIFIED("ZS"),
  CEBUANO("CB"),
  TAGALOG("T"),
  FRENCH("FR"),
  SPANISH("S"),
  KOREAN("K"),
  JAPANESE("J"),
  FARSI("F"),
  INDONESIAN("I"),
  SLOVAK("SK"),
  // Not sure what ths "R" category is. It exists in the "related" column, but no songs have it as
  // part of its id.
  UNKNOWN_R("R"),
  // Not sure what ths "LB" category is. It exists in the "related" column, but no songs have it as
  // part of its id.
  UNKNOWN_LB("LB");

  public final String abbreviation;

  public static Optional<HymnType> fromString(String stringRepresentation) {
    for (HymnType hymnType : HymnType.values()) {
      if (stringRepresentation.equals(hymnType.abbreviation)) {
        return Optional.of(hymnType);
      }
    }
    return Optional.empty();
  }

  public boolean isTransliterable() {
    return this == HymnType.CHINESE || this == HymnType.CHINESE_SUPPLEMENTAL ||
        this == CHINESE_SIMPLIFIED || this == CHINESE_SUPPLEMENTAL_SIMPLIFIED;
  }

  HymnType(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  @Override
  public String toString() {
    return abbreviation;
  }
}
