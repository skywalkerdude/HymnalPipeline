package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableSet;

/**
 * When the song on Hymnal.net is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<HymnalDbKey> BLOCK_LIST = ImmutableSet.of(
      HymnalDbKey.create(HymnType.NEW_SONG, "584"));

}
