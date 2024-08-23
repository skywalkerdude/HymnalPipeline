package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

class LanguageAuditorTest {

  private Set<PipelineError> errors;
  private LanguageAuditor target;

  @BeforeEach
  public void setUp() {
    this.errors = new HashSet<>();
    this.target = new LanguageAuditor(errors);
  }

  @Test
  public void audit__emptySet__noErrorsAdded() {
    target.audit(new HashSet<>(), Optional.empty());
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__emptySet_exceptionsNotUsed__errorAdded() {
    SongReference songReference =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    target.audit(ImmutableSet.of(), Optional.of(ImmutableSet.of(ImmutableSet.of(songReference))));
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setErrorType(PipelineError.ErrorType.AUDITOR_OBSOLETE_EXCEPTION)
                     .addMessages(String.format("[[%s]]", songReference))
                     .build());
  }

  @Test
  public void audit__danglingLanguageSet__errorAdded() {
    SongReference songReference =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_DANGLING_LANGUAGE_SET)
                     .addMessages(String.format("[%s]", songReference))
                     .build());
  }

  @Test
  public void audit__multipleInstances__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("2").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_TOO_MANY_INSTANCES)
                     .addMessages(String.format("[%s, %s]", songReference1, songReference2))
                     .addMessages(HymnType.CLASSIC_HYMN.toString())
                     .build());
  }

  @Test
  public void audit__multipleInstances_newSong_withLetter__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("1b").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__multipleInstances_newSong_withoutLetter__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_TOO_MANY_INSTANCES)
                     .addMessages(String.format("[%s, %s]", songReference1, songReference2))
                     .addMessages(HymnType.NEW_SONG.toString())
                     .build());
  }

  @Test
  public void audit__multipleInstances_howardHigashi__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.HOWARD_HIGASHI.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.HOWARD_HIGASHI.abbreviatedValue).setHymnNumber("1b").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__multipleInstances_howardHiagshi_withoutLetter__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.HOWARD_HIGASHI.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.HOWARD_HIGASHI.abbreviatedValue).setHymnNumber("2").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_TOO_MANY_INSTANCES)
                     .addMessages(String.format("[%s, %s]", songReference1, songReference2))
                     .addMessages(HymnType.HOWARD_HIGASHI.toString())
                     .build());
  }

  @Test
  public void audit__multipleInstances_hasException__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("2").build();
    target.audit(
        ImmutableSet.of(new HashSet<>(ImmutableSet.of(songReference1, songReference2))),
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__multipleInstances_hasException_butStillTooManyInstances__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("2").build();
    SongReference songReference3 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("3").build();
    SongReference songReference4 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("4").build();
    target.audit(
        ImmutableSet.of(new LinkedHashSet<>(ImmutableSet.of(songReference1, songReference2, songReference3, songReference4))),
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_TOO_MANY_INSTANCES)
                     .addMessages(String.format("[%s, %s]", songReference3, songReference4))
                     .addMessages(HymnType.CLASSIC_HYMN.toString())
                     .build());
  }

  @Test
  public void audit__incompatibleTypes__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_INCOMPATIBLE_LANGUAGES)
                     .addMessages(String.format("[%s, %s]", songReference1, songReference2))
                     .build());
  }

  @Test
  public void audit__incompatibleTypes_hasLetterSuffix__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("1a").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__incompatibleTypes_hasException__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    Set<Set<SongReference>> songReferenceSets =
        ImmutableSet.of(new HashSet<>(ImmutableSet.of(songReference1, songReference2)));
    target.audit(
        songReferenceSets,
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__incompatibleTypes_hasException__originalReferencesRemoved() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    Set<Set<SongReference>> songReferenceSets =
        ImmutableSet.of(new HashSet<>(ImmutableSet.of(songReference1, songReference2)));
    target.audit(
        songReferenceSets,
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(songReferenceSets).containsExactly(new HashSet<>(ImmutableSet.of()));
  }

  @Test
  public void audit__incompatibleTypes_hasException_butStillIncompatible__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    SongReference songReference3 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("3").build();
    SongReference songReference4 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("4").build();
    Set<Set<SongReference>> songReferenceSets =
        ImmutableSet.of(new HashSet<>(ImmutableSet.of(songReference1, songReference2, songReference3, songReference4)));
    target.audit(
        songReferenceSets,
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_INCOMPATIBLE_LANGUAGES)
                     .addMessages(String.format("[%s, %s]", songReference3, songReference4))
                     .build());
  }
}
