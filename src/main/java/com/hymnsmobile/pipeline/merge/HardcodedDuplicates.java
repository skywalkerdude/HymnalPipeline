package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.hymnsmobile.pipeline.merge.patchers.Patcher;
import com.hymnsmobile.pipeline.models.SongReference;

/**
 * Directory of hardcoded duplicate songs that are not obviously duplicates of something that
 * already exists in the (i.e. shares a parent hymn), but we know as a fact is a duplicate hymn.
 */
public class HardcodedDuplicates {

  public static ImmutableMap<SongReference, SongReference> H4A_DUPLICATES = ImmutableMap.of(
          "bf/157", "h/8330",
          "bf/69", "h/8773",
          "I/2025", "I/ns6i"
      ).entrySet().stream()
      .collect(toImmutableMap(
          entry -> Patcher.createFromStringAbbreviation(entry.getKey()),
          entry -> Patcher.createFromStringAbbreviation(entry.getValue())));
}
