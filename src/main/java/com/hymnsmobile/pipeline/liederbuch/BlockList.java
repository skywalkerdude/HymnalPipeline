package com.hymnsmobile.pipeline.liederbuch;

import static com.hymnsmobile.pipeline.liederbuch.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.liederbuch.HymnType.NEW_SONG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;

/**
 * When the song on Hymnal.net is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<LiederbuchKey> BLOCK_LIST = ImmutableSet.of(
      // Non-existent song
      LiederbuchKey.newBuilder().setType(NEW_SONG.abbreviation).setNumber("582").build(),

      // Duplicates of G419 and G420 respectively
      LiederbuchKey.newBuilder().setType(GERMAN.abbreviation).setNumber("10001").build(),
      LiederbuchKey.newBuilder().setType(GERMAN.abbreviation).setNumber("10002").build());
}
