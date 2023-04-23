package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.merge.patchers.HymnalNetPatcher;
import com.hymnsmobile.pipeline.merge.patchers.Patcher;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.ArrayList;
import java.util.HashSet;
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
    Set<Set<SongLink>> languageSets =
        generateSongLinkSets(builders, languageFieldDescriptor);
    languageAuditor.audit(languageSets);
    writeSongLinks(builders, languageFieldDescriptor, languageSets);
  }

  private void fixRelevants(ImmutableList<Hymn.Builder> builders) {
    FieldDescriptor relevantFieldDescriptor = Hymn.getDescriptor().findFieldByName("relevants");
    Set<Set<SongLink>> relevantsSets =
        generateSongLinkSets(builders, relevantFieldDescriptor);
    relevantsAuditor.audit(relevantsSets);
    writeSongLinks(builders, relevantFieldDescriptor, relevantsSets);
  }

  /**
   * Generates aggregated sets of {@link SongLink}s that represent all links of a single song,
   * described by the {@link FieldDescriptor}.
   */
  private Set<Set<SongLink>> generateSongLinkSets(
      ImmutableList<Hymn.Builder> builders, FieldDescriptor descriptor) {
    Set<Set<SongLink>> songLinkSets = new HashSet<>();
    builders.forEach(builder -> {
      Hymn hymn = builder.build();
      // noinspection unchecked
      List<SongLink> links = (List<SongLink>) hymn.getField(descriptor);

      if (links.isEmpty()) {
        return;
      }

      final Set<SongLink> songLinkSet = new LinkedHashSet<>();
      // Start the populating with a nameless SongLink containing the current hymn's SongReference.
      // We should get the name of the reference for free as we process the rest of the songs, but
      // if that doesn't happen, then we add it manually later.
      assert hymn.getReferencesCount() > 0;
      populateSongLinkSet(
          builders, descriptor,
          SongLink.newBuilder().setReference(hymn.getReferences(0)).build(), songLinkSet);

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
            () -> errors.add(
                PipelineError.newBuilder()
                    .setSeverity(Severity.ERROR)
                    .setMessage(String.format("Dangling reference: %s in %s", builder, songLinkSet))
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
      ImmutableList<Hymn.Builder> builders,
      FieldDescriptor descriptor,
      SongLink songLink,
      Set<SongLink> songLinkSet) {

    // Contains the nameless version
    SongLink namelessLink = songLink.toBuilder().clearName().build();
    if (songLinkSet.contains(namelessLink)) {
      // Replace nameless version with named version
      songLinkSet.remove(namelessLink);
      songLinkSet.add(songLink);
    } else if (songLinkSet.stream()
        .anyMatch(existing -> existing.getReference().equals(songLink.getReference()))) {
      // Reference already exists in the link (even if it's under a different name)
      return;
    }

    songLinkSet.add(songLink);

    // noinspection unchecked
    ((List<SongLink>) getReferencedHymnBuilder(builders, songLink.getReference())
        .getField(descriptor))
        .forEach(linkedSong ->
            populateSongLinkSet(builders, descriptor, linkedSong, songLinkSet));
  }

  private Hymn.Builder getReferencedHymnBuilder(
      ImmutableList<Hymn.Builder> builders,
      SongReference songReference) {
    ImmutableList<Hymn.Builder> results = builders.stream()
        .filter(builder -> builder.getReferencesList().contains(songReference))
        .collect(toImmutableList());
    assert results.size() == 1;
    return results.get(0);
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
    if (songReference.getHymnType() == HymnType.GERMAN) {
      // Large portion of songs on Hymnal.net have only a one-way reference to the German song,
      // meaning that the German song references other languages, but they don't reference it
      // back. In this case, we fall back to inferring the name.
      return Optional.of("German");
    } else if (songReference.getHymnType() == HymnType.JAPANESE) {
      // H4a added Japanese songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Japanese");
    } else if (songReference.getHymnType() == HymnType.KOREAN) {
      // H4a added Korean songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Korean");
    } else if (songReference.getHymnType() == HymnType.FARSI) {
      // H4a added Farsi songs, but in general, existing songs yet map to it, so we need to infer
      // the name.
      return Optional.of("Farsi");
    } else if (songReference.getHymnType() == HymnType.INDONESIAN) {
      // H4a added Indonesian songs, but in general, existing songs yet map to it, so we need to
      // infer the name.
      return Optional.of("Indonesian");
    }
    return Optional.empty();
  }

  /**
   * Write the newly aggregated {@link SongLink} sets onto each hymn.
   */
  private void writeSongLinks(
      ImmutableList<Hymn.Builder> builders,
      FieldDescriptor descriptor, Set<Set<SongLink>> songLinkSets) {
    builders.forEach(builder -> {
      ImmutableList<SongReference> references = ImmutableList.copyOf(builder.getReferencesList());
      ImmutableList<Set<SongLink>> setContainingHymn =
          songLinkSets.stream()
              .filter(songLinks ->
                  songLinks.stream()
                      .map(SongLink::getReference)
                      .anyMatch(references::contains))
              .collect(toImmutableList());

      if (setContainingHymn.isEmpty()) {
        return;
      }

      assert setContainingHymn.size() == 1;

      // Make a copy of the list, so we aren't destructively altering it within a loop
      List<SongLink> newLinks = new ArrayList<>(setContainingHymn.get(0));
      // Remove self from set
      if (!newLinks.removeIf(songLink -> references.contains(songLink.getReference()))) {
        throw new IllegalStateException(references + " not found");
      }
      builder.clearField(descriptor).setField(descriptor, newLinks);
    });
  }
}
