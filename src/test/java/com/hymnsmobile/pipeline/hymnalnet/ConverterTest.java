package com.hymnsmobile.pipeline.hymnalnet;

import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.hymnsmobile.pipeline.hymnalnet.HymnType.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConverterTest {

  private Set<PipelineError> errors;

  @BeforeEach
  public void setUp() {
    this.errors = new HashSet<>();
  }

  @Test
  public void extractFromPath__simplePath__extractsCorrectKey() {
    assertThat(Converter.extractFromPath("/en/hymn/h/339", HymnalNetKey.getDefaultInstance(), errors))
        .hasValue(HymnalNetKey.newBuilder().setHymnType(CLASSIC_HYMN.abbreviation).setHymnNumber("339").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void extractFromPath__hymnNumberContainsLetter__extractsCorrectKey() {
    assertThat(Converter.extractFromPath("/en/hymn/ns/c339abd", HymnalNetKey.getDefaultInstance(), errors))
        .hasValue(HymnalNetKey.newBuilder().setHymnType(NEW_SONG.abbreviation).setHymnNumber("c339abd").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void extractFromPath__pathWithQueryParams__extractsCorrectKey() {
    assertThat(Converter.extractFromPath("/en/hymn/ch/339?gb=1&param2=2", HymnalNetKey.getDefaultInstance(), errors))
        .hasValue(HymnalNetKey.newBuilder().setHymnType(CHINESE.abbreviation).setHymnNumber("339").setQueryParams("?gb=1").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void extractFromPath__hinario__extractsCorrectKey() {
    assertThat(Converter.extractFromPath("https://www.hinario.org/detail.php?hymn=1151ab", HymnalNetKey.getDefaultInstance(), errors))
        .hasValue(HymnalNetKey.newBuilder().setHymnType(HINOS.abbreviation).setHymnNumber("1151ab").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void extractFromPath__emptyHymnType__addsError() {
    assertThat(Converter.extractFromPath("/en/hymn//339",
        HymnalNetKey.newBuilder()
            .setHymnType(CLASSIC_HYMN.abbreviation)
            .setHymnNumber("12")
            .build(),
        errors)).isEmpty();
    assertThat(errors)
        .containsExactly(
            PipelineError.newBuilder()
                .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
                .addMessages("/en/hymn//339, a related song of hymn_type: \"h\"\nhymn_number: \"12\"\n")
                .build());
  }

  @Test
  public void extractFromPath__emptyHymnNumber__addsError() {
    assertThat(Converter.extractFromPath("/en/hymn/h/",
        HymnalNetKey.newBuilder()
            .setHymnType(CLASSIC_HYMN.abbreviation)
            .setHymnNumber("12")
            .build(),
        errors)).isEmpty();
    assertThat(errors)
        .containsExactly(
            PipelineError.newBuilder()
                .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
                .addMessages("/en/hymn/h/, a related song of hymn_type: \"h\"\nhymn_number: \"12\"\n")
                .build());
  }

  @Test
  public void extractFromPath__unrecognizedHymnType__addsError() {
    assertThat(Converter.extractFromPath("/en/hymn/unrecognized/339",
        HymnalNetKey.newBuilder()
            .setHymnType(CLASSIC_HYMN.abbreviation)
            .setHymnNumber("12")
            .build(),
        errors)).isEmpty();
    assertThat(errors)
        .containsExactly(
            PipelineError.newBuilder()
                .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
                .addMessages("/en/hymn/unrecognized/339, a related song of hymn_type: \"h\"\nhymn_number: \"12\"\n")
                .build());
  }

  @Test
  public void extractFromPath__hinario_missingHymnNumber__addsError() {
    assertThat(Converter.extractFromPath("https://www.hinario.org/detail.php?hymn=",
        HymnalNetKey.newBuilder()
            .setHymnType(CLASSIC_HYMN.abbreviation)
            .setHymnNumber("12")
            .build(),
        errors)).isEmpty();
    assertThat(errors)
        .containsExactly(
            PipelineError.newBuilder()
                .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
                .addMessages("https://www.hinario.org/detail.php?hymn=, a related song of hymn_type: \"h\"\nhymn_number: \"12\"\n")
                .build());
  }
}
