package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public abstract class Auditor {

  protected final Set<PipelineError> errors;

  /**
   * All exceptions we've seen so far. We need to keep track of this because the auditing step is
   * cumulative, meaning that at each step, we are auditing not only the newly added songs, but we
   * are actually auditing the entire database, meaning that all the previous exceptions will need
   * to re-apply. Therefore, we need to keep a running tally of all the exceptions we've seen, since
   * they need to be re-applied each time.
   */
  private final Set<ImmutableSet<SongReference>> allExceptionsSoFar;

  /**
   * The set of exceptions used for the current audit.
   */
  @Nullable
  private Set<ImmutableSet<SongReference>> currentAuditExceptions;

  public Auditor(Set<PipelineError> errors) {
    this.errors = errors;
    this.allExceptionsSoFar = new HashSet<>();
  }

  public void audit(Set<Set<SongReference>> songReferenceSets,
      Optional<ImmutableSet<ImmutableSet<SongReference>>> exceptions) {
    exceptions.ifPresent(this.allExceptionsSoFar::addAll);

    currentAuditExceptions = new HashSet<>(allExceptionsSoFar);
    this.performAudit(songReferenceSets);
    if (!currentAuditExceptions.isEmpty()) {
      this.errors.add(PipelineError.newBuilder()
          .setSource(PipelineError.Source.MERGE)
          .setSeverity(Severity.WARNING)
          .setErrorType(ErrorType.AUDITOR_OBSOLETE_EXCEPTION)
          .addMessages(currentAuditExceptions.toString())
          .build());
    }
    currentAuditExceptions = null;
  }

  protected abstract void performAudit(Set<Set<SongReference>> songReferenceSets);

  /**
   * If the set includes a pre-defined set of exceptions, then remove it from the set. Returns true
   * if items were removed from the set.
   */
  protected boolean removeExceptions(Set<SongReference> setToAudit) {
    if (currentAuditExceptions == null) {
      return false;
    }
    for (ImmutableSet<SongReference> exception : currentAuditExceptions) {
      if (setToAudit.containsAll(exception)) {
        if (!setToAudit.removeAll(exception)) {
          throw new IllegalArgumentException(
              exception + " was unable to be removed from " + setToAudit);
        }
        // Remove exception from exceptions set because we have "used" the exception.
        currentAuditExceptions.remove(exception);
        return true;
      }
    }
    return false;
  }
}
