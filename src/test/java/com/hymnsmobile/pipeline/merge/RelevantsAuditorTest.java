package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

class RelevantsAuditorTest {

  private Set<PipelineError> errors;
  private RelevantsAuditor target;

  @BeforeEach
  public void setUp() {
    this.errors = new HashSet<>();
    this.target = new RelevantsAuditor(errors);
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
  public void audit__danglingRelevantSet__errorAdded() {
    SongReference songReference =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_DANGLING_RELEVANT_SET)
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
  public void audit__incompatibleTypes__errorAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    target.audit(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2)), Optional.empty());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_INCOMPATIBLE_RELEVANTS)
                     .addMessages(String.format("[%s, %s]", songReference1, songReference2))
                     .build());
  }

  @Test
  public void audit__incompatibleTypes_hasException__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    target.audit(
        ImmutableSet.of(new HashSet<>(ImmutableSet.of(songReference1, songReference2))),
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).isEmpty();
  }

  @Test
  public void audit__incompatibleTypes_hasException__originalReferencesPreserved() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    Set<Set<SongReference>> songReferenceSets =
        ImmutableSet.of(new LinkedHashSet<>(ImmutableSet.of(songReference1, songReference2)));
    target.audit(
        songReferenceSets,
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    Truth.assertThat(songReferenceSets).containsExactly(new LinkedHashSet<>(ImmutableSet.of(songReference1, songReference2)));
  }

  @Test
  public void audit__incompatibleTypes_hasException_butStillIncompatible__noErrorsAdded() {
    SongReference songReference1 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1").build();
    SongReference songReference2 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("2").build();
    SongReference songReference3 =
        SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue).setHymnNumber("3").build();
    SongReference songReference4 =
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue).setHymnNumber("4").build();
    target.audit(
        ImmutableSet.of(new LinkedHashSet<>(ImmutableSet.of(songReference1, songReference2, songReference3, songReference4))),
        Optional.of(ImmutableSet.of(ImmutableSet.of(songReference1, songReference2))));
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.AUDITOR_INCOMPATIBLE_RELEVANTS)
                     .addMessages(String.format("[%s, %s]", songReference3, songReference4))
                     .build());
  }
}
