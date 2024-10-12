package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.exceptions.Exceptions;
import com.hymnsmobile.pipeline.merge.patchers.Patcher;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static com.hymnsmobile.pipeline.merge.HymnType.CLASSIC_HYMN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SanitizationPipelineTest {

  private Set<PipelineError> errors;

  private SanitizationPipeline target;

  @BeforeEach
  public void setUp() {
    this.errors = new HashSet<>();
    this.target = new SanitizationPipeline(new LanguageAuditor(errors), new RelevantsAuditor(errors), errors);
  }

  @Test
  public void sanitize__emptyInput__returnsEmptyResult() {
    assertThat(target.sanitize(ImmutableList.of())).isEmpty();
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__singleHymn__returnsItself() {
    Hymn hymn = Hymn.newBuilder()
                    .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                    .build();
    assertThat(target.sanitize(ImmutableList.of(hymn))).containsExactly(hymn);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__missingHymnReference__throwsError() {
    Hymn hymn = Hymn.newBuilder()
                    .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                    .build();
    assertThatThrownBy(() -> target.sanitize(ImmutableList.of(hymn)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("hymn references were empty");
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__languageReferenceNotFound__throwsError() {
    Hymn hymn = Hymn.newBuilder()
                    .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                    .addLanguages(SongReference.newBuilder().setHymnNumber("c").setHymnNumber("1"))
                    .build();
    assertThatThrownBy(() -> target.sanitize(ImmutableList.of(hymn)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("results was not of size 1");
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__completeReferencesGraph__populatesAllReferences() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                  .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                  .build();
    Hymn ch1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                   .build();
    Hymn de1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .build();
    Hymn nt1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                   .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();
    Hymn pt1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("pt").setHymnNumber("1"))
                   .build();

    Hymn h1Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                          .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                          .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                          .build();
    Hymn ch1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                           .build();
    Hymn de1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .build();
    Hymn nt1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                           .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .build();
    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1, ch1, de1, nt1, pt1));
    assertThat(result).containsExactly(h1Expected, ch1Expected, de1Expected, nt1Expected, pt1);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__incompleteReferencesGraph__populatesAllReferences() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                  .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                  .build();
    Hymn ch1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .build();
    Hymn de1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                   .build();
    Hymn nt1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                   .build();

    Hymn h1Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                          .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                          .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                          .build();
    Hymn ch1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                           .build();
    Hymn de1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .build();
    Hymn nt1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                           .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .build();
    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1, ch1, de1, nt1));
    assertThat(result).containsExactly(h1Expected, ch1Expected, de1Expected, nt1Expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__danglingReference__fixesDanglingReference() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    Hymn nt1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                   .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();

    Hymn h1Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                          .build();
    Hymn nt1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                           .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .build();
    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1, nt1));
    assertThat(result).containsExactly(h1Expected, nt1Expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__languageReferences_relevantReferences_patcher__usesPatchedResults() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                  .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                  .build();
    Hymn ch1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();
    Hymn de1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("de").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();
    Hymn nt1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("nt").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .build();

    Patcher patcher = mock(Patcher.class);
    doReturn(ImmutableList.of()).when(patcher).patch(any());

    Exceptions exceptions = mock(Exceptions.class);

    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1, ch1, de1, nt1), patcher, exceptions);
    assertThat(result).isEmpty();
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__selfReference__removesReference_addsPipelineError() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    Hymn h1Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .build();
    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1));
    assertThat(result).containsExactly(h1Expected);
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.MERGE)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.SANITIZER_SELF_REFERENCE)
            .addMessages(h1.getReferencesList().toString())
            .build(),
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.MERGE)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.AUDITOR_DANGLING_LANGUAGE_SET)
            .addMessages(h1.getReferencesList().toString())
            .build());
  }

  @Test
  public void sanitize__auditError_addsPipelineError() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .build();
    Hymn h2 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    ImmutableList<Hymn> result = target.sanitize(ImmutableList.of(h1, h2));
    assertThat(result).containsExactly(h1, h2);
    assertThat(errors).ignoringRepeatedFieldOrder().containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.MERGE)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.AUDITOR_TOO_MANY_INSTANCES)
            .addMessages("CLASSIC_HYMN")
            .addMessages("hymn_type: \"h\"\nhymn_number: \"1\"\n")
            .addMessages("hymn_type: \"h\"\nhymn_number: \"2\"\n")
            .build());
  }

  @Test
  public void sanitize__auditError_exception__noErrorAdded() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .build();
    Hymn h2 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    ImmutableList<Hymn> result =
        target.sanitize(ImmutableList.of(h1, h2),
                        new Patcher(errors) {
                          @Override
                          protected void performPatch() {
                            // do nothing
                          }
                        },
                        new Exceptions() {
                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
                            return ImmutableSet.of(
                                ImmutableSet.of(
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("1").build(),
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("2").build())
                            );
                          }

                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> relevantExceptions() {
                            return ImmutableSet.of();
                          }
                        });
    assertThat(result).containsExactly(h1, h2);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__language_auditError_exception_multiSet__noErrorAdded() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                  .build();
    Hymn h2 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    Hymn ch1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();
    Hymn h2Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                          .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .addLanguages(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                          .build();
    Hymn ch1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addLanguages(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                           .build();
    ImmutableList<Hymn> result =
        target.sanitize(ImmutableList.of(h1, h2, ch1),
                        new Patcher(errors) {
                          @Override
                          protected void performPatch() {
                            // do nothing
                          }
                        },
                        new Exceptions() {
                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
                            return ImmutableSet.of(
                                ImmutableSet.of(
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("1").build(),
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("2").build())
                            );
                          }

                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> relevantExceptions() {
                            return ImmutableSet.of();
                          }
                        });
    assertThat(result).containsExactly(h1, h2Expected, ch1Expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void sanitize__relevant_auditError_exception_multiSet__noErrorAdded() {
    Hymn h1 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addRelevants(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                  .build();
    Hymn h2 = Hymn.newBuilder()
                  .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                  .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                  .build();
    Hymn ch1 = Hymn.newBuilder()
                   .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                   .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                   .build();
    Hymn h2Expected = Hymn.newBuilder()
                          .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                          .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                          .addRelevants(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                          .build();
    Hymn ch1Expected = Hymn.newBuilder()
                           .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("1"))
                           .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("1"))
                           .addRelevants(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
                           .build();
    ImmutableList<Hymn> result =
        target.sanitize(ImmutableList.of(h1, h2, ch1),
                        new Patcher(errors) {
                          @Override
                          protected void performPatch() {
                            // do nothing
                          }
                        },
                        new Exceptions() {
                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> languageExceptions() {
                            return ImmutableSet.of();
                          }

                          @Override
                          public ImmutableSet<ImmutableSet<SongReference>> relevantExceptions() {
                            return ImmutableSet.of(
                                ImmutableSet.of(
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("1").build(),
                                    SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
                                                 .setHymnNumber("2").build())
                            );
                          }
                        });
    assertThat(result).containsExactly(h1, h2Expected, ch1Expected);
    assertThat(errors).isEmpty();
  }
}
