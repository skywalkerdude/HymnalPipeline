package com.hymnsmobile.pipeline.hymnalnet;

import static com.hymnsmobile.pipeline.models.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.FRENCH;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_TUNE;
import static com.hymnsmobile.pipeline.models.HymnType.TAGALOG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.SongReference;

public final class Exceptions {

  static final ImmutableSet<ImmutableSet<SongReference>> HYMNAL_DB_LANGUAGES_EXCEPTIONS = ImmutableSet.of(
      // Both h/1353 and h/8476 are valid translations of the Chinese song ch/476 and the French song hf/129.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1353").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8476").build(),
          SongReference.newBuilder().setType(FRENCH).setNumber("1353").build(),
          SongReference.newBuilder().setType(TAGALOG).setNumber("1353").build(),
          SongReference.newBuilder().setType(CHINESE).setNumber("1353").build(),
          SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("1353").build()),

      // Both h/8330 and ns/154 are valid translations of the Chinese song ch/330.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8330").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("154").build()),

      // Both ns/19 and ns/474 are valid translations of the Chinese song ts/428.
      ImmutableSet.of(
          SongReference.newBuilder().setType(NEW_SONG).setNumber("19").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("474").build()),

      // h/505 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
      // valid translations of that song.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CHINESE).setNumber("383").build(),
          SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("383").build(),
          SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("27").build(),
          SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setNumber("27")
              .build()),

      // h/893 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
      // valid translations of that song.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CHINESE).setNumber("641").build(),
          SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("641").build(),
          SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("917").build(),
          SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setNumber("917")
              .build()),

      // h/1353 and h/8476 are essentially two slightly different versions of the same song. So both should link to
      // the same set of translations, since the lyrics are very similar.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1353").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8476").build(),
          SongReference.newBuilder().setType(TAGALOG).setNumber("1353").build(),
          SongReference.newBuilder().setType(CHINESE).setNumber("476").build(),
          SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("476").build()),

      // TODO H4A
      // T437 is from H4A, and seems like also a valid translation of h/437 as well as ht/c333
      ImmutableSet.of(
          SongReference.newBuilder().setType(TAGALOG).setNumber("c333").build(),
          SongReference.newBuilder().setType(TAGALOG).setNumber("437").build())
  );

  static final ImmutableSet<ImmutableSet<SongReference>> HYMNAL_DB_RELEVANT_EXCEPTIONS = ImmutableSet.of(
      // h/528, ns/306, and h/8444 are basically different versions of the same song.
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("528").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("306").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8444").build()),

      // Both h/79 and h/8079 have the same chorus
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("79").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8079").build()),

      // Both ns/19 and ns/474 are two English translations of the same song
      ImmutableSet.of(
          SongReference.newBuilder().setType(NEW_SONG).setNumber("19").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("474").build()),

      // Both h/267 and h/1360 have the same chorus
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("267").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1360").build()),

      // h/720, h/8526, nt/720, and nt/720b have all different tunes of the same song
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("720").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8526").build(),
          SongReference.newBuilder().setType(NEW_TUNE).setNumber("720").build(),
          SongReference.newBuilder().setType(NEW_TUNE).setNumber("720b").build()),

      // h/666 is a brother Lee rewrite of h/8661
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("666").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8661").build()),

      // Both h/445 is h/1359 but without the chorus
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("445").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1359").build()),

      // Both h/1353 are h/8476 are alternate versions of each other (probably different translations of the same
      // Chinese song)
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1353").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8476").build()),

      // h/921 is the original and h/1358 is an adapted version
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("921").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1358").build()),

      // h/18 is the original and ns/7 is an adapted version
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("18").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("7").build()),

      // c/21 is a shortened version of h/70
      ImmutableSet.of(
          SongReference.newBuilder().setType(CHILDREN_SONG).setNumber("21").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("70").build()),

      // c/162 is a shortened version of h/993
      ImmutableSet.of(
          SongReference.newBuilder().setType(CHILDREN_SONG).setNumber("162").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("993").build()),

      // ns/179 is the adapted version of h/1248
      ImmutableSet.of(
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1248").build(),
          SongReference.newBuilder().setType(NEW_SONG).setNumber("179").build()),

      // Both ns/154 and h/8330 are valid translations of the Chinese song ch/330.
      ImmutableSet.of(
          SongReference.newBuilder().setType(NEW_SONG).setNumber("154").build(),
          SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("8330").build())
  );

  private Exceptions() {
  }
}