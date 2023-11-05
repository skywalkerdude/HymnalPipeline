package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.HardcodedDuplicates.H4A_DUPLICATES;
import static com.hymnsmobile.pipeline.merge.Utilities.getHymnFrom;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.models.Verse;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Class that merges H4a songs into the pipeline results.
 */
@MergeScope
public class H4aMerger {

  private final Converter converter;
  private final Set<PipelineError> errors;

  @Inject
  public H4aMerger(Converter converter, @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
  }

  ImmutableList<Hymn> merge(ImmutableList<H4aHymn> h4aHymns, ImmutableList<Hymn> mergedHymns) {
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    h4aHymns.stream()
        // Sort by hymn type
        .sorted(Comparator.comparingInt(
            o -> com.hymnsmobile.pipeline.h4a.HymnType.fromString(o.getId().getType()).orElseThrow()
                .ordinal()))
        // Merge in H4a hymns
        .forEach(h4aHymn -> mergeHymn(h4aHymn, h4aHymns, builders));
    return builders.stream().map(Hymn.Builder::build).collect(toImmutableList());
  }

  private void mergeHymn(
      H4aHymn h4aHymn,
      ImmutableList<H4aHymn> h4aHymns,
      List<Hymn.Builder> builders) {
    SongReference h4aReference = converter.toSongReference(h4aHymn.getId());
    if (H4A_DUPLICATES.containsKey(h4aReference)) {
      getHymnFrom(H4A_DUPLICATES.get(h4aReference), builders).orElseThrow()
          .addReferences(h4aReference)
          .addProvenance("h4a");
      return;
    }

    if (HymnType.fromString(h4aReference.getHymnType()) == HymnType.LIEDERBUCH) {
      mergeGermanHymn(h4aHymn, builders);
      return;
    }

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
            PipelineError.newBuilder()
                .setSeverity(Severity.ERROR)
                .setErrorType(ErrorType.UNEXPECTED_HYMN_TYPE)
                .addMessages("Should have all been added by hymnal.net already")
                .addMessages(h4aReference.toString())
                .build());
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
                    .addLanguages(h4aReference)
                    .addProvenance("h4a"));
        builders.add(converter.toHymn(h4aHymn).toBuilder());
        break;
      case BE_FILLED:
        if (h4aHymn.hasParentHymn()) {
          H4aKey parentKey = h4aHymn.getParentHymn();
          // Add the BE_FILLED song as another reference, not as a new song since it's more than
          // likely a duplicate in terms of content.
          SongReference parentReference = converter.toSongReference(parentKey);
          getHymnFrom(parentReference, builders).orElseThrow().addReferences(h4aReference).addProvenance("h4a");
        } else {
          builders.add(converter.toHymn(h4aHymn).toBuilder());
        }
    }
  }

  /**
   * Most German songs have two sets of lyrics: an older, more doctrinally accurate version, and an
   * updated more singable version, which is less doctrinally accurate. The saints in the
   * German-speaking world have mostly adopted the latter and no longer use the former. Hymnal.net
   * contains the old version while H4a contains the new version.
   * <p/>
   * So here, we are going to both fix the lyrics and add the Liederbuch numbering of the German
   * song provided by H4a.
   */
  private void mergeGermanHymn(H4aHymn h4aGermanHymn, List<Hymn.Builder> builders) {
    SongReference germanSongReference = converter.toSongReference(h4aGermanHymn.getId());

    // Try to find the associated German song that has already been processed
    ImmutableList<SongReference> relatedReferences =
        ImmutableList.<H4aKey>builder()
            .add(h4aGermanHymn.getParentHymn())
            .addAll(h4aGermanHymn.getRelatedList())
            .build().stream()
            .filter(h4aKey ->
                !TextUtil.isEmpty(h4aKey.getType()) && !TextUtil.isEmpty(h4aKey.getNumber()))
            .distinct()
            .map(converter::toSongReference)
            .collect(toImmutableList());
    List<Hymn.Builder> germanSongs =
        relatedReferences.stream()
            .map(relatedReference -> getHymnFrom(relatedReference, builders).orElseThrow())
            .flatMap(relatedBuilder -> relatedBuilder.getLanguagesList().stream())
            .filter(relatedReference -> HymnType.fromString(relatedReference.getHymnType()) == HymnType.GERMAN)
            .map(germanReference -> getHymnFrom(germanReference, builders).orElseThrow())
            .distinct()
            .collect(Collectors.toList());
    // Apply a manual mapping if it is appropriate
    manualMapping(germanSongReference)
        .flatMap(manualMapping -> getHymnFrom(manualMapping, builders))
        .ifPresent(germanSongs::add);

    if (germanSongs.size() == 1) {
      // If the German song is already in the db, we added it as an alternate key to the
      // already existing song.
      germanSongs.get(0).addReferences(germanSongReference);

      // We also fix the lyrics by nuking the existing lyrics and just using the new German lyrics.
      // TODO add some validation here (verse lengths, verse types, etc.)
      germanSongs.get(0).clearLyrics().addAllLyrics(h4aGermanHymn.getVersesList());
    } else if (germanSongs.isEmpty()) {
      // This is a new song, so add it into the list
      builders.add(converter.toHymn(h4aGermanHymn).toBuilder());
    } else {
      throw new IllegalStateException("Shouldn't have more than 1 matching German song");
    }

    // Go through all the related and add Liederbuch as a related song
    relatedReferences.forEach(
        relatedReference -> getHymnFrom(relatedReference, builders).orElseThrow()
            .addLanguages(germanSongReference));
  }

  /**
   * Try to get a song's parent explicitly if possible. If that's not possible, try to infer it.
   */
  private Optional<H4aHymn> guessParent(H4aHymn h4aHymn, ImmutableList<H4aHymn> h4aHymns) {
    if (h4aHymn.hasParentHymn()) {
      return getH4AFrom(h4aHymn.getParentHymn(), h4aHymns);
    }

    // parentKey is null, so we try to infer it from the type
    Set<H4aKey> inferredParentKeys = inferParent(h4aHymn.getId());
    if (inferredParentKeys.isEmpty()) {
      // Couldn't infer any parents, so just give up at this point.
      return Optional.empty();
    }

    for (H4aKey inferredParentKey : inferredParentKeys) {
      Optional<H4aHymn> inferredParentHymn = getH4AFrom(inferredParentKey, h4aHymns);
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
   * @return a ser of {@link H4aKey}s that could be the parent of the passed-in key.
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

  private Optional<H4aHymn> getH4AFrom(H4aKey key, List<H4aHymn> hymns) {
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
    if (HymnType.fromString(songReference.getHymnType()) != HymnType.LIEDERBUCH) {
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
