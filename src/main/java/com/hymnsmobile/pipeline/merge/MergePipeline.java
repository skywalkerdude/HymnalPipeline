package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.hymnsmobile.pipeline.merge.Utilities.getHymnFrom;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchHymn;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.merge.patchers.H4aPatcher;
import com.hymnsmobile.pipeline.merge.patchers.HymnalNetPatcher;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.russian.RussianHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
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
public class MergePipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Converter converter;
  private final H4aMerger h4aMerger;
  private final H4aPatcher h4aPatcher;
  private final HymnalNetPatcher hymnalNetPatcher;
  private final SanitizationPipeline sanitizationPipeline;
  private final Set<PipelineError> errors;

  @Inject
  public MergePipeline(
      Converter converter,
      H4aMerger h4aMerger,
      H4aPatcher h4aPatcher,
      HymnalNetPatcher hymnalNetPatcher,
      SanitizationPipeline sanitizationPipeline,
      @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
    this.h4aMerger = h4aMerger;
    this.h4aPatcher = h4aPatcher;
    this.hymnalNetPatcher = hymnalNetPatcher;
    this.sanitizationPipeline = sanitizationPipeline;
  }

  /**
   * Initially, just convert all Hymnal.net songs into the common format
   */
  public ImmutableList<Hymn> convertHymnalNet(ImmutableList<HymnalNetJson> hymnalNetHymns) {
    LOGGER.info("Converting Hymnal.net");
    ImmutableList<Hymn> hymns = hymnalNetHymns.stream().map(converter::toHymn).collect(toImmutableList());
    LOGGER.info("Sanitizing Hymnal.net");
    return sanitizationPipeline.sanitize(hymns, hymnalNetPatcher);
  }

  public ImmutableList<Hymn> mergeRussian(ImmutableList<RussianHymn> russianHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Russian");
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    russianHymns.forEach(russianHymn -> {
      builders.add(converter.toHymn(russianHymn).toBuilder());

      // Set the parent to also reference the Russian hymn.
      SongReference parentReference = russianHymn.getParent().getReference();
      Hymn.Builder parent = getHymnFrom(parentReference, builders).orElseThrow();
      parent.addLanguages(
          SongLink.newBuilder()
              .setName("Russian")
              .setReference(SongReference.newBuilder()
                  .setHymnType(HymnType.RUSSIAN.abbreviatedValue)
                  .setHymnNumber(String.valueOf(russianHymn.getNumber())))
              .build());
    });
    LOGGER.info("Sanitizing Russian");
    return sanitizationPipeline.sanitize(
        builders.stream().map(Hymn.Builder::build).collect(toImmutableList()));
  }

  public ImmutableList<Hymn> mergeH4a(
      ImmutableList<H4aHymn> h4aHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Hymns for Android");
    ImmutableList<Hymn> merged = h4aMerger.merge(h4aHymns, mergedHymns);
    LOGGER.info("Sanitizing Hymns for Android");
    return sanitizationPipeline.sanitize(merged, h4aPatcher);
  }

  public ImmutableList<Hymn> mergeLiederbuch(
      ImmutableList<LiederbuchHymn> liederbuchHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Liederbuch");

    ImmutableSet<SongReference> existingReferences =
        mergedHymns.stream()
            .flatMap(mergedHymn -> mergedHymn.getReferencesList().stream())
            .collect(toImmutableSet());

    liederbuchHymns.forEach(liederbuchHymn -> {
      SongReference liederbuchReference = converter.toSongReference(liederbuchHymn.getKey());
      if (!existingReferences.contains(liederbuchReference)) {
        errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.WARNING)
                .setMessage("Liederbuch songs should be covered by other sources already.")
                .build());
      }
    });
    LOGGER.info("Sanitizing Liederbuch");
    return sanitizationPipeline.sanitize(mergedHymns);
  }

  public ImmutableList<Hymn> mergeSongbase(
      ImmutableList<SongbaseHymn> songbaseHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Songbase");
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    songbaseHymns.forEach(songbaseHymn -> {
      Hymn.Builder songbaseBuilder = converter.toHymn(songbaseHymn).toBuilder().addProvenance("songbase");

      // Find a hymn that already matches one of the songbase song's references, if it exists
      ImmutableList<Hymn.Builder> matchingReference =
          songbaseBuilder.getReferencesList().stream()
              .map(reference -> getHymnFrom(reference, builders))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(toImmutableList());
      if (matchingReference.isEmpty()) {
        // No matching references, so add the songbase song
        builders.add(songbaseBuilder);
        return;
      }

      assert matchingReference.size() == 1;
      // Add the new references
      songbaseBuilder.getReferencesList().stream()
          .filter(reference -> !matchingReference.get(0).getReferencesList().contains(reference))
          .forEach(reference -> matchingReference.get(0).addReferences(reference));
      // Set inline chords property
      matchingReference.get(0).setInlineChords(songbaseBuilder.getInlineChords());
    });
    LOGGER.info("Sanitizing Songbase");
    return sanitizationPipeline.sanitize(
        builders.stream().map(Hymn.Builder::build).collect(toImmutableList()));
  }

  @SafeVarargs
  public final ImmutableList<PipelineError> mergeErrors(
      ImmutableList<PipelineError>... errorLists) {
    ImmutableList.Builder<PipelineError> allErrors = ImmutableList.builder();
    for (ImmutableList<PipelineError> errorList : errorLists) {
      allErrors.addAll(errorList);
    }
    return allErrors.build();
  }

  public final ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }
}
