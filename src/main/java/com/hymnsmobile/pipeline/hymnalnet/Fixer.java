package com.hymnsmobile.pipeline.hymnalnet;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Performs algorithmic fixes on the set of hymns from Hymnal.net.
 */
@HymnalNetPipelineScope
public class Fixer {

  private final LanguageAuditor languageAuditor;
  private final Map<SongReference, Hymn.Builder> referenceHymnMap;
  private final RelevantsAuditor relevantsAuditor;
  private final Set<PipelineError> errors;
  private final Set<Hymn> hymns;

  @Inject
  public Fixer(LanguageAuditor languageAuditor, RelevantsAuditor relevantsAuditor,
      Set<PipelineError> errors, @HymnalNet Set<Hymn> hymns) {
    this.errors = errors;
    this.hymns = hymns;
    this.languageAuditor = languageAuditor;
    this.referenceHymnMap = new HashMap<>();
    this.relevantsAuditor = relevantsAuditor;
  }

  public void fix() {
    this.referenceHymnMap.clear();
    referenceHymnMap.putAll(hymns.stream().collect(Collectors.toMap(Hymn::getReference, Hymn::toBuilder)));
    fixLanguages();
    fixRelevants();
  }

  private void fixLanguages() {
    FieldDescriptor languageFieldDescriptor = Hymn.getDescriptor().findFieldByName("languages");
    Set<Set<SongLink>> languageSets = generateSongLinkSets(languageFieldDescriptor);
    languageAuditor.audit(languageSets);
    writeSongLinks(languageFieldDescriptor, languageSets);
  }

  private void fixRelevants() {
    FieldDescriptor languageFieldDescriptor = Hymn.getDescriptor().findFieldByName("relevants");
    Set<Set<SongLink>> relevantsSets = generateSongLinkSets(languageFieldDescriptor);
    relevantsAuditor.audit(relevantsSets);
    writeSongLinks(languageFieldDescriptor, relevantsSets);
  }

  /**
   * Generates aggregated sets of {@link SongLink}s that represent all links of a single song,
   * described by the {@link FieldDescriptor}.
   */
  private Set<Set<SongLink>> generateSongLinkSets(FieldDescriptor descriptor) {
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
      populateSongLinkSet(descriptor,
          SongLink.newBuilder().setReference(hymn.getReference()).build(), songLinkSet);

      // If the nameless song we added is still nameless, that means there was something wrong with
      // the mapping. We will try to infer it, but if that is not possible, then we add an error
      // and return.
      List<SongLink> nameLessSongs = songLinkSet.stream()
          .filter(songLink -> TextUtil.isEmpty(songLink.getName())).collect(Collectors.toList());
      if (nameLessSongs.size() > 1)  {
        throw new IllegalStateException(
            String.format("Multiple nameless songs in %s", nameLessSongs));
      }
      // If there exists a nameless song, try to rectify it by inferring the name
      if (nameLessSongs.size() == 1) {
        SongLink namelessSong = nameLessSongs.get(0);
        songLinkSet.remove(nameLessSongs.get(0));

        // TODO determine if we should infer or not
        Optional<String> inferredName = inferName(namelessSong.getReference());
        if (inferredName.isPresent()) {
          songLinkSet.add(
              namelessSong.toBuilder().setName(inferredName.get()).build());
        } else {
          errors.add(PipelineError.newBuilder().setSeverity(Severity.ERROR).setMessage(
              String.format("Dangling reference: %s in %s", hymn.getReference(), songLinkSet)).build());
          return;
        }
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

  private void populateSongLinkSet(FieldDescriptor descriptor, SongLink songLink,
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
    ((List<SongLink>) referenceHymnMap.get(songLink.getReference()).getField(descriptor)).forEach(
        linkedSong -> populateSongLinkSet(descriptor, linkedSong, songLinkSet));
  }

  /**
   * We hope that most songs are some kind of circular reference (i.e. h/1 -> cb/1 -> h/1). This
   * way, we get the name of the reference for free. There are some cases where a song references a
   * group of songs, but there is no reference to it. In those cases, we can fall back to inferring
   * the name from the type of the hymn. However, this is very much a last resort as it's still
   * preferable to explicitly fix the song in {@link Fixer} as this approach may swallow up some
   * errors.
   */
  private Optional<String> inferName(SongReference songReference) {
    if (songReference.getType() == HymnType.GERMAN) {
      // Large portion of songs on Hymnal.net have only a one-way reference to the German song,
      // meaning that the German song references other languages, but they don't reference it
      // back. In this case, we fall back to inferring the name.
      return Optional.of("German");
    }
    return Optional.empty();
  }

  /**
   * Write the newly aggregated {@link SongLink} sets onto each hymn.
   */
  private void writeSongLinks(FieldDescriptor descriptor, Set<Set<SongLink>> songLinkSets) {
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
      referenceHymnMap.get(songReference).clearField(descriptor).setField(descriptor, newLinks);
    });
    hymns.clear();
    hymns.addAll(
        referenceHymnMap.values().stream().map(Hymn.Builder::build).collect(Collectors.toList()));
  }
}
