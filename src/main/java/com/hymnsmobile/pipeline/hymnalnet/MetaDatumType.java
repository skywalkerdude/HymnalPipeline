package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.hymnalnet.models.Datum;
import com.hymnsmobile.pipeline.hymnalnet.models.MetaDatum;
import java.util.Optional;

public enum MetaDatumType {
  CATEGORY("Category"),
  SUB_CATEGORY("Subcategory"),
  AUTHOR("Lyrics", "Author"),
  COMPOSER("Music"),
  KEY("Key"),
  TIME("Time"),
  METER("Meter"),
  HYMN_CODE("Hymn Code"),
  SCRIPTURES("Scriptures"),
  MUSIC("Music"),
  SVG_SHEET("svg"),
  PDF_SHEET("Lead Sheet"),
  LANGUAGES("Languages"),
  RELEVANT("Relevant");

  public final ImmutableSet<String> jsonKeys;

  MetaDatumType(String... jsonKeys) {
    this.jsonKeys = ImmutableSet.copyOf(jsonKeys);
  }

  public static Optional<MetaDatumType> fromJsonRepresentation(MetaDatum metaDatum) {
    String name = metaDatum.getName();

    // Special case since the json key for both "Music" and "Composer" is just "Music." Therefore,
    // we need to do some special logic go differentiate them.
    if (name.equals("Music")) {
      if (metaDatum.getDataList().stream().map(Datum::getValue).anyMatch(
          value -> value.equals("mp3") || value.equals("MIDI") || value.equals("Tune (MIDI)"))) {
        return Optional.of(MUSIC);
      } else {
        return Optional.of(COMPOSER);
      }
    }

    for (MetaDatumType metaDatumType : MetaDatumType.values()) {
      if (metaDatumType.jsonKeys.contains(name)) {
        return Optional.of(metaDatumType);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return jsonKeys.toString();
  }
}