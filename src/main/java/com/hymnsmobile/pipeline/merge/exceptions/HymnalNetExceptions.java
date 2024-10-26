package com.hymnsmobile.pipeline.merge.exceptions;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.SongReference;

import javax.inject.Inject;

import static com.hymnsmobile.pipeline.merge.HymnType.*;

@MergeScope
public class HymnalNetExceptions extends Exceptions {

  @Inject
  public HymnalNetExceptions() {}

  public final ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
    return ImmutableSet.of(
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

        // h/8622 is a retranslation of ch/622. There is an original English version of h/855 already. Typically, this
        // retranslation would be taken care of by SanitizationPipeline#separateRetranslations. However, h/8622 actually
        // has its own Portuguese version (pt/1372) in addition to the original Portuguese song (pt/855).
        //
        // Since this is such a special one-off case, we make an exception and allow both translations to coexist in
        // the same language group.
        // an exception to reduce confusion.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(PORTUGUESE.abbreviatedValue)
                .setHymnNumber("855").build(),
            SongReference.newBuilder().setHymnType(PORTUGUESE.abbreviatedValue)
                .setHymnNumber("1372").build())
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

        // Both h/445 is h/1359 but without the chorus
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("445").build(),
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                .setHymnNumber("1359").build()),

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
