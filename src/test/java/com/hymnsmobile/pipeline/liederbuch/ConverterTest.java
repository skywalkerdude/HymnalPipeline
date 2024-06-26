package com.hymnsmobile.pipeline.liederbuch;

import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterTest {

  private Converter target;

  @BeforeEach
  void setUp() {
    this.target = new Converter();
  }

  @Test
  void toKey__simpleId__extractsCorrectKey() {
    assertThat(target.toKey("E55"))
        .isEqualTo(LiederbuchKey.newBuilder().setType(HymnType.CLASSIC_HYMN.abbreviation).setNumber("55").build());
  }

  @Test
  void toKey__complexId__extractsCorrectKey() {
    assertThat(target.toKey("CS300"))
        .isEqualTo(LiederbuchKey.newBuilder().setType(HymnType.CHINESE_SUPPLEMENTAL.abbreviation).setNumber("300").build());
  }

  @Test
  public void fetchHymns__missingHymnType__throwsException() {
    assertThatThrownBy(() ->
        target.toKey("33"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unable to extract type from 33");
  }

  @Test
  public void fetchHymns__unrecognizedHymnType__throwsException() {
    assertThatThrownBy(() ->
        target.toKey("UNKNOWN3"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("No value present");
  }
}
