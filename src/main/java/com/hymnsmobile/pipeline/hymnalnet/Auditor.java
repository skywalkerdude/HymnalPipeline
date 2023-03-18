package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.hymnalnet.Exceptions.HYMNAL_DB_LANGUAGES_EXCEPTIONS;
import static com.hymnsmobile.pipeline.hymnalnet.Exceptions.HYMNAL_DB_RELEVANT_EXCEPTIONS;
import static com.hymnsmobile.pipeline.models.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.models.HymnType.HOWARD_HIGASHI;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_TUNE;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * QAs the songs to ensure that songs are correct and there's no glaring errors/mistakes.
 */
@HymnalNetPipelineScope
public class Auditor {

  private final Set<PipelineError> errors;
  private final Set<Hymn> hymns;

  private Map<SongReference, Hymn> referenceHymnMap;

  @Inject
  public Auditor(Set<PipelineError> errors, @HymnalNet Set<Hymn> hymns) {
    this.errors = errors;
    this.hymns = hymns;
  }

  public void auditHymns() {
    referenceHymnMap = hymns.stream().collect(Collectors.toMap(Hymn::getReference, hymn -> hymn));

    Set<Set<SongLink>> languageSets = new HashSet<>();
    Set<Set<SongLink>> relevantSets = new HashSet<>();
    hymns.forEach(hymn -> {
      languageSets.add(populateLanguageSets(hymn));
      relevantSets.add(populateRelevantSets(hymn));
    });
    languageSets.forEach(
        languageSet -> auditLanguageSet(
            languageSet.stream().map(SongLink::getReference).collect(Collectors.toSet())));
    relevantSets.forEach(
        relevantSet -> auditRelevantSet(
            relevantSet.stream().map(SongLink::getReference).collect(Collectors.toSet())));
  }

  private Set<SongLink> populateLanguageSets(Hymn hymn) {
    Set<SongLink> allLanguages = new HashSet<>();
    hymn.getLanguagesList().forEach(language -> populateLanguageSets(language, allLanguages));
    return allLanguages;
  }

  private void populateLanguageSets(SongLink songLink, Set<SongLink> allLanguages) {
    if (allLanguages.contains(songLink)) {
      return;
    }

    allLanguages.add(songLink);
    referenceHymnMap.get(songLink.getReference()).getLanguagesList().forEach(
        language -> populateLanguageSets(language, allLanguages));
  }

  private void auditLanguageSet(Set<SongReference> setToAudit) {
    if (setToAudit.size() == 1) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR)
              .setMessage(String.format("Dangling set: %s", setToAudit))
              .build());
    }

    // Extract the hymn types for audit.
    ImmutableList<com.hymnsmobile.pipeline.models.HymnType> hymnTypes =
        setToAudit.stream().map(SongReference::getType).collect(toImmutableList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the languages list.
    for (com.hymnsmobile.pipeline.models.HymnType hymnType : com.hymnsmobile.pipeline.models.HymnType.values()) {
      // For each song like h225b or ns92f, increment the allowance of that type of hymn, since those are valid
      // alternates.
      int timesAllowed = 1;
      if ((hymnType == CLASSIC_HYMN || hymnType == NEW_SONG || hymnType == HOWARD_HIGASHI)) {
        for (SongReference songReference : setToAudit) {
          if (songReference.getType() == hymnType && songReference.getNumber().matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      // If the current set includes an exception group, then remove that exception group from the list and audit
      // again.
      for (Set<SongReference> exception : HYMNAL_DB_LANGUAGES_EXCEPTIONS) {
        if (setToAudit.containsAll(exception)) {
          if (!setToAudit.removeAll(exception)) {
            throw new IllegalArgumentException(exception + " was unable to be removed from " + setToAudit);
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
        || hymnTypes.contains(CHINESE_SIMPLIFIED) && hymnTypes.contains(CHINESE_SUPPLEMENTAL_SIMPLIFIED)) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR).
              setMessage(String.format("%s has incompatible languages types", setToAudit))
              .build());
    }
  }

  private Set<SongLink> populateRelevantSets(Hymn hymn) {
    Set<SongLink> allRelevants = new HashSet<>();
    hymn.getRelevantsList().forEach(relevant -> populateRelevantSets(relevant, allRelevants));
    return allRelevants;
  }

  private void populateRelevantSets(SongLink songLink, Set<SongLink> allRelevants) {
    if (allRelevants.contains(songLink)) {
      return;
    }
    allRelevants.add(songLink);
    referenceHymnMap.get(songLink.getReference()).getRelevantsList().forEach(
        relevant -> populateLanguageSets(relevant, allRelevants));
  }

  private void auditRelevantSet(Set<SongReference> setToAudit) {
    if (setToAudit.size() == 1) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR).
              setMessage(String.format(
                  "Relevant set with only 1 key is a dangling reference, which needs fixing: %s",
                  setToAudit))
              .build());
    }

    // Extract the hymn types for audit.
    List<com.hymnsmobile.pipeline.models.HymnType> hymnTypes = setToAudit.stream()
        .map(SongReference::getType).collect(Collectors.toList());

    // Verify that the same hymn type doesn't appear more than the allowed number of times the relevant list.
    for (com.hymnsmobile.pipeline.models.HymnType hymnType : com.hymnsmobile.pipeline.models.HymnType.values()) {
      int timesAllowed = 1;

      // For each song like h/810, ns/698b, nt/394b, de/786b increment the allowance of that type of hymn,
      // since those are valid alternates.
      if ((hymnType == CLASSIC_HYMN || hymnType == NEW_TUNE || hymnType == NEW_SONG || hymnType == GERMAN)) {
        for (SongReference reference : setToAudit) {
          if (reference.getType().equals(hymnType) && reference.getNumber().matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
            timesAllowed++;
          }
        }
      }

      // If the current set includes an exception group, then remove that exception group from the list and audit
      // again.
      for (Set<SongReference> exception : HYMNAL_DB_RELEVANT_EXCEPTIONS) {
        if (setToAudit.containsAll(exception)) {
          if (!setToAudit.removeAll(exception)) {
            throw new IllegalArgumentException(exception + " was unable to be removed from " + setToAudit);
          }
          auditRelevantSet(setToAudit);
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

    // Verify that incompatible hymn types don't appear together the relevants list.
    if ((hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(NEW_SONG))
        || (hymnTypes.contains(CLASSIC_HYMN) && hymnTypes.contains(CHILDREN_SONG))
        || hymnTypes.contains(CHILDREN_SONG) && hymnTypes.contains(NEW_SONG)
        || hymnTypes.contains(CHINESE) && hymnTypes.contains(CHINESE_SUPPLEMENTAL)) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(Severity.ERROR).
              setMessage(String.format("%s has incompatible relevant types", setToAudit))
              .build());
    }
  }
}
