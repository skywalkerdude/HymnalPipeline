package com.hymnsmobile.pipeline.russian;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;

class RussianPipelineTest {

  private RussianPipeline pipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    pipeline = DaggerPipelineTestComponent.create().russianPipeline().build().pipeline();
  }

  @Test
  public void runEndToEnd() throws IOException {
    pipeline.run();

    ImmutableList<RussianHymn> russianHymns = pipeline.getRussianHymns();
    assertThat(russianHymns.size()).isEqualTo(793);

   RussianHymn expected =
        TestUtils.readTextProto("src/test/resources/russian/storage/R1.textproto", RussianHymn.newBuilder());

    ProtoTruth.assertThat(
            russianHymns.stream()
                .filter(russianHymn -> russianHymn.getNumber() == 1)
                .findFirst().orElseThrow())
        .isEqualTo(expected);
  }
}
