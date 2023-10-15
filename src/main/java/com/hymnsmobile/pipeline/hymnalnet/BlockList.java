package com.hymnsmobile.pipeline.hymnalnet;

import static com.hymnsmobile.pipeline.hymnalnet.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.hymnalnet.HymnType.DUTCH;
import static com.hymnsmobile.pipeline.hymnalnet.HymnType.NEW_SONG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;

/**
 * When the song on Hymnal.net is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<HymnalNetKey> BLOCK_LIST = ImmutableSet.of(
      // Non-existent songs
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("582").build(),
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("881").build(),
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("978").build(),
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("981").build(),
      HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("987").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("31").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("37").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("95").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("1345").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("9").build(),
      HymnalNetKey.newBuilder().setHymnType(DUTCH.abbreviation).setHymnNumber("49").build(),
      HymnalNetKey.newBuilder().setHymnType(CLASSIC_HYMN.abbreviation).setHymnNumber("8808").build());
}
