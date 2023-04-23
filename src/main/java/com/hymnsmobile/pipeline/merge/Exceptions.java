package com.hymnsmobile.pipeline.merge;

import static com.hymnsmobile.pipeline.models.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_TUNE;
import static com.hymnsmobile.pipeline.models.HymnType.TAGALOG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.SongReference;

public final class Exceptions {

  static final ImmutableSet<ImmutableSet<SongReference>> LANGUAGES_EXCEPTIONS = ImmutableSet.of(
      // h/1353 and h/8476 are essentially two slightly different versions of the same song. So both should link to
      // the same set of translations, since the lyrics are very similar.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1353").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8476").build(),
          SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("1353").build(),
          SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("476").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("476").build()),

      // Both h/8330 and ns/154 are valid translations of the Chinese song ch/330.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8330").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("154").build()),

      // Both ns/19 and ns/474 are valid translations of the Chinese song ts/428.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("19").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("474").build()),

      // h/505 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
      // valid translations of that song.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("383").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("383").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("27").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("27")
              .build()),

      // h/893 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
      // valid translations of that song.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("641").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("641").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("917").build(),
          SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("917")
              .build()),

      // ht/437 is from H4A, and seems like also a valid translation of h/437 as well as ht/c333
      // from hymnal.net.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("c333").build(),
          SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("437").build())
  );

  static final ImmutableSet<ImmutableSet<SongReference>> RELEVANT_EXCEPTIONS = ImmutableSet.of(
      // h/528, ns/306, and h/8444 are basically different versions of the same song.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("528").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("306").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8444").build()),

      // Both h/79 and h/8079 have the same chorus
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("79").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8079").build()),

      // Both ns/19 and ns/474 are two English translations of the same song
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("19").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("474").build()),

      // Both h/267 and h/1360 have the same chorus
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("267").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1360").build()),

      // h/720, h/8526, nt/720, and nt/720b have all different tunes of the same song
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("720").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8526").build(),
          SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("720").build(),
          SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("720b").build()),

      // h/666 is a brother Lee rewrite of h/8661
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("666").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8661").build()),

      // Both h/445 is h/1359 but without the chorus
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("445").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1359").build()),

      // Both h/1353 are h/8476 are alternate versions of each other (probably different translations of the same
      // Chinese song)
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1353").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8476").build()),

      // h/921 is the original and h/1358 is an adapted version
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("921").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1358").build()),

      // h/18 is the original and ns/7 is an adapted version
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("18").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("7").build()),

      // c/21 is a shortened version of h/70
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("21").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("70").build()),

      // ns/179 is the adapted version of h/1248
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1248").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("179").build()),

      // Both ns/154 and h/8330 are valid translations of the Chinese song ch/330.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("154").build(),
          SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8330").build()),

      // ns/547 and ns/945 don't have the same tune, but are very similar and based on the same
      // scripture passage.
      ImmutableSet.of(
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("547").build(),
          SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("945").build())
  );

  private Exceptions() {
  }
}
