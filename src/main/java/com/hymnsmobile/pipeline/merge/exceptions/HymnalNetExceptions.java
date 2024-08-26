package com.hymnsmobile.pipeline.merge.exceptions;

import static com.hymnsmobile.pipeline.merge.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.merge.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.merge.HymnType.NEW_TUNE;
import static com.hymnsmobile.pipeline.merge.HymnType.TAGALOG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.HymnType;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.SongReference;
import javax.inject.Inject;

@MergeScope
public class HymnalNetExceptions extends Exceptions {

  @Inject
  public HymnalNetExceptions() {}

  public final ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
    return ImmutableSet.of(
        // h/1353 and h/8476 are essentially two slightly different versions of the same song. So both should link to
        // the same set of translations, since the lyrics are very similar.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1353").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8476").build(),
            SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("1353")
                .build(),
            SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("476")
                .build(),
            SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("476").build()),

        // Both h/8330 and ns/154 are valid translations of the Chinese song ch/330.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8330").build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("154")
                .build()),

        // Both ns/19 and ns/474 are valid translations of the Chinese song ts/428.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("19")
                .build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("474")
                .build()),

        // h/505 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
        // valid translations of that song.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("383")
                .build(),
            SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("383").build(),
            SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue)
                .setHymnNumber("27").build(),
            SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("27")
                .build()),

        // h/893 seems to have two linked Chinese songs, that from my investigation via Google Translate, both are
        // valid translations of that song.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("641")
                .build(),
            SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("641").build(),
            SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue)
                .setHymnNumber("917").build(),
            SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("917")
                .build()),

        // h/8526 is an alternate tune of h/720.
        //
        // In Hymnal.net, ch/526 play the same tune as h/8526, while cb/720, ht/720, de/720, and
        // pt/720 play the same tune as h/720. Technically, h/8526 and ch/526 should be in their own
        // language group, while h/720, cb/720, de/720, and pt/720 should be in their own language
        // group. However, this means that someone on h/720 won't be able to find the Chinese
        // version of that song, which may seem like a mistake.
        //
        // Therefore, we are going to allow both classic hymns to be in the same language group as
        // an exception to reduce confusion.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("720").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8526").build())
    );
  }

  public final ImmutableSet<ImmutableSet<SongReference>> relevantExceptions() {
    return ImmutableSet.of(
        // h/528, ns/306, and h/8444 are basically different versions of the same song.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("528").build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("306")
                .build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8444").build()),

        // Both h/79 and h/8079 have the same chorus
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("79").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8079").build()),

        // Both ns/19 and ns/474 are two English translations of the same song
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("19")
                .build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("474")
                .build()),

        // Both h/267 and h/1360 have the same chorus
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("267").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1360").build()),

        // h/720, h/8526, nt/720, and nt/720b have all different tunes of the same song
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("720").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8526").build(),
            SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("720")
                .build(),
            SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("720b")
                .build()),

        // h/666 is a brother Lee rewrite of h/8661
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("666").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8661").build()),

        // Both h/445 is h/1359 but without the chorus
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("445").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1359").build()),

        // Both h/1353 are h/8476 are alternate versions of each other (probably different translations of the same
        // Chinese song)
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1353").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8476").build()),

        // h/921 is the original and h/1358 is an adapted version
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("921").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1358").build()),

        // h/18 is the original and ns/7 is an adapted version
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("18").build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("7")
                .build()),

        // c/21 is a shortened version of h/70
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue)
                .setHymnNumber("21").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("70").build()),

        // ns/179 is the adapted version of h/1248
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1248").build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("179")
                .build()),

        // Both ns/154 and h/8330 are valid translations of the Chinese song ch/330.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("154")
                .build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("8330").build()),

        // Both h/254 and h/8211 are valid translations of the Chinese song ch/211.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("254")
                         .build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                         .setHymnNumber("8211").build()),

        // Both h/984 and h/8204 are valid translations of the Chinese song ch/204.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("984")
                         .build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                         .setHymnNumber("8204").build()),

        // ns/547 and ns/945 don't have the same tune, but are very similar and based on the same
        // scripture passage.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("547")
                .build(),
            SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("945")
                .build())
    );
  }
}
