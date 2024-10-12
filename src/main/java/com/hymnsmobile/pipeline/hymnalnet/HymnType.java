package com.hymnsmobile.pipeline.hymnalnet;

import java.util.Optional;

/**
 * All the hymn types currently on Hymnal.net
 */
public enum HymnType {
  CLASSIC_HYMN("h", Optional.of(1360)),
  NEW_TUNE("nt", Optional.empty()),
  NEW_SONG("ns", Optional.of(1040)),
  CHILDREN_SONG("c", Optional.of(214)),
  HOWARD_HIGASHI("lb", Optional.of(87)),
  DUTCH("hd", Optional.empty()),
  GERMAN("de", Optional.empty()),
  // We don't need to worry about simplified Chinese in here because in Hymnal.net, wherever there is a "ch/X" song,
  // there is also a "ch/X?gb=1" song. Since Hymnal.net maintains this requirement for us, we don't need to maintain
  // it here. If that changes on Hymnal.net's side, then we will need to update the pipeline to account for that.
  CHINESE("ch", Optional.empty()),
  CHINESE_SUPPLEMENTAL("ts", Optional.empty()),
  CEBUANO("cb", Optional.empty()),
  TAGALOG("ht", Optional.empty()),
  FRENCH("hf", Optional.of(215)),
  SPANISH("hs", Optional.of(500)),
  HINOS("pt", Optional.empty()),
  HEBREW("he", Optional.empty()),
  INDONESIAN("I", Optional.empty()),
  KOREAN("K", Optional.empty()),
  RUSSIAN("ru", Optional.empty()),
  JAPANESE("ja", Optional.empty()),
  ARABIC("ar", Optional.empty()),
  ESTONIAN("et", Optional.empty());

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
