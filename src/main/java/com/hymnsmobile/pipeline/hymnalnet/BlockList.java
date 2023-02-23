package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.SongReference;

/**
 * When the song on Hymnal.net is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<SongReference> BLOCK_LIST = ImmutableSet.of(
      SongReference.newBuilder().setType(HymnType.NEW_SONG).setNumber("582").build());
}
