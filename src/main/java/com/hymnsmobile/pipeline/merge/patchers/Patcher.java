package com.hymnsmobile.pipeline.merge.patchers;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.hymnsmobile.pipeline.merge.HymnType;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.models.SongReferenceOrBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Performs one-off patches to the set of hymns from Hymns For Android that are unfixable with a
 * general algorithm.
 */
@MergeScope
public abstract class Patcher {

  protected List<Hymn.Builder> builders;

  protected final Set<PipelineError> errors;

  public Patcher(Set<PipelineError> errors) {
    this.errors = errors;
  }

  protected abstract void performPatch();

  public ImmutableList<Hymn> patch(ImmutableList<Hymn> allHymns) {
    this.builders = allHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());
    performPatch();
    return builders.stream().map(Hymn.Builder::build).collect(toImmutableList());
  }

  protected void removeRelevants(SongReference.Builder songReference,
      SongReference.Builder... relevants) {
    Hymn.Builder builder = getHymn(songReference);

    // Need to make a copy because builder.getRelevantsList() returns an unmodifiable list.
    List<SongReference> existingRelevants = new ArrayList<>(builder.getRelevantsList());
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

  private SongReference createFromStringAbbreviation(String abbr) {
    return SongReference.newBuilder().setHymnType(abbr.split("/")[0])
        .setHymnNumber(abbr.split("/")[1]).build();
  }

  protected void removeLanguages(String from, String... languages) {
    removeLanguages(createFromStringAbbreviation(from),
        Arrays.stream(languages).map(this::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void removeLanguages(SongReference.Builder from, SongReference.Builder... languages) {
    removeLanguages(from.build(), Arrays.stream(languages).map(SongReference.Builder::build)
        .toArray(SongReference[]::new));
  }

  protected void removeLanguages(SongReference from, SongReference... languages) {
    Hymn.Builder builder = getHymn(from);

    ImmutableList<SongReference> languagesToRemove = Arrays.stream(languages)
        .flatMap((Function<SongReference, Stream<SongReference>>) songReference -> {
          // If the song is a Chinese song, also add the simplified version
          HymnType hymnType = HymnType.fromString(songReference.getHymnType());
          if (hymnType == HymnType.CHINESE) {
            return ImmutableList.of(songReference, SongReference.newBuilder().setHymnType("chx")
                .setHymnNumber(songReference.getHymnNumber()).build()).stream();
          } else if (hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
            return ImmutableList.of(songReference, SongReference.newBuilder().setHymnType("tsx")
                .setHymnNumber(songReference.getHymnNumber()).build()).stream();
          } else {
            return ImmutableList.of(songReference).stream();
          }
        }).collect(toImmutableList());

    ImmutableList<SongReference> existingLanguages =
        ImmutableList.copyOf(builder.getLanguagesList());

    for (SongReference language : languagesToRemove) {
      if (existingLanguages.contains(language)) {
        builder.removeLanguages(builder.getLanguagesList().indexOf(language));
      } else {
        errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.WARNING)
                .setErrorType(ErrorType.PATCHER_REMOVAL_ERROR)
                .addMessages(String.format("%s not a language of %s", language, from))
                .build());
      }
    }
  }

  protected void addLanguages(String from, String... languages) {
    addLanguages(createFromStringAbbreviation(from),
        Arrays.stream(languages).map(this::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void addLanguages(SongReference songReference, SongReference... languages) {
    addSongLink(songReference, Hymn.getDescriptor().findFieldByName("languages"), languages);
  }

  protected void addLanguages(SongReference.Builder songReference,
      SongReference.Builder... languages) {
    addSongLink(songReference, Hymn.getDescriptor().findFieldByName("languages"), languages);
  }

  protected void addRelevants(SongReference.Builder songReference,
      SongReference.Builder... relevants) {
    addSongLink(songReference, Hymn.getDescriptor().findFieldByName("relevants"), relevants);
  }

  protected void addSongLink(SongReference.Builder from, FieldDescriptor field,
      SongReference.Builder... songLinks) {
    addSongLink(from.build(), field, Arrays.stream(songLinks).map(SongReference.Builder::build)
        .toArray(SongReference[]::new));
  }

  protected void addSongLink(SongReference from, FieldDescriptor field,
      SongReference... songLinks) {
    Hymn.Builder builder = getHymn(from);
    // noinspection unchecked
    List<SongReference> links = (List<SongReference>) builder.getField(field);
    for (SongReference songLink : songLinks) {
      if (links.contains(songLink)) {
        errors.add(PipelineError.newBuilder()
            .setSeverity(Severity.WARNING)
            .setErrorType(ErrorType.PATCHER_ADD_ERROR)
            .addMessages(String.format("%s already includes %s as a %s", from, songLink, field))
            .build());
        continue;
      }
      builder.addRepeatedField(field, songLink);
    }
  }

  protected void clearLanguages(SongReference.Builder songReference) {
    clearSongLinks(Hymn.getDescriptor().findFieldByName("languages"), songReference);
  }

  protected void clearSongLinks(FieldDescriptor field, SongReference.Builder songReference) {
    getHymn(songReference).clearField(field);
  }

  protected void resetLanguages(SongReference.Builder english, SongReference.Builder... languages) {
    resetSongLinks(Hymn.getDescriptor().findFieldByName("languages"), english, languages);
  }

  protected void resetRelevants(SongReference.Builder originalTune, SongReference.Builder... relevants) {
    resetSongLinks(Hymn.getDescriptor().findFieldByName("relevants"), originalTune, relevants);
  }

  protected void resetSongLinks(FieldDescriptor field, SongReference.Builder original,
      SongReference.Builder... links) {
    Hymn.Builder originalSong = getHymn(original).clearField(field);
    for (SongReference.Builder link : links) {
      originalSong.addRepeatedField(field, link.build());
      getHymn(link).clearField(field).addRepeatedField(field, original.build());
    }
  }

  protected Hymn.Builder getHymn(SongReference.Builder songReference) {
    return getHymn(songReference.build());
  }

  protected Hymn.Builder getHymn(SongReference songReference) {
    ImmutableList<Hymn.Builder> hymn =
        builders.stream().filter(
                builder -> builder.getReferencesList().stream()
                    .anyMatch(reference -> reference.equals(songReference)))
            .collect(toImmutableList());
    if (hymn.size() != 1) {
      throw new IllegalStateException(
          String.format("Wrong number of songs with %s were found: %s", songReference,
              hymn.stream().map(Hymn.Builder::getReferencesList).collect(toImmutableList())));
    }
    return hymn.stream().findAny().get();
  }

  protected void removeReference(SongReference.Builder songReference) {
    removeReference(songReference.build());
  }

  protected void removeReference(SongReference songReference) {
    Hymn.Builder builder = getHymn(songReference);
    if (builder.getReferencesList().size() == 1) {
      builders.remove(builder);
    } else {
      builder.removeReferences(builder.getReferencesList().indexOf(songReference));
    }
  }
}
