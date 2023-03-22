package com.hymnsmobile.pipeline.h4a;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;

/**
 * When the song on Hymns 4 Android is so wrong that we just have to skip it.
 */
public final class BlockList {

  public static final ImmutableSet<H4aKey> BLOCK_LIST = ImmutableSet.<H4aKey>builder()
      // Non-existent song
      .add(H4aKey.newBuilder().setType("NS").setNumber("582").build())

      // Here are the only two German versions of new songs in the H4A db. These songs are covered
      // by Hymnal.net, so it's easier to just remove them than to create a special cases to handle
      // just two songs:
      //
      // G419 and G10002 are two different translations of "God's eternal economy" (NS180)
      .add(H4aKey.newBuilder().setType("G").setNumber("419").build())
      .add(H4aKey.newBuilder().setType("G").setNumber("10001").build())
      //
      // G10002 and G420 are two different translations of "What miracle! What mystery!" (NS151)
      .add(H4aKey.newBuilder().setType("G").setNumber("420").build())
      .add(H4aKey.newBuilder().setType("G").setNumber("10002").build())

      // Songs that show up in "related" column but don't actually exist in the h4a db. These should
      // be ignored since they map to nothing.
      .add(H4aKey.newBuilder().setType("C").setNumber("825").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("914").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("912").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("389").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("834").build())
      .add(H4aKey.newBuilder().setType("T").setNumber("898").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("815").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("486").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("806").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("905").build())
      .add(H4aKey.newBuilder().setType("BF").setNumber("1040").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("856").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("812").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("810").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("850").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("901").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("517c").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("510c").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("513c").build())
      .add(H4aKey.newBuilder().setType("CB").setNumber("57").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("925").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("917").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("840").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("352").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("158").build())
      .add(H4aKey.newBuilder().setType("CB").setNumber("1360").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("506c").build())
      .add(H4aKey.newBuilder().setType("CB").setNumber("381").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("46").build())
      .add(H4aKey.newBuilder().setType("C").setNumber("481c").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("9117").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("46").build())
      .add(H4aKey.newBuilder().setType("CS").setNumber("400").build())
      .build();

  public static boolean shouldBlock(H4aKey key) {
    HymnType type = HymnType.fromString(key.getType()).orElseThrow();
    String number = key.getNumber();

    if (type == HymnType.UNKNOWN) {
      return true;
    }

    // Tagalog songs > T1360 are often times just repeats of their English counterpart or with small
    // insignificant changes. Even when they're not repeats, they're not very useful songs.
    // Therefore, we just ignore them.
    if (type == HymnType.TAGALOG && Integer.parseInt(number) > 1360) {
      return true;
    }

    if (BLOCK_LIST.contains(key)) {
      return true;
    }

    return false;
  }
}
