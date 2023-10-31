package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.merge.patchers.Patcher;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Pipeline that looks for duplicate hymns and merges them into a single hymn with multiple
 * references.
 */
@MergeScope
public class SanitizationPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final LanguageAuditor languageAuditor;
  private final RelevantsAuditor relevantsAuditor;
  private final Set<PipelineError> errors;

  @Inject
  public SanitizationPipeline(
      LanguageAuditor languageAuditor,
      RelevantsAuditor relevantsAuditor,
      @Merge Set<PipelineError> errors) {
    this.errors = errors;
    this.languageAuditor = languageAuditor;
    this.relevantsAuditor = relevantsAuditor;
  }

  public ImmutableList<Hymn> sanitize(ImmutableList<Hymn> allHymns) {
    return sanitize(allHymns, Optional.empty());
  }

  public ImmutableList<Hymn> sanitize(ImmutableList<Hymn> allHymns, Patcher patcher) {
    return sanitize(allHymns, Optional.of(patcher));
  }

  private ImmutableList<Hymn> sanitize(ImmutableList<Hymn> allHymns, Optional<Patcher> patcher) {
    if (patcher.isPresent()) {
      allHymns = patcher.get().patch(allHymns);
    }

    ImmutableList<Hymn.Builder> builders =
        allHymns.stream().map(Hymn::toBuilder).collect(toImmutableList());

    fixLanguages(builders);
    fixRelevants(builders);

    return builders.stream().map(Hymn.Builder::build).collect(toImmutableList());
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  private void fixLanguages(ImmutableList<Hymn.Builder> builders) {
    FieldDescriptor languageFieldDescriptor = Hymn.getDescriptor().findFieldByName("languages");
    Set<Set<SongReference>> languageSets =
        generateSongLinkSets(builders, languageFieldDescriptor);
    languageAuditor.audit(languageSets);
    writeSongLinks(builders, languageFieldDescriptor, languageSets);
  }

  private void fixRelevants(ImmutableList<Hymn.Builder> builders) {
    FieldDescriptor relevantFieldDescriptor = Hymn.getDescriptor().findFieldByName("relevants");
    Set<Set<SongReference>> relevantsSets =
        generateSongLinkSets(builders, relevantFieldDescriptor);
    relevantsAuditor.audit(relevantsSets);
    writeSongLinks(builders, relevantFieldDescriptor, relevantsSets);
  }

  /**
   * Generates aggregated sets of {@link SongLink}s that represent all links of a single song,
   * described by the {@link FieldDescriptor}.
   */
  private Set<Set<SongReference>> generateSongLinkSets(
      ImmutableList<Hymn.Builder> builders, FieldDescriptor descriptor) {
    Set<Set<SongReference>> songLinkSets = new HashSet<>();
    builders.forEach(builder -> {
      Hymn hymn = builder.build();
      // noinspection unchecked
      List<SongReference> links = (List<SongReference>) hymn.getField(descriptor);

      if (links.isEmpty()) {
        return;
      }

      if (links.stream().anyMatch(link -> hymn.getReferencesList().contains(link))) {
        errors.add(PipelineError.newBuilder()
            .setSeverity(Severity.ERROR)
            .setErrorType(ErrorType.AUDITOR_SELF_REFERENCE)
            .addMessages(hymn.getReferencesList().toString())
            .build());
      }

      final Set<SongReference> songLinkSet = new LinkedHashSet<>();
      if (hymn.getReferencesCount() == 0) {
        throw new IllegalStateException("hymn references were empty");
      }
      populateSongLinkSet(builders, descriptor, hymn.getReferences(0), songLinkSet);

      // Once we have the song link set for this hymn populated correctly, we attempt to merge it
      // with an existing set, if it exists, that already contains the current songs.
      List<Set<SongReference>> setToMergeWith = songLinkSets.stream().filter(
              songLinks -> songLinkSet.stream().anyMatch(songLinks::contains))
          .collect(Collectors.toList());
      if (setToMergeWith.size() > 1) {
        throw new IllegalStateException(
            "Set too big. This shouldn't happen, as it indicates a code error.");
      }
      if (setToMergeWith.isEmpty()) {
        songLinkSets.add(songLinkSet);
      } else {
        setToMergeWith.get(0).addAll(songLinkSet);
      }
    });
    return songLinkSets;
  }

  private void populateSongLinkSet(ImmutableList<Hymn.Builder> builders,
      FieldDescriptor descriptor, SongReference songLink, Set<SongReference> songLinks) {
    if (songLinks.contains(songLink)) {
      return;
    }
    songLinks.add(songLink);
    // noinspection unchecked
    ((List<SongReference>) getReferencedHymnBuilder(builders, songLink)
        .getField(descriptor))
        .forEach(linkedSong ->
            populateSongLinkSet(builders, descriptor, linkedSong, songLinks));
  }

  private Hymn.Builder getReferencedHymnBuilder(
      ImmutableList<Hymn.Builder> builders,
      SongReference songReference) {
    ImmutableList<Hymn.Builder> results = builders.stream()
        .filter(builder -> builder.getReferencesList().contains(songReference))
        .collect(toImmutableList());
    if (results.size() != 1) {
      throw new IllegalStateException("results was not of size 1");
    }
    return results.get(0);
  }

  /**
   * Write the newly aggregated {@link SongLink} sets onto each hymn.
   */
  private void writeSongLinks(
      ImmutableList<Hymn.Builder> builders,
      FieldDescriptor descriptor, Set<Set<SongReference>> songLinkSets) {
    builders.forEach(builder -> {
      ImmutableList<SongReference> references = ImmutableList.copyOf(builder.getReferencesList());
      ImmutableList<Set<SongReference>> setContainingHymn =
          songLinkSets.stream()
              .filter(songLinks -> songLinks.stream().anyMatch(references::contains))
              .collect(toImmutableList());

      if (setContainingHymn.isEmpty()) {
        return;
      }

      if (setContainingHymn.size() != 1) {
        throw new IllegalStateException("Set containing hymn was not size 1");
      }

      // Make a copy of the list, so we aren't destructively altering it within a loop
      List<SongReference> newLinks = new ArrayList<>(setContainingHymn.get(0));
      // Remove self from set
      if (!newLinks.removeIf(references::contains)) {
        throw new IllegalStateException(references + " not found");
      }
      builder.clearField(descriptor).setField(descriptor, newLinks);
    });
  }
}
