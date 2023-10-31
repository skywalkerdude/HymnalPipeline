package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.HashSet;
import java.util.Set;

public abstract class Auditor {

  protected final Set<PipelineError> errors;
  protected final Set<ImmutableSet<SongReference>> exceptions;

  public Auditor(Set<PipelineError> errors, ImmutableSet<ImmutableSet<SongReference>> exceptions) {
    this.errors = errors;
    this.exceptions = new HashSet<>(exceptions);
  }

  public void audit(Set<Set<SongReference>> songReferenceSets) {
    this.performAudit(songReferenceSets);
    if (!exceptions.isEmpty()) {
      this.errors.add(PipelineError.newBuilder()
          .setSeverity(Severity.WARNING)
          .setErrorType(ErrorType.AUDITOR_OBSOLETE_RELEVANTS_EXCEPTIONS)
          .addMessages(exceptions.toString())
          .build());
    }
  }

  protected abstract void performAudit(Set<Set<SongReference>> songReferenceSets);

  /**
   * If the set includes a pre-defined set of exceptions, then remove it from the set. Returns
   * if things were removed from the set.
   */
  protected boolean removeExceptions(Set<SongReference> setToAudit) {
    for (ImmutableSet<SongReference> exception : exceptions) {
      if (setToAudit.containsAll(exception)) {
        if (!setToAudit.removeAll(exception)) {
          throw new IllegalArgumentException(
              exception + " was unable to be removed from " + setToAudit);
        }
        // Remove exception from exceptions set because we have "used" the exception.
        exceptions.remove(exception);
        return true;
      }
    }
    return false;
  }
}
