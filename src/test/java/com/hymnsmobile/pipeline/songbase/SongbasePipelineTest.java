package com.hymnsmobile.pipeline.songbase;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.songbase.models.Songbase;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hymnsmobile.pipeline.dagger.PipelineTestModule.MOCK_FILE_WRITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class SongbasePipelineTest {

  private SongbasePipeline pipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    pipeline = DaggerPipelineTestComponent.create().songbasePipelineComponent().build().pipeline();
  }

  @Test
  public void runEndToEnd() throws IOException, InterruptedException {
    pipeline.run();

    ImmutableList<SongbaseHymn> songbaseHymns = pipeline.getSongbaseHymns();
    assertThat(songbaseHymns.size()).isEqualTo(6);

    Songbase expected =
        TestUtils.readTextProto("src/test/resources/songbase/output/output.textproto", Songbase.newBuilder());

    assertThat(songbaseHymns).containsExactlyInAnyOrderElementsOf(expected.getSongbaseHymnList());
    assertThat(pipeline.getErrors()).isEmpty();

    ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> response = ArgumentCaptor.forClass(String.class);
    verify(MOCK_FILE_WRITER).writeString(fileName.capture(), response.capture());
    assertThat(fileName.getValue()).isEqualTo("storage/songbase/1993-07-17_10-10-00_PDT.txt");
    assertThat(response.getValue()).isEqualTo(TestUtils.readText("src/test/resources/songbase/input/_api_v2_app_data"));
  }
}