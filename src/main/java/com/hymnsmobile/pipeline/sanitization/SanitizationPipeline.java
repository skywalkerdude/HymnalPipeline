package com.hymnsmobile.pipeline.sanitization;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.dagger.Sanitization;
import com.hymnsmobile.pipeline.sanitization.patchers.H4aPatcher;
import com.hymnsmobile.pipeline.sanitization.patchers.HymnalNetPatcher;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Pipeline that looks for duplicate hymns and merges them into a single hymn with multiple
 * references.
 */
public class SanitizationPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final LanguageAuditor languageAuditor;
  private final H4aPatcher h4aPatcher;
  private final HymnalNetPatcher hymnalNetPatcher;
  private final RelevantsAuditor relevantsAuditor;
  private final Set<PipelineError> errors;

  @Inject
  public SanitizationPipeline(
      H4aPatcher h4aPatcher,
      HymnalNetPatcher hymnalNetPatcher,
      LanguageAuditor languageAuditor,
      RelevantsAuditor relevantsAuditor,
      @Sanitization Set<PipelineError> errors) {
    this.errors = errors;
    this.languageAuditor = languageAuditor;
    this.h4aPatcher = h4aPatcher;
    this.hymnalNetPatcher = hymnalNetPatcher;
    this.relevantsAuditor = relevantsAuditor;
  }

  public ImmutableMap<ImmutableList<SongReference>, Hymn> sanitize(
      ImmutableMap<ImmutableList<SongReference>, Hymn> allHymns) {
    LOGGER.info("Sanitization pipeline starting");
    ImmutableMap<ImmutableList<SongReference>, Hymn> hymnalNetPatchedHymns =
        hymnalNetPatcher.patch(allHymns);
    ImmutableMap<ImmutableList<SongReference>, Hymn> patchedHymens =
        h4aPatcher.patch(hymnalNetPatchedHymns);

    ImmutableList<Hymn> hymns = patchedHymens.values().stream().collect(toImmutableList());
    ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap = hymns.stream()
        .collect(toImmutableMap(Hymn::getReference, Hymn::toBuilder));

    fixLanguages(hymns, referenceHymnMap);
    fixRelevants(hymns, referenceHymnMap);

    ImmutableMap.Builder<ImmutableList<SongReference>, Hymn> fixedHymns = ImmutableMap.builder();
    referenceHymnMap.forEach((songReference, builder) -> {
      ImmutableList<ImmutableList<SongReference>> matchingReferences = patchedHymens.entrySet()
          .stream()
          .filter(entry -> entry.getValue().getReference().equals(songReference))
          .map(Entry::getKey)
          .collect(toImmutableList());
      if (matchingReferences.size() != 1) {
        throw new IllegalStateException(
            "Couldn't find single set of song references matching " + songReference);
      }
      fixedHymns.put(matchingReferences.get(0), builder.build());
    });
    LOGGER.info("Sanitization pipeline finished");
    return fixedHymns.build();
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  private void fixLanguages(ImmutableList<Hymn> hymns,
      ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap) {
    FieldDescriptor languageFieldDescriptor = Hymn.getDescriptor().findFieldByName("languages");
    Set<Set<SongLink>> languageSets = generateSongLinkSets(hymns, referenceHymnMap,
        languageFieldDescriptor);
    languageAuditor.audit(languageSets);
    writeSongLinks(referenceHymnMap, languageFieldDescriptor, languageSets);
  }

  private void fixRelevants(ImmutableList<Hymn> hymns,
      ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap) {
    FieldDescriptor relevantFieldDescriptor = Hymn.getDescriptor().findFieldByName("relevants");
    Set<Set<SongLink>> relevantsSets = generateSongLinkSets(hymns, referenceHymnMap,
        relevantFieldDescriptor);
    relevantsAuditor.audit(relevantsSets);
    writeSongLinks(referenceHymnMap, relevantFieldDescriptor, relevantsSets);
  }

  /**
   * Generates aggregated sets of {@link SongLink}s that represent all links of a single song,
   * described by the {@link FieldDescriptor}.
   */
  private Set<Set<SongLink>> generateSongLinkSets(
      ImmutableList<Hymn> hymns,
      ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap,
      FieldDescriptor descriptor) {
    Set<Set<SongLink>> songLinkSets = new HashSet<>();
    hymns.forEach(hymn -> {
      // noinspection unchecked
      List<SongLink> links = (List<SongLink>) hymn.getField(descriptor);

      if (links.isEmpty()) {
        return;
      }

      final Set<SongLink> songLinkSet = new LinkedHashSet<>();
      // Start the populating with a nameless SongLink containing the current hymn's SongReference.
      // We should get the name of the reference for free as we process the rest of the songs, but
      // if that doesn't happen, then we add it manually later.
      populateSongLinkSet(referenceHymnMap, descriptor,
          SongLink.newBuilder().setReference(hymn.getReference()).build(), songLinkSet);

      // If the nameless song we added is still nameless, that means there was something wrong with
      // the mapping. We will try to infer it, but if that is not possible, then we add an error
      // and return.
      List<SongLink> nameLessSongs = songLinkSet.stream()
          .filter(songLink -> TextUtil.isEmpty(songLink.getName())).collect(Collectors.toList());
      if (nameLessSongs.size() > 1) {
        throw new IllegalStateException(
            String.format("Multiple nameless songs in %s", nameLessSongs));
      }
      // If there exists a nameless song, try to rectify it by inferring the name
      if (nameLessSongs.size() == 1) {
        SongLink namelessSong = nameLessSongs.get(0);
        songLinkSet.remove(nameLessSongs.get(0));

        Optional<String> inferredName = inferName(namelessSong.getReference());
        inferredName.ifPresentOrElse(
            name -> songLinkSet.add(namelessSong.toBuilder().setName(name).build()),
            () -> errors.add(PipelineError.newBuilder().setSeverity(Severity.ERROR).setMessage(
                    String.format("Dangling reference: %s in %s", hymn.getReference(), songLinkSet))
                .build()));
      }

      // Once we have the song link set for this hymn populated correctly, we attempt to merge it
      // with an existing set, if it exists, that already contains the current songs.
      List<Set<SongLink>> setToMergeWith = songLinkSets.stream().filter(
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

  private void populateSongLinkSet(
      ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap,
      FieldDescriptor descriptor,
      SongLink songLink,
      Set<SongLink> songLinkSet) {
    if (songLinkSet.contains(songLink)) {
      return;
    }

    // Contains the nameless version
    if (songLinkSet.contains(songLink.toBuilder().clearName().build())) {
      // Replace nameless version with named version
      songLinkSet.remove(songLink.toBuilder().clearName().build());
      songLinkSet.add(songLink);
    }

    songLinkSet.add(songLink);
    // noinspection unchecked
    ((List<SongLink>) Objects.requireNonNull(referenceHymnMap.get(songLink.getReference()))
        .getField(descriptor))
        .forEach(linkedSong -> populateSongLinkSet(referenceHymnMap, descriptor, linkedSong,
            songLinkSet));
  }

  /**
   * We hope that most songs are some kind of circular reference (i.e. h/1 -> cb/1 -> h/1). This
   * way, we get the name of the reference for free. There are some cases where a song references a
   * group of songs, but there is no reference to it. In those cases, we can fall back to inferring
   * the name from the type of the hymn. However, this is very much a last resort as it's still
   * preferable to explicitly fix the song in {@link HymnalNetPatcher} as this approach may swallow
   * up some errors.
   */
  private Optional<String> inferName(SongReference songReference) {
    if (songReference.getType() == HymnType.GERMAN) {
      // Large portion of songs on Hymnal.net have only a one-way reference to the German song,
      // meaning that the German song references other languages, but they don't reference it
      // back. In this case, we fall back to inferring the name.
      return Optional.of("German");
    } else if (songReference.getType() == HymnType.JAPANESE) {
      // H4a added Japanese songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Japanese");
    } else if (songReference.getType() == HymnType.KOREAN) {
      // H4a added Korean songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Korean");
    } else if (songReference.getType() == HymnType.FARSI) {
      // H4a added Farsi songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Farsi");
    } else if (songReference.getType() == HymnType.INDONESIAN) {
      // H4a added Indonesian songs, but in general, existing songs yet map to it, so we need to
      // infer the name.
      return Optional.of("Indonesian");
    }
    return Optional.empty();
  }

  /**
   * Write the newly aggregated {@link SongLink} sets onto each hymn.
   */
  private void writeSongLinks(ImmutableMap<SongReference, Hymn.Builder> referenceHymnMap,
      FieldDescriptor descriptor, Set<Set<SongLink>> songLinkSets) {
    referenceHymnMap.keySet().forEach(songReference -> {
      List<Set<SongLink>> setsContainingHymn = songLinkSets.stream().filter(
              songLinks -> songLinks.stream().map(SongLink::getReference)
                  .anyMatch(songLink -> songLink.equals(songReference)))
          .collect(Collectors.toList());

      if (setsContainingHymn.isEmpty()) {
        return;
      }

      if (setsContainingHymn.size() > 1) {
        throw new IllegalStateException("Multiple language sets containing " + songReference);
      }

      // Make a copy of the list, so we aren't destructively altering it within a loop
      List<SongLink> newLinks = new ArrayList<>(setsContainingHymn.get(0));
      // Remove self from set
      if (!newLinks.removeIf(songLink -> songLink.getReference().equals(songReference))) {
        throw new IllegalStateException(songReference + " not found");
      }
      Objects.requireNonNull(referenceHymnMap.get(songReference))
          .clearField(descriptor)
          .setField(descriptor, newLinks);
    });
  }
}
