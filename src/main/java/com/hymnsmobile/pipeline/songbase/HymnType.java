package com.hymnsmobile.pipeline.songbase;

import java.util.Optional;

/**
 * All the hymn types currently on Songbase
 */
public enum HymnType {
  HYMNAL("english_hymnal"),
  BLUE_SONGBOOK("blue_songbook"),
  HIMNOS("spanish_hymnal"),
  LIEDERBUCH("german_hymnal"),
  CANTIQUES("french_hymnal"),
  SONGBASE("songbase");

  public final String codeName;

  public static Optional<HymnType> fromString(String stringRepresentation) {
    for (HymnType hymnType : HymnType.values()) {
      if (stringRepresentation.equals(hymnType.codeName)) {
        return Optional.of(hymnType);
      }
    }
    return Optional.empty();
  }

  HymnType(String codeName) {
    this.codeName = codeName;
  }

  @Override
  public String toString() {
    return codeName;
  }
}
