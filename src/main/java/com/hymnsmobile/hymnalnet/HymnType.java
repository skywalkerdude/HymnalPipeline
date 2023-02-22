package com.hymnsmobile.hymnalnet;

import java.util.Optional;

public enum HymnType {
  CLASSIC_HYMN("h", Optional.of(1360)),
  NEW_TUNE("nt", Optional.empty()),
  NEW_SONG("ns", Optional.of(852)),
  CHILDREN_SONG("c", Optional.of(213)),
  HOWARD_HIGASHI("lb", Optional.of(87)),
  DUTCH("hd", Optional.of(120)),
  GERMAN("de", Optional.empty()),
  CHINESE("ch", Optional.of(1111)),
  CHINESE_SUPPLEMENTAL("ts", Optional.of(1005)),
  CEBUANO("cb", Optional.empty()),
  TAGALOG("ht", Optional.empty()),
  FRENCH("hf", Optional.of(215)),
  SPANISH("hs", Optional.of(500));

  public final String abbreviation;

  /**
   * Max number of this hymn type on Hymnal.net, or absent if the numbers are not continguous.
   */
  public final Optional<Integer> maxNumber;

  public static Optional<HymnType> fromString(String stringRepresentation) {
    for (HymnType hymnType : HymnType.values()) {
      if (stringRepresentation.equals(hymnType.abbreviation)) {
        return Optional.of(hymnType);
      }
    }
    return Optional.empty();
  }

  HymnType(String abbreviation, Optional<Integer> maxNumber) {
    this.abbreviation = abbreviation;
    this.maxNumber = maxNumber;
  }

  @Override
  public String toString() {
    return abbreviation;
  }
}