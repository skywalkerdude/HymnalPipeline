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
import com.hymnsmobile.pipeline.models.SongReference;
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

  public static SongReference createFromStringAbbreviation(String abbr) {
    return SongReference.newBuilder().setHymnType(abbr.split("/")[0])
        .setHymnNumber(abbr.split("/")[1]).build();
  }

  protected void removeRelevants(String from, String... relevants) {
    removeRelevants(createFromStringAbbreviation(from),
        Arrays.stream(relevants).map(Patcher::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void removeRelevants(SongReference.Builder from,
      SongReference.Builder... relevants) {
    removeRelevants(from.build(),
        Arrays.stream(relevants).map(SongReference.Builder::build).toArray(SongReference[]::new));
  }

  protected void removeRelevants(SongReference from, SongReference... relevants) {
    Hymn.Builder builder = getHymn(from);

    ImmutableList<SongReference> existingRelevants =
        ImmutableList.copyOf(builder.getRelevantsList());

    for (SongReference relevant : relevants) {
      if (existingRelevants.contains(relevant)) {
        builder.removeRelevants(builder.getRelevantsList().indexOf(relevant));
      } else {
        errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.WARNING)
                .setErrorType(ErrorType.PATCHER_REMOVAL_ERROR)
                .addMessages(String.format("%s not a relevant of %s", relevant, from))
                .build());
      }
    }
  }

  protected void removeLanguages(String from, String... languages) {
    removeLanguages(from, true, languages);
  }

  protected void removeLanguages(String from, boolean includeSimplified, String... languages) {
    removeLanguages(createFromStringAbbreviation(from), includeSimplified,
        Arrays.stream(languages).map(Patcher::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void removeLanguages(SongReference from, boolean includeSimplified,
      SongReference... languages) {
    Hymn.Builder builder = getHymn(from);

    ImmutableList<SongReference> languagesToRemove = Arrays.stream(languages)
        .flatMap((Function<SongReference, Stream<SongReference>>) songReference -> {
          if (!includeSimplified) {
            return ImmutableList.of(songReference).stream();
          }

          // If the song is a Chinese song, also add the simplified version
          HymnType songReferenceType = HymnType.fromString(songReference.getHymnType());
          if (songReferenceType == HymnType.CHINESE) {
            return ImmutableList.of(songReference, SongReference.newBuilder().setHymnType("chx")
                .setHymnNumber(songReference.getHymnNumber()).build()).stream();
          } else if (songReferenceType == HymnType.CHINESE_SUPPLEMENTAL) {
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

  protected void addLanguages(String to, String... languages) {
    addLanguages(createFromStringAbbreviation(to),
        Arrays.stream(languages).map(Patcher::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void addLanguages(SongReference to, SongReference... languages) {
    addSongLinks(to, Hymn.getDescriptor().findFieldByName("languages"), languages);
  }

  protected void addLanguages(SongReference.Builder to,
      SongReference.Builder... languages) {
    addSongLinks(to, Hymn.getDescriptor().findFieldByName("languages"), languages);
  }

  protected void addRelevants(String to, String... relevants) {
    addRelevants(createFromStringAbbreviation(to),
        Arrays.stream(relevants).map(Patcher::createFromStringAbbreviation)
            .toArray(SongReference[]::new));
  }

  protected void addRelevants(SongReference.Builder to, SongReference.Builder... relevants) {
    addSongLinks(to, Hymn.getDescriptor().findFieldByName("relevants"), relevants);
  }

  protected void addRelevants(SongReference to, SongReference... relevants) {
    addSongLinks(to, Hymn.getDescriptor().findFieldByName("relevants"), relevants);
  }

  protected void addSongLinks(SongReference.Builder to, FieldDescriptor field,
      SongReference.Builder... songLinks) {
    addSongLinks(to.build(), field, Arrays.stream(songLinks).map(SongReference.Builder::build)
        .toArray(SongReference[]::new));
  }

  protected void addSongLinks(SongReference to, FieldDescriptor field,
      SongReference... songLinks) {
    // If the song is a Chinese song, also perform for the simplified version.
    HymnType hymnType = HymnType.fromString(to.getHymnType());
    if (hymnType == HymnType.CHINESE) {
      addSongLinks(
          SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED.abbreviatedValue)
              .setHymnNumber(to.getHymnNumber()).build(), field, songLinks);
    }
    if (hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
      addSongLinks(
          SongReference.newBuilder()
              .setHymnType(HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
              .setHymnNumber(to.getHymnNumber()).build(), field, songLinks);
    }

    Hymn.Builder builder = getHymn(to);
    // noinspection unchecked
    List<SongReference> links = (List<SongReference>) builder.getField(field);
    for (SongReference songLink : songLinks) {
      if (links.contains(songLink)) {
        errors.add(PipelineError.newBuilder()
            .setSeverity(Severity.WARNING)
            .setErrorType(ErrorType.PATCHER_ADD_ERROR)
            .addMessages(String.format("%s already includes %s as a %s", to, songLink, field))
            .build());
        continue;
      }
      builder.addRepeatedField(field, songLink);
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
}
