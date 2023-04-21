package com.hymnsmobile.pipeline.sanitization;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_TUNE;
import static com.hymnsmobile.pipeline.sanitization.Exceptions.RELEVANT_EXCEPTIONS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.dagger.Sanitization;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationScope;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * QAs the songs to ensure that songs are correct and there's no glaring errors/mistakes.
 */
@SanitizationScope
public class RelevantsAuditor {

  private final Set<PipelineError> errors;

  @Inject
  public RelevantsAuditor(@Sanitization Set<PipelineError> errors) {
    this.errors = errors;
  }

  public void audit(Set<Set<SongLink>> relevantsSets) {
    relevantsSets.forEach(
        relevantsSet -> auditRelevantsSet(
            relevantsSet.stream().map(SongLink::getReference).collect(Collectors.toSet())));
  }

  private void auditRelevantsSet(Set<SongReference> setToAudit) {
    if (setToAudit.size() == 1) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR)
              .setMessage(String.format("Dangling relevant set: %s", setToAudit))
              .build());
    }

    // Extract the hymn types for audit.
    ImmutableList<HymnType> hymnTypes =
        setToAudit.stream().map(SongReference::getHymnType).collect(toImmutableList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the relevant list.
    for (HymnType hymnType : HymnType.values()) {
      int timesAllowed = 1;

      // For each song like h/810, ns/698b, nt/394b, de/786b, ch/nt575c, chx/nt575c increment the
      // allowance of that type of hymn, since those are valid alternates.
      if (ImmutableSet.of(CLASSIC_HYMN, NEW_TUNE, NEW_SONG, GERMAN, CHINESE, CHINESE_SIMPLIFIED)
          .contains(hymnType)) {
        for (SongReference songReference : setToAudit) {
          if (songReference.getHymnType() == hymnType && songReference.getHymnNumber()
              .matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      // If the current set includes an exception group, then remove that exception group from the list and audit
      // again.
      for (Set<SongReference> exception : RELEVANT_EXCEPTIONS) {
        if (setToAudit.containsAll(exception)) {
          if (!setToAudit.removeAll(exception)) {
            throw new IllegalArgumentException(exception + " was unable to be removed from " + setToAudit);
          }
          auditRelevantsSet(setToAudit);
          return;
        }
      }

      if (Collections.frequency(hymnTypes, hymnType) > timesAllowed) {
        errors.add(
            PipelineError
                .newBuilder()
                .setSeverity(Severity.ERROR).
                setMessage(String.format("%s has too many instances of %s", setToAudit, hymnType))
                .build());
      }
    }

    // Verify that incompatible hymn types don't appear together the relevant list.
    if ((hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(NEW_SONG))
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(HymnType.CHILDREN_SONG))
        || hymnTypes.contains(HymnType.CHILDREN_SONG) && hymnTypes.contains(NEW_SONG)
        || hymnTypes.contains(CHINESE) && hymnTypes.contains(HymnType.CHINESE_SUPPLEMENTAL)) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR).
              setMessage(String.format("Incompatible relevant types: %s", setToAudit))
              .build());
    }
  }
}
