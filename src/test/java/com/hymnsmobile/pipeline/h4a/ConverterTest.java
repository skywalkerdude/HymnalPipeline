package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterTest {

  private Set<PipelineError> pipelineErrors;

  private Converter target;

  @BeforeEach
  void setUp() {
    this.pipelineErrors = new HashSet<>();
    this.target = new Converter(pipelineErrors);
  }

  @Test
  void toKey__simpleId__extractsCorrectKey() {
    assertThat(target.toKey("SK55")).hasValue(H4aKey.newBuilder().setType("SK").setNumber("55").build());
    assertThat(pipelineErrors).isEmpty();
  }

  @Test
  void toKey__complexId__extractsCorrectKey() {
    assertThat(target.toKey("Ce506c")).hasValue(H4aKey.newBuilder().setType("C").setNumber("e506c").build());
  }

  @Test
  public void fetchHymns__missingHymnType__addsErrorToErrorsList() {
    assertThat(target.toKey("33")).isEmpty();
    assertThat(pipelineErrors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.H4A)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_KEY)
            .addMessages("33")
            .build());
  }

  @Test
  public void fetchHymns__unrecognizedHymnType__addsErrorToErrorsList() {
    assertThat(target.toKey("UNKNOWN3")).isEmpty();
    assertThat(pipelineErrors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.H4A)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .addMessages("UNKNOWN3")
            .build());
  }
}
