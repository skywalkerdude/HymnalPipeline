package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.AbstractMessage;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.HymnType.*;

/**
 * QAs the songs to ensure that songs are correct and there's no glaring errors/mistakes.
 */
@MergeScope
public class RelevantsAuditor extends Auditor {

  @Inject
  public RelevantsAuditor(@Merge Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performAudit(Set<Set<SongReference>> songReferenceSets) {
    songReferenceSets.forEach(songReferences -> auditRelevantsSet(new LinkedHashSet<>(songReferences), false));
  }

  private void auditRelevantsSet(Set<SongReference> setToAudit, boolean ignoreDanglingReference) {
    if (setToAudit.size() == 1 && !ignoreDanglingReference) {
      errors.add(PipelineError.newBuilder()
          .setSource(PipelineError.Source.MERGE)
          .setSeverity(Severity.ERROR)
          .setErrorType(ErrorType.AUDITOR_DANGLING_RELEVANT_SET)
          .addMessages(setToAudit.toString())
          .build());
    }

    // Extract the hymn types for audit.
    ImmutableList<HymnType> hymnTypes =
        setToAudit.stream()
            .map(SongReference::getHymnType)
            .map(HymnType::fromString)
            .collect(toImmutableList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the relevant list.
    for (HymnType hymnType : HymnType.values()) {
      int timesAllowed = 1;

      // For each song like h/810, ns/698b, nt/394b, de/786b, ch/nt575c, chx/nt575c increment the
      // allowance of that type of hymn, since those are valid alternates.
      if (ImmutableSet.of(CLASSIC_HYMN, NEW_TUNE, NEW_SONG, GERMAN, CHINESE, CHINESE_SIMPLIFIED)
          .contains(hymnType)) {
        for (SongReference songReference : setToAudit) {
          if (HymnType.fromString(songReference.getHymnType()) == hymnType && songReference.getHymnNumber()
              .matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      if (Collections.frequency(hymnTypes, hymnType) > timesAllowed) {
        // If exceptions were removed, then we audit the new set and return early (i.e. don't keep
        // looking at hte rest of the hymn types because that list is no longer accurate)
        if (removeExceptions(setToAudit)) {
          // May cause a dangling reference set if we remove everything except for one song, so we need to special case
          // to ignore that error, if it happens.
          auditRelevantsSet(setToAudit, true);
          return;
        }
        errors.add(PipelineError.newBuilder()
            .setSource(PipelineError.Source.MERGE)
            .setSeverity(Severity.ERROR)
            .setErrorType(ErrorType.AUDITOR_TOO_MANY_INSTANCES)
            .addMessages(hymnType.toString())
            .addAllMessages(setToAudit.stream().map(AbstractMessage::toString).collect(Collectors.toSet()))
            .build());
      }
    }

    // Verify that incompatible hymn types don't appear together the relevant list.
    if (((hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(NEW_SONG))
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(HymnType.CHILDREN_SONG))
        || hymnTypes.contains(HymnType.CHILDREN_SONG) && hymnTypes.contains(NEW_SONG)
        || hymnTypes.contains(CHINESE) && hymnTypes.contains(HymnType.CHINESE_SUPPLEMENTAL))
        && !removeExceptions(setToAudit)) {
      errors.add(PipelineError.newBuilder()
          .setSource(PipelineError.Source.MERGE)
          .setSeverity(Severity.ERROR)
          .setErrorType(ErrorType.AUDITOR_INCOMPATIBLE_RELEVANTS)
          .addMessages(setToAudit.toString())
          .build());
    }
  }
}
