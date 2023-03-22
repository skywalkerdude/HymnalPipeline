package com.hymnsmobile.pipeline.sanitization.patchers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Performs one-off patches to the set of hymns from Hymns For Android that are unfixable with a
 * general algorithm.
 */
@SanitizationScope
public abstract class Patcher {

  protected Map<List<SongReference>, Hymn.Builder> builders;

  protected final Set<PipelineError> errors;

  public Patcher(Set<PipelineError> errors) {
    this.errors = errors;
  }

  protected abstract void performPatch();

  public ImmutableMap<ImmutableList<SongReference>, Hymn> patch(
      ImmutableMap<ImmutableList<SongReference>, Hymn> allHymns) {
    this.builders = allHymns.entrySet().stream()
        .collect(
            toMap(entry -> new ArrayList<>(entry.getKey()), entry -> entry.getValue().toBuilder()));
        // .collect(toImmutableMap(Entry::getKey, entry -> entry.getValue().toBuilder()));

    performPatch();

    return builders.entrySet().stream()
        .collect(toImmutableMap(
            entry -> ImmutableList.copyOf(entry.getKey()), entry -> entry.getValue().build()));
  }

  protected void removeRelevants(SongReference.Builder songReference,
      SongReference.Builder... relevants) {
    Hymn.Builder builder = getHymn(songReference);

    List<SongReference> existingRelevants = builder.getRelevantsList().stream()
        .map(SongLink::getReference).collect(Collectors.toList());
    for (SongReference.Builder relevant : relevants) {
      if (!existingRelevants.contains(relevant.build())) {
        continue;
      }
      int index = existingRelevants.indexOf(relevant.build());
      builder.removeRelevants(index);
      // Need to perform this removal as well so the next iteration of the loop also has the correct
      // indices.
      existingRelevants.remove(index);
    }
  }

  protected void removeLanguages(SongReference.Builder songReference,
      SongReference.Builder... languages) {
    Hymn.Builder builder = getHymn(songReference);

    List<SongReference> existingLanguages = builder.getLanguagesList().stream()
        .map(SongLink::getReference).collect(Collectors.toList());
    for (SongReference.Builder language : languages) {
      if (!existingLanguages.contains(language.build())) {
        continue;
      }
      int index = existingLanguages.indexOf(language.build());
      builder.removeLanguages(index);
      // Need to perform this removal as well so the next iteration of the loop also has the correct
      // indices.
      existingLanguages.remove(index);
    }
  }

  protected void addLanguages(SongReference.Builder songReference, SongLink.Builder... languages) {
    addSongLink(Hymn.getDescriptor().findFieldByName("languages"), songReference, languages);
  }

  protected void addRelevants(SongReference.Builder songReference, SongLink.Builder... relevants) {
    addSongLink(Hymn.getDescriptor().findFieldByName("relevants"), songReference, relevants);
  }

  protected void addSongLink(FieldDescriptor field, SongReference.Builder songReference,
      SongLink.Builder... songLinks) {
    Hymn.Builder builder = getHymn(songReference);
    for (SongLink.Builder songLink : songLinks) {
      builder.addRepeatedField(field, songLink.build());
    }
  }

  protected void clearLanguages(SongReference.Builder songReference) {
    clearSongLinks(Hymn.getDescriptor().findFieldByName("languages"), songReference);
  }

  protected void clearSongLinks(FieldDescriptor field, SongReference.Builder songReference) {
    getHymn(songReference).clearField(field);
  }

  protected void resetLanguages(SongReference.Builder english, SongLink.Builder... languages) {
    resetSongLinks(Hymn.getDescriptor().findFieldByName("languages"), english, "English",
        languages);
  }

  protected void resetRelevants(SongReference.Builder originalTune, SongLink.Builder... relevants) {
    resetSongLinks(Hymn.getDescriptor().findFieldByName("relevants"), originalTune, "Original Tune",
        relevants);
  }

  protected void resetSongLinks(FieldDescriptor field, SongReference.Builder original,
      String songLabel, SongLink.Builder... links) {
    Hymn.Builder originalSong = getHymn(original).clearField(field);
    for (SongLink.Builder link : links) {
      originalSong.addRepeatedField(field, link.build());
      getHymn(link.getReference()).clearField(field)
          .addRepeatedField(field,
              SongLink.newBuilder().setName(songLabel).setReference(original).build());
    }
  }

  protected Hymn.Builder getHymn(SongReference.Builder songReference) {
    return getHymn(songReference.build());
  }

  protected Hymn.Builder getHymn(SongReference songReference) {
    ImmutableList<Hymn.Builder> builder = builders.values().stream().filter(
        hymn -> hymn.getReference().equals(songReference)).collect(toImmutableList());
    if (builder.size() != 1) {
      throw new IllegalStateException(
          String.format("Wrong number of songs with %s were found: %s", songReference, builder));
    }
    return builder.stream().findAny().get();
  }

  protected void removeReference(SongReference.Builder songReference) {
    removeReference(songReference.build());
  }

  protected void removeReference(SongReference songReference) {
    ImmutableList<List<SongReference>> songReferences = builders.keySet().stream().filter(
        references -> references.contains(songReference)).collect(toImmutableList());
    if (songReferences.isEmpty()) {
      errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING)
          .setMessage(String.format("Tried to remove %s but didn't find it", songReference))
          .build());
      return;
    }
    if (songReferences.size() != 1) {
      throw new IllegalStateException(
          String.format("Wrong number of songs with %s were found: %s", songReference,
              songReferences));
    }
    if (songReferences.get(0).size() == 1) {
      builders.remove(songReferences.get(0));
    } else {
      songReferences.get(0).remove(songReference);
    }
  }
}
