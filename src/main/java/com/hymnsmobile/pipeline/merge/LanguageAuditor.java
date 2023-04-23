package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.Exceptions.LANGUAGES_EXCEPTIONS;
import static com.hymnsmobile.pipeline.models.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.HOWARD_HIGASHI;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * QAs the songs to ensure that songs are correct and there's no glaring errors/mistakes.
 */
@MergeScope
public class LanguageAuditor {

  private final Set<PipelineError> errors;

  @Inject
  public LanguageAuditor(@Merge Set<PipelineError> errors) {
    this.errors = errors;
  }

  public void audit(Set<Set<SongLink>> languageSets) {
    languageSets.forEach(
        languageSet -> auditLanguageSet(
            languageSet.stream().map(SongLink::getReference).collect(Collectors.toSet())));
  }

  private void auditLanguageSet(Set<SongReference> setToAudit) {
    if (setToAudit.size() == 1) {
      throw new IllegalStateException(
          "Dangling language set. Should have been taken care of by fixer");
    }

    // Extract the hymn types for audit.
    ImmutableList<com.hymnsmobile.pipeline.models.HymnType> hymnTypes =
        setToAudit.stream().map(SongReference::getHymnType).collect(toImmutableList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the languages list.
    for (com.hymnsmobile.pipeline.models.HymnType hymnType : com.hymnsmobile.pipeline.models.HymnType.values()) {
      // For each song like ns/151de, lb/12s,  or , increment the allowance of that type of hymn, since those are valid
      // alternates.
      int timesAllowed = 1;
      if (ImmutableSet.of(NEW_SONG, HOWARD_HIGASHI).contains(hymnType)) {
        for (SongReference songReference : setToAudit) {
          if (songReference.getHymnType() == hymnType && songReference.getHymnNumber()
              .matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      // If the current set includes an exception group, then remove that exception group from the list and audit
      // again.
      for (Set<SongReference> exception : LANGUAGES_EXCEPTIONS) {
        if (setToAudit.containsAll(exception)) {
          if (!setToAudit.removeAll(exception)) {
            throw new IllegalArgumentException(
                exception + " was unable to be removed from " + setToAudit);
          }
          auditLanguageSet(setToAudit);
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

    // Verify that incompatible hymn types don't appear together the languages list.
    if ((hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(NEW_SONG))
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(CHILDREN_SONG))
        || hymnTypes.contains(CHILDREN_SONG) && hymnTypes.contains(NEW_SONG)
        || hymnTypes.contains(CHINESE) && hymnTypes.contains(CHINESE_SUPPLEMENTAL)
        || hymnTypes.contains(CHINESE_SIMPLIFIED) && hymnTypes.contains(
        CHINESE_SUPPLEMENTAL_SIMPLIFIED)) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR).
              setMessage(String.format("Incompatible languages types: %s", setToAudit))
              .build());
    }
  }
}
