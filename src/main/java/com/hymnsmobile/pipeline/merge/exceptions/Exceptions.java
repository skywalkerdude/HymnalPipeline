package com.hymnsmobile.pipeline.merge.exceptions;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.SongReference;

public abstract class Exceptions {

  public abstract ImmutableSet<ImmutableSet<SongReference>> languageExceptions();

  public abstract ImmutableSet<ImmutableSet<SongReference>> relevantExceptions();
}
