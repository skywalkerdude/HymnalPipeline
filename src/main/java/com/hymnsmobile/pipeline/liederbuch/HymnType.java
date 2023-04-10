package com.hymnsmobile.pipeline.liederbuch;

import java.util.Optional;

/**
 * All the hymn types currently on Liederbuch
 */
public enum HymnType {
  CLASSIC_HYMN("E"),
  NEW_SONG("NS"),
  FARSI("F"),
  CHINESE("C"),
  CHINESE_SUPPLEMENTAL("CS"),
  GERMAN("G");

  public final String abbreviation;

  public static Optional<HymnType> fromString(String stringRepresentation) {
    for (HymnType hymnType : HymnType.values()) {
      if (stringRepresentation.equals(hymnType.abbreviation)) {
        return Optional.of(hymnType);
      }
    }
    return Optional.empty();
  }

  HymnType(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  @Override
  public String toString() {
    return abbreviation;
  }
}
