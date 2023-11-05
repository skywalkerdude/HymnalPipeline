package com.hymnsmobile.pipeline.merge.exceptions;

import static com.hymnsmobile.pipeline.merge.HymnType.BE_FILLED;
import static com.hymnsmobile.pipeline.merge.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.merge.HymnType.TAGALOG;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.SongReference;
import javax.inject.Inject;

@MergeScope
public class H4aExceptions extends Exceptions {

  @Inject
  public H4aExceptions() {}

  public final ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
    return ImmutableSet.of(
        // ht/437 is from H4A, and seems like also a valid translation of h/437 as well as ht/c333
        // from hymnal.net.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("c333")
                .build(),
            SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("437")
                .build()),
        // ch/276 is translated in Hymnal.net by h/8276. bf/231 is an alternate translation that is
        // very similar to h/8276, but slightly different. So here, we allow both to exist as an
        // exception.
        ImmutableSet.of(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8276")
                .build(),
            SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("231")
                .build())
    );
  }

  public final ImmutableSet<ImmutableSet<SongReference>> relevantExceptions() {
    return ImmutableSet.of();
  }
}
