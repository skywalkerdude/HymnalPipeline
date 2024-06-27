package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.models.H4aKey;
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
    assertThat(target.toKey("SK55")).isEqualTo(H4aKey.newBuilder().setType("SK").setNumber("55").build());
  }

  @Test
  void toKey__complexId__extractsCorrectKey() {
    assertThat(target.toKey("Ce506c")).isEqualTo(H4aKey.newBuilder().setType("C").setNumber("e506c").build());
  }

  @Test
  public void fetchHymns__missingHymnType__throwsException() {
    assertThatThrownBy(() ->
        target.toKey("33"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unable to extract type from 33");
  }

  @Test
  public void fetchHymns__unrecognizedHymnType__throwsException() {
    assertThatThrownBy(() ->
        target.toKey("UNKNOWN3"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
  }
}
