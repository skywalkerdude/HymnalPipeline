package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.dagger.MiscBlockList;
import com.hymnsmobile.pipeline.h4a.dagger.NonExistentRelatedSongs;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * When the song on Hymns 4 Android is so wrong that we just have to skip it.
 */
public class BlockList {

  public enum BlockStatus {
    OK, NON_EXISTENT, BLOCKED, UNPARSEABLE;
  }

  private final Converter converter;
  private final List<String> miscBlockList;
  private final List<String> nonExistentRelatedSongs;

  @Inject
  public BlockList(Converter converter,
                   @MiscBlockList List<String> miscBlockList,
                   @NonExistentRelatedSongs List<String> nonExistentRelatedSongs) {
    this.converter = converter;
    this.miscBlockList = miscBlockList;
    this.nonExistentRelatedSongs = nonExistentRelatedSongs;
  }

  public BlockStatus blockStatus(String id) {
    if (miscBlockList.remove(id)) {
      return BlockStatus.BLOCKED;
    }

    if (nonExistentRelatedSongs.remove(id)) {
      return BlockStatus.NON_EXISTENT;
    }

    Optional<H4aKey> key = converter.toKey(id);
    if (key.isEmpty()) { // Pipeline error logged already by converter.
      return BlockStatus.UNPARSEABLE;
    }

    HymnType type = HymnType.fromString(key.get().getType()).orElseThrow();
    String number = key.get().getNumber();

    // Songs in the form of CXXXc is usually the H4a representation of the hymnal.net hymn ns/XXXc. We have already
    // ingested this as part of the Hymnal.net pipeline and wrote it in the form ch/nsXXXc, so we can ignore them here.
    // In fact, these are typically non-existent in the H4a database.
    if (type == HymnType.CHINESE && number.endsWith("c")) {
      return BlockStatus.NON_EXISTENT;
    }

    if (type == HymnType.UNKNOWN_R || type == HymnType.UNKNOWN_LB) {
      return BlockStatus.NON_EXISTENT;
    }

    // Tagalog songs > T1360 are often times just repeats of their English counterpart or with small
    // insignificant changes. Even when they're not repeats, they're not very useful songs.
    // Therefore, we just ignore them.
    if (type == HymnType.TAGALOG && Integer.parseInt(number) > 1360) {
      return BlockStatus.BLOCKED;
    }

    // Spanish songs > S500 are part of a draft of newly translated Spanish songs from NYCYPCD. They will be published
    // at some point as a new hymnal, but as of now, they are not cleaned up enough to be able to use. Therefore, we
    // just ignore them.
    if (type == HymnType.SPANISH && Integer.parseInt(number) > 500) {
      return BlockStatus.BLOCKED;
    }
    return BlockStatus.OK;
  }

  public List<String> items() {
    return new ArrayList<>() {{
      addAll(nonExistentRelatedSongs);
      addAll(miscBlockList);
    }};
  }
}
