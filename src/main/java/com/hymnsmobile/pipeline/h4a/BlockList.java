package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.models.H4aKey;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * When the song on Hymns 4 Android is so wrong that we just have to skip it.
 */
public class BlockList {

  public enum BlockStatus {
    OK, NON_EXISTENT, BLOCKED, UNPARSEABLE;
  }

  /**
   * Songs that are blocked for miscellaneous reasons.
   */
  private static final Set<String> BLOCK_LIST = new HashSet<>() {{
    // Song "exists" in the H4a db (as well as on Hymnal.net) but is a gibberish song.
    add("NS582");

    // G10001 is a duplicate of "God's eternal economy" (NS180) and thus can just be ignored.
    add("G10001");

    // G10002 is a different translation of "What miracle! What mystery!" (NS151), but the
    // translation we want is already covered by G420, so we can skip this song.
    add("G10002");

    add("IloveYou");
    add("OJesusLord");
    add("I\'malwayscallingonYou.");
  }};

  /**
   * Songs that show up in "related" column but don't actually exist in the h4a db. These should be ignored since they
   * map to nothing.
   */
  private static final Set<String> NONEXISTENT_RELATED_SONGS = new HashSet<>() {{
    // Songs that show up in "related" column but don't actually exist in the h4a db. These should
    // be ignored since they map to nothing.
    add("C825");
    add("C914");
    add("C912");
    add("C389");
    add("C834");
    add("T898");
    add("C815");
    add("C486");
    add("C806");
    add("C905");
    add("BF1040");
    add("C856");
    add("C812");
    add("C810");
    add("C850");
    add("C901");
    add("C517c");
    add("C510c");
    add("C513c");
    add("CB57");
    add("C925");
    add("C917");
    add("C840");
    add("CS352");
    add("CS158");
    add("CB1360");
    add("C506c");
    add("CB381");
    add("C481c");
    add("CS9117");
    add("CS46");
    add("CS400");
  }};

  private final Converter converter;

  @Inject
  public BlockList(Converter converter) {
    this.converter = converter;
  }

  public BlockStatus blockStatus(String id) {
    if (BLOCK_LIST.remove(id)) {
      return BlockStatus.BLOCKED;
    }

    if (NONEXISTENT_RELATED_SONGS.remove(id)) {
      return BlockStatus.NON_EXISTENT;
    }

    Optional<H4aKey> key = converter.toKey(id);
    if (key.isEmpty()) { // Pipeline error logged already by converter.
      return BlockStatus.UNPARSEABLE;
    }

    HymnType type = HymnType.fromString(key.get().getType()).orElseThrow();
    String number = key.get().getNumber();

    if (type == HymnType.UNKNOWN_R || type == HymnType.UNKNOWN_LB) {
      return BlockStatus.BLOCKED;
    }

    // Tagalog songs > T1360 are often times just repeats of their English counterpart or with small
    // insignificant changes. Even when they're not repeats, they're not very useful songs.
    // Therefore, we just ignore them.
    if (type == HymnType.TAGALOG && Integer.parseInt(number) > 1360) {
      return BlockStatus.BLOCKED;
    }
    return BlockStatus.OK;
  }

  public Set<String> items() {
    return new HashSet<>() {{
      addAll(NONEXISTENT_RELATED_SONGS);
      addAll(BLOCK_LIST);
    }};
  }
}
