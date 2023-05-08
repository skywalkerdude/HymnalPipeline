package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.HymnType.LIEDERBUCH;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
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
import com.hymnsmobile.pipeline.models.Verse;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
  private final H4aPatcher h4aPatcher;
  private final HymnalNetPatcher hymnalNetPatcher;
  private final SanitizationPipeline sanitizationPipeline;
  private final Set<PipelineError> errors;

  @Inject
  public MergePipeline(
      Converter converter,
      H4aPatcher h4aPatcher,
      HymnalNetPatcher hymnalNetPatcher,
      SanitizationPipeline sanitizationPipeline,
      @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
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

  public ImmutableList<Hymn> mergeH4a(ImmutableList<H4aHymn> h4aHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Hymns for Android");
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    h4aHymns.stream()
        // Sort by hymn type
        .sorted(Comparator.comparingInt(
            o -> com.hymnsmobile.pipeline.h4a.HymnType.fromString(o.getId().getType()).orElseThrow().ordinal()))
        // Merge in H4a hymns
        .forEach(h4aHymn -> mergeH4aHymn(h4aHymn, h4aHymns, builders));
    LOGGER.info("Sanitizing Hymns for Android");
    return sanitizationPipeline.sanitize(
        builders.stream().map(Hymn.Builder::build).collect(toImmutableList()), h4aPatcher);
  }

  public ImmutableList<Hymn> mergeLiederbuch(
      ImmutableList<LiederbuchHymn> liederbuchHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Liederbuch");
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    liederbuchHymns.forEach(liederbuchHymn -> {
      SongReference songReference = converter.toSongReference(liederbuchHymn.getKey());

      // Only interested in the Liederbuch (German) songs, since all the other songs are covered
      // by Hymnal.net or H4a
      if (HymnType.fromString(songReference.getHymnType()) != LIEDERBUCH) {
        return;
      }

      ImmutableList<SongReference> relatedReferences =
          liederbuchHymn.getRelatedList().stream()
              .map(converter::toSongReference)
              .collect(toImmutableList());

      // Try to find the associated German song that has already been processed and add the
      // liederbuch song as an alternate key to that song.
      List<Hymn.Builder> germanSongs =
          relatedReferences.stream()
              .map(relatedReference -> getHymnFrom(relatedReference, builders).orElseThrow())
              .flatMap(relatedBuilder -> relatedBuilder.getLanguagesList().stream())
              .map(SongLink::getReference)
              .filter(relatedReference -> HymnType.fromString(relatedReference.getHymnType()) == HymnType.GERMAN)
              .map(germanReference -> getHymnFrom(germanReference, builders).orElseThrow())
              .distinct()
              // Add a link to the Liederbuch song to the German song
              // todo need to figure out how to handle the references. maybe in relevants?
              // .peek(entry ->
              //     entry.getValue().addLanguages(
              //         SongLink.newBuilder().setName("Liederbuch").setReference(songReference)))
              .collect(Collectors.toList());

      // Apply a manual mapping if it is appropriate
      manualMapping(songReference)
          .flatMap(manualMapping -> getHymnFrom(manualMapping, builders))
          .ifPresent(germanSongs::add);

      if (germanSongs.isEmpty()) {
        this.errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.WARNING)
                .setMessage("Couldn't find mapping for " + songReference)
                .build());
        return;
      }
      if (germanSongs.size() > 1) {
        throw new IllegalStateException("Shouldn't have more than 1 matching German song");
      }
      germanSongs.get(0).addReferences(songReference);
    });
    LOGGER.info("Sanitizing Liederbuch");
    return sanitizationPipeline.sanitize(
        builders.stream().map(Hymn.Builder::build).collect(toImmutableList()));
  }

  public ImmutableList<Hymn> mergeSongbase(
      ImmutableList<SongbaseHymn> songbaseHymns, ImmutableList<Hymn> mergedHymns) {
    LOGGER.info("Merging Songbase");
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    songbaseHymns.forEach(songbaseHymn -> {
      Hymn.Builder songbaseBuilder = converter.toHymn(songbaseHymn).toBuilder();

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

  private void mergeH4aHymn(
      H4aHymn h4aHymn,
      ImmutableList<H4aHymn> h4aHymns,
      List<Hymn.Builder> builders) {
    SongReference h4aReference = converter.toSongReference(h4aHymn.getId());

    // We're only interested in adding new songs, so if the song itself already exists, then we
    // can skip it. Note: At this point, we don't actually need to port over the languages of
    // existing hymns, since they will be added when we encounter them.
    if (getHymnFrom(h4aReference, builders).isPresent()) {
      return;
    }

    switch (HymnType.fromString(h4aReference.getHymnType())) {
      case CLASSIC_HYMN:
      case NEW_SONG:
      case CHILDREN_SONG:
      case HOWARD_HIGASHI:
      case DUTCH:
      case CHINESE:
      case CHINESE_SIMPLIFIED:
      case CHINESE_SUPPLEMENTAL:
      case CHINESE_SUPPLEMENTAL_SIMPLIFIED:
      case CEBUANO:
      case FRENCH:
      case SPANISH:
        this.errors.add(
            PipelineError.newBuilder().setSeverity(Severity.WARNING).setMessage(String.format(
                    "%s have already been added by hymnal.net. Warrants investigation",
                    h4aReference))
                .build());
      case GERMAN:
      case TAGALOG:
        // Even though hymnal.net has Tagalog songs, it isn't contiguous and there are a bunch of
        // holes that are filled by H4a
      case KOREAN:
      case INDONESIAN:
      case JAPANESE:
      case FARSI:
        guessParent(h4aHymn, h4aHymns)
            .map(parent -> converter.toSongReference(parent.getId()))
            .ifPresent(parent ->
                getHymnFrom(parent, builders).orElseThrow()
                    .addLanguages(converter.toLanguageLink(h4aReference)));
        builders.add(converter.toHymn(h4aHymn).orElseThrow().toBuilder());
        break;
      case BE_FILLED:
        if (h4aHymn.hasParentHymn()) {
          H4aKey parentKey = h4aHymn.getParentHymn();
          // Add the BE_FILLED song as another reference, not as a new song since it's more than
          // likely a duplicate in terms of content.
          SongReference parentReference = converter.toSongReference(parentKey);
          getHymnFrom(parentReference, builders).orElseThrow().addReferences(h4aReference);
        } else {
          builders.add(converter.toHymn(h4aHymn).orElseThrow().toBuilder());
        }
    }
  }

  /**
   * Try to get a song's parent explicitly if possible. If that's not possible, try to infer it.
   */
  private Optional<H4aHymn> guessParent(H4aHymn h4aHymn, ImmutableList<H4aHymn> h4aHymns) {
    if (h4aHymn.hasParentHymn()) {
      return getHymnFrom(h4aHymn.getParentHymn(), h4aHymns);
    }

    // parentKey is null, so we try to infer it from the type
    Set<H4aKey> inferredParentKeys = inferParent(h4aHymn.getId());
    if (inferredParentKeys.isEmpty()) {
      // Couldn't infer any parents, so just give up at this point.
      return Optional.empty();
    }

    for (H4aKey inferredParentKey : inferredParentKeys) {
      Optional<H4aHymn> inferredParentHymn = getHymnFrom(inferredParentKey, h4aHymns);
      if (inferredParentHymn.isEmpty()) {
        // If the inferredParentHymn doesn't exist, it most definitely is not the correct parent.
        continue;
      }

      if (h4aHymn.getRelatedList().contains(inferredParentKey) && inferredParentHymn.get()
          .getRelatedList().contains(h4aHymn.getId())) {
        // H4a's inferred parent points back to it as well. Huzzah!
        return inferredParentHymn;
      }

      // Same number of lyrics and the lyric type matches, so we are going to assume that they
      // are the same song
      if (lyricsMatch(h4aHymn, inferredParentHymn.get())) {
        return inferredParentHymn;
      }
    }
    // Didn't find any matching parent
    return Optional.empty();
  }

  /**
   * @return a list of {@link H4aKey}s that could be the parent of the passed-in key.
   */
  private Set<H4aKey> inferParent(H4aKey key) {
    Set<H4aKey> inferredParents = new LinkedHashSet<>();
    switch (com.hymnsmobile.pipeline.h4a.HymnType.fromString(key.getType()).orElseThrow()) {
      case INDONESIAN:
      case JAPANESE:
      case KOREAN:
        inferredParents.add(
            H4aKey.newBuilder().setType(com.hymnsmobile.pipeline.h4a.HymnType.CHINESE.abbreviation)
                .setNumber(key.getNumber()).build());
        break;
      case FRENCH:
        inferredParents.add(
            H4aKey.newBuilder().setType(com.hymnsmobile.pipeline.h4a.HymnType.CLASSIC_HYMN.abbreviation)
                .setNumber(key.getNumber())
                .build());
        break;
      default:
    }
    return inferredParents;
  }

  /**
   * Gets the hymn that matches the passed-in song reference.
   */
  private Optional<Hymn.Builder> getHymnFrom(
      SongReference songReference, List<Hymn.Builder> mergedHymns) {
    ImmutableList<Hymn.Builder> connections =
        mergedHymns.stream()
            .filter(hymn -> hymn.getReferencesList().contains(songReference))
            .collect(toImmutableList());

    if (connections.isEmpty()) {
      return Optional.empty();
    }
    if (connections.size() > 1) {
      throw new IllegalStateException(
          "There should be at most be one hymn matching each reference.");
    }
    return Optional.of(connections.get(0));
  }

  private Optional<H4aHymn> getHymnFrom(H4aKey key, List<H4aHymn> hymns) {
    Set<H4aHymn> connections = hymns.stream()
        .filter(hymn -> hymn.getId().equals(key))
        .collect(Collectors.toSet());
    if (connections.isEmpty()) {
      return Optional.empty();
    }
    if (connections.size() > 1) {
      throw new IllegalStateException(
          "There should be at most be one hymn matching each reference.");
    }
    return connections.stream().findFirst();
  }

  private boolean lyricsMatch(H4aHymn hymn1, H4aHymn hymn2) {
    List<Verse> hymn1Lyrics = hymn1.getVersesList();
    List<Verse> hymn2Lyrics = hymn2.getVersesList();
    if (hymn1Lyrics.size() != hymn2Lyrics.size()) {
      return false;
    }
    for (int i = 0; i < hymn1Lyrics.size(); i++) {
      Verse hymn1Verse = hymn1Lyrics.get(i);
      Verse hymn2Verse = hymn2Lyrics.get(i);
      if (!hymn1Verse.getVerseType().equals(hymn2Verse.getVerseType())) {
        return false;
      }
    }
    return true;
  }

  /**
   * There are certain Liederbuch songs that do map to an already-processed song, but there's no
   * explicit mapping in the file, so we need to manually fix them here.
   */
  private Optional<SongReference> manualMapping(SongReference songReference) {
    if (HymnType.fromString(songReference.getHymnType()) != LIEDERBUCH) {
      return Optional.empty();
    }
    switch (songReference.getHymnNumber()) {
      case "419":
        return Optional.of(
            SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("180de").build());
      case "420":
        return Optional.of(
            SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("151de").build());
      default:
        return Optional.empty();
    }
  }
}
