package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.HymnType.*;

/**
 * QAs the songs to ensure that songs are correct and there's no glaring errors/mistakes.
 */
@MergeScope
public class LanguageAuditor extends Auditor {

  @Inject
  public LanguageAuditor(@Merge Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performAudit(Set<Set<SongReference>> songReferenceSets) {
    songReferenceSets.forEach(songReferences -> auditLanguageSet(new LinkedHashSet<>(songReferences), false));
  }

  private void auditLanguageSet(Set<SongReference> setToAudit, boolean ignoreDanglingReference) {
    if (setToAudit.size() == 1 && !ignoreDanglingReference) {
      errors.add(PipelineError.newBuilder()
          .setSeverity(Severity.ERROR)
          .setErrorType(ErrorType.AUDITOR_DANGLING_LANGUAGE_SET)
          .addMessages(setToAudit.toString())
          .build());
      return;
    }

    // Extract the hymn types for audit.
    ImmutableList<HymnType> hymnTypes =
        setToAudit.stream()
            .map(SongReference::getHymnType)
            .map(HymnType::fromString)
            .collect(toImmutableList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the languages list.
    for (HymnType hymnType : HymnType.values()) {
      // For each song like ns/151de, lb/12s,  or , increment the allowance of that type of hymn, since those are valid
      // alternates.
      int timesAllowed = 1;
      if (ImmutableSet.of(NEW_SONG, HOWARD_HIGASHI).contains(hymnType)) {
        for (SongReference songReference : setToAudit) {
          if (HymnType.fromString(songReference.getHymnType()) == hymnType && songReference.getHymnNumber()
              .matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      if (Collections.frequency(hymnTypes, hymnType) > timesAllowed) {
        // If exceptions were removed, then we audit the new set and return early (i.e. don't keep
        // looking at the rest of the hymn types because that list is no longer accurate)
        if (removeExceptions(setToAudit)) {
          // May cause a dangling reference set if we remove everything except for one song, so we need to special case
          // to ignore that error, if it happens.
          auditLanguageSet(setToAudit, true);
          return;
        }
        errors.add(PipelineError.newBuilder()
            .setSeverity(Severity.ERROR)
            .setErrorType(ErrorType.AUDITOR_TOO_MANY_INSTANCES)
            .addMessages(setToAudit.toString())
            .addMessages(hymnType.toString())
            .build());
      }
    }

    // Verify that incompatible hymn types don't appear together the languages list.
    if (((hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(NEW_SONG))
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(CHILDREN_SONG))
        || hymnTypes.contains(CHILDREN_SONG) && hymnTypes.contains(NEW_SONG)
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(BE_FILLED))
        || (hymnTypes.contains(NEW_SONG) && hymnTypes.contains(BE_FILLED))
        || (hymnTypes.contains(CHILDREN_SONG) && hymnTypes.contains(BE_FILLED))
        || (hymnTypes.contains(HOWARD_HIGASHI) && hymnTypes.contains(BE_FILLED))
        || hymnTypes.contains(CHINESE) && hymnTypes.contains(CHINESE_SUPPLEMENTAL)
        || hymnTypes.contains(CHINESE_SIMPLIFIED) && hymnTypes.contains(CHINESE_SUPPLEMENTAL_SIMPLIFIED))
        && !removeExceptions(setToAudit)) {
      errors.add(PipelineError.newBuilder()
          .setSeverity(Severity.ERROR)
          .setErrorType(ErrorType.AUDITOR_INCOMPATIBLE_LANGUAGES)
          .addMessages(setToAudit.toString())
          .build());
    }
  }
}
