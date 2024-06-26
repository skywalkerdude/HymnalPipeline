package com.hymnsmobile.pipeline.liederbuch;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchHymn;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;

class LiederbuchPipelineTest {

  private LiederbuchPipeline pipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    pipeline = DaggerPipelineTestComponent.create().liederbuchPipelineComponent().build().pipeline();
  }

  @Test
  public void runEndToEnd() throws IOException {
    pipeline.run();

    ImmutableList<LiederbuchHymn> liederbuchSongs = pipeline.getLiederbuchSong();
    assertThat(liederbuchSongs.size()).isEqualTo(4307);

    LiederbuchHymn expected =
        TestUtils.readTextProto("src/test/resources/liederbuch/storage/G23.textproto", LiederbuchHymn.newBuilder());

    ProtoTruth.assertThat(
            liederbuchSongs.stream()
                .filter
                    (liederbuchSong ->
                        liederbuchSong.getKey().equals(
                            LiederbuchKey.newBuilder().setType(HymnType.GERMAN.abbreviation).setNumber("23").build()))
                .findFirst().orElseThrow())
        .isEqualTo(expected);
    ProtoTruth.assertThat(pipeline.getErrors()).isEmpty();
  }
}
