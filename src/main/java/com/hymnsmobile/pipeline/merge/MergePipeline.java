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
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.russian.RussianHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import java.util.List;
import java.util.Set;
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
  private final SongbaseMerger songbaseMerger;

  @Inject
  public MergePipeline(
      Converter converter,
      H4aMerger h4aMerger,
      H4aPatcher h4aPatcher,
      HymnalNetPatcher hymnalNetPatcher,
      SanitizationPipeline sanitizationPipeline,
      SongbaseMerger songbaseMerger,
      @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
    this.h4aMerger = h4aMerger;
    this.h4aPatcher = h4aPatcher;
    this.hymnalNetPatcher = hymnalNetPatcher;
    this.sanitizationPipeline = sanitizationPipeline;
    this.songbaseMerger = songbaseMerger;
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
      SongReference parentReference = russianHymn.getParent();
      Hymn.Builder parent = getHymnFrom(parentReference, builders).orElseThrow();
      parent.addLanguages(SongReference.newBuilder().setHymnType(HymnType.RUSSIAN.abbreviatedValue)
          .setHymnNumber(String.valueOf(russianHymn.getNumber())).build());
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
        errors.add(PipelineError.newBuilder()
            .setSeverity(Severity.WARNING)
            .setErrorType(ErrorType.LIEDERBUCH_ALREADY_COVERED)
            .addMessages(liederbuchReference.toString())
            .build());
      }
    });
    LOGGER.info("Sanitizing Liederbuch");
    return sanitizationPipeline.sanitize(mergedHymns);
  }

  public ImmutableList<Hymn> mergeSongbase(
      ImmutableList<SongbaseHymn> songbaseHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Songbase");
    ImmutableList<Hymn> merged = songbaseMerger.merge(songbaseHymns, mergedHymns);
    LOGGER.info("Sanitizing Songbase");
    return sanitizationPipeline.sanitize(merged, h4aPatcher);
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
