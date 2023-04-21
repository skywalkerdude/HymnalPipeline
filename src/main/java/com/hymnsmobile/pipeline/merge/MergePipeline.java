package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.hymnsmobile.pipeline.h4a.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.h4a.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.models.HymnType.LIEDERBUCH;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hymnsmobile.pipeline.h4a.HymnType;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchHymn;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.models.Verse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private final Set<PipelineError> errors;

  @Inject
  public MergePipeline(Converter converter, @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
  }

  public ImmutableMap<ImmutableList<SongReference>, Hymn> mergeHymns(
      ImmutableList<HymnalNetJson> hymnalNetHymns,
      ImmutableList<H4aHymn> h4aHymns,
      ImmutableList<LiederbuchHymn> liederbuchHymns) {
    LOGGER.info("Merge pipeline finished");
    // Create mutable versions of everything fo collection.
    Map<List<SongReference>, Hymn.Builder> mergedHymns = new LinkedHashMap<>();

    // Populate the initial list with hymnal.net songs
    mergedHymns.putAll(hymnalNetHymns.stream().collect(toMap(
        hymn -> {
          List<SongReference> songReferences = new ArrayList<>();
          songReferences.add(converter.toSongReference(hymn.getKey()));
          return songReferences;
        },
        hymn -> converter.toHymn(hymn).orElseThrow().toBuilder())));

    h4aHymns
        .stream()
        // Sort by hymn type
        .sorted(Comparator.comparingInt(
            o -> HymnType.fromString(o.getId().getType()).orElseThrow().ordinal()))
        // Merge in H4a hymns
        .forEach(h4aHymn -> mergeH4aHymn(h4aHymn, h4aHymns, mergedHymns));

    liederbuchHymns.forEach(liederbuchHymn -> {
      SongReference songReference = converter.toSongReference(liederbuchHymn.getKey());

      // Only interested in the Liederbuch (German) songs, since all the other songs are covered
      // by Hymnal.net or H4a
      if (songReference.getHymnType() != LIEDERBUCH) {
        return;
      }

      ImmutableList<SongReference> relatedReferences =
          liederbuchHymn.getRelatedList().stream()
              .map(converter::toSongReference)
              .collect(toImmutableList());

      // Try to find the associated German song that has already been processed and add the
      // liederbuch song as an alternate key to that song.
      List<Entry<List<SongReference>, Hymn.Builder>> germanSongs =
          relatedReferences.stream()
              .map(relatedReference -> getHymnFrom(relatedReference, mergedHymns).orElseThrow())
              .flatMap(relatedReference -> relatedReference.getLanguagesList().stream())
              .map(SongLink::getReference)
              .filter(relatedReference -> relatedReference.getHymnType() == GERMAN)
              .map(relatedReference ->
                  getMatchingReferences(relatedReference, mergedHymns).orElseThrow())
              // Add a link to the Liederbuch song to the German song
              // todo need to figure out how to handle the references. maybe in relevants?
              // .peek(entry ->
              //     entry.getValue().addLanguages(
              //         SongLink.newBuilder().setName("Liederbuch").setReference(songReference)))
              .collect(Collectors.toList());

      // Apply a manual mapping if it is appropriate
      manualMapping(songReference)
          .flatMap(manualMapping -> getMatchingReferences(manualMapping, mergedHymns))
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
      germanSongs.get(0).getKey().add(songReference);
    });

    ImmutableMap<ImmutableList<SongReference>, Hymn> rtn = mergedHymns.entrySet().stream()
        .collect(toImmutableMap(
            entry -> ImmutableList.copyOf(entry.getKey()), entry -> entry.getValue().build()));
    LOGGER.info("Merge pipeline finished");
    return rtn;
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

  private void mergeH4aHymn(H4aHymn h4aHymn, ImmutableList<H4aHymn> h4aHymns,
      Map<List<SongReference>, Hymn.Builder> mergedHymns) {
    SongReference h4aReference = converter.toSongReference(h4aHymn.getId());

    switch (h4aReference.getHymnType()) {
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
        // We're only interested in adding new songs, so if the song itself already exists, then we
        // can skip it. Note: At this point, we don't actually need to port over the languages of
        // existing hymns, since they will be added when we encounter them.
        if (getHymnFrom(h4aReference, mergedHymns).isPresent()) {
          return;
        }
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
                getHymnFrom(parent, mergedHymns).orElseThrow()
                    .addLanguages(converter.toLanguageLink(h4aReference)));
        mergedHymns.put(
            // Need to do new ArrayList<>(List.of(...)) to make it a mutable list
            new ArrayList<>(List.of(h4aReference)),
            converter.toHymn(h4aHymn).orElseThrow().toBuilder());
        break;
      case BE_FILLED:
        if (h4aHymn.hasParentHymn()) {
          H4aKey parentKey = h4aHymn.getParentHymn();
          // Add the BE_FILLED song as another reference, not as a new song since it's more
          // than likely a duplicate in terms of content.
          SongReference parentReference = converter.toSongReference(parentKey);
          getMatchingReferences(parentReference, mergedHymns)
              .map(Entry::getKey)
              .orElseThrow()
              .add(h4aReference);
        } else {
          // Need to do new ArrayList<>(List.of(...)) to make it a mutable list
          mergedHymns.put(new ArrayList<>(List.of(h4aReference)),
              converter.toHymn(h4aHymn).orElseThrow().toBuilder());
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
    switch (HymnType.fromString(key.getType()).orElseThrow()) {
      case INDONESIAN:
      case JAPANESE:
      case KOREAN:
        inferredParents.add(
            H4aKey.newBuilder().setType(CHINESE.abbreviation).setNumber(key.getNumber()).build());
        break;
      case FRENCH:
        inferredParents.add(
            H4aKey.newBuilder().setType(CLASSIC_HYMN.abbreviation).setNumber(key.getNumber())
                .build());
        break;
      default:
    }
    return inferredParents;
  }

  /**
   * Gets the entry that matches the passed-in song reference.
   */
  private Optional<Entry<List<SongReference>, Hymn.Builder>> getMatchingReferences(
      SongReference songReference, Map<List<SongReference>, Hymn.Builder> mergedHymns) {
    List<Entry<List<SongReference>, Hymn.Builder>> connections = mergedHymns.entrySet().stream()
        .filter(
            entry -> entry.getKey().contains(songReference)).collect(Collectors.toList());

    if (connections.isEmpty()) {
      return Optional.empty();
    }
    if (connections.size() > 1) {
      throw new IllegalStateException(
          "There should be at most be one hymn matching each reference.");
    }
    return Optional.of(connections.get(0));
  }

  private Optional<Hymn.Builder> getHymnFrom(SongReference songReference,
      Map<List<SongReference>, Hymn.Builder> mergedHymns) {
    return getMatchingReferences(songReference, mergedHymns).map(Entry::getValue);
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
    if (songReference.getHymnType() != LIEDERBUCH) {
      return Optional.empty();
    }
    switch (songReference.getHymnNumber()) {
      case "419":
        return Optional.of(
            SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("180de").build());
      case "420":
        return Optional.of(
            SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("151de").build());
      default:
        return Optional.empty();
    }
  }
}
