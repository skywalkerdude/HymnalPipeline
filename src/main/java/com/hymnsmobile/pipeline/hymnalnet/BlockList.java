package com.hymnsmobile.pipeline.hymnalnet;

import static com.hymnsmobile.pipeline.hymnalnet.HymnType.NEW_SONG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;

/**
 * When the song on Hymnal.net is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<HymnalNetKey> BLOCK_LIST = ImmutableSet.of(
      // Non-existent song
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("582").build());
}
