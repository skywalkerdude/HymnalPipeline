package com.hymnsmobile.pipeline.h4a;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineTestModule;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

class H4aPipelineTest {

  @BeforeEach
  public void setUp() {
    H4aPipelineTestModule.overrideMiscBlockList.clear();
    H4aPipelineTestModule.overrideNonExistentRelatedSongs.clear();
  }

  @Test
  public void blockListErrors__addPipelineErrors() throws BadHanyuPinyinOutputFormatCombination, SQLException {
    H4aPipelineTestModule.overrideMiscBlockList.add("E14214124");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES140");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES163");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES164");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES261");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES221");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES300");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES421");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES422");
    H4aPipelineTestModule.overrideMiscBlockList.add("ES437");

    // Need to add "E2" twice so it triggers the NON_EXISTENT case in related songs.
    H4aPipelineTestModule.overrideNonExistentRelatedSongs.add("E2");
    H4aPipelineTestModule.overrideNonExistentRelatedSongs.add("E2");

    H4aPipeline pipeline = DaggerPipelineTestComponent.create().h4aTestComponent().build().pipeline();
    pipeline.run();

    ProtoTruth.assertThat(pipeline.getErrors()).containsExactly(
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.WARNING)
            .setErrorType(PipelineError.ErrorType.PATCHER_OBSOLETE_BLOCK_LIST_ITEM)
            .setSource(PipelineError.Source.H4A)
            .addMessages("type: \"E\"\nnumber: \"2\"\n")
            .addMessages("E14214124")
            .build(),
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .setSource(PipelineError.Source.H4A)
            .addMessages("ES500")
            .build());
  }

  @Test
  public void runEndToEnd() throws BadHanyuPinyinOutputFormatCombination, SQLException, IOException {
    H4aPipeline pipeline = DaggerPipelineTestComponent.create().h4aTestComponent().build().pipeline();
    pipeline.run();

    ImmutableList<H4aHymn> h4aHymns = pipeline.getH4aHymns();

    H4aHymn expected =
        TestUtils.readTextProto("src/test/resources/h4a/storage/E1336.textproto", H4aHymn.newBuilder());

    ProtoTruth.assertThat(
                  h4aHymns.stream()
                          .filter
                              (h4aHymn ->
                                   h4aHymn.getId().equals(H4aKey.newBuilder().setType("E").setNumber("1336").build()))
                          .findFirst().orElseThrow())
              .isEqualTo(expected);
    ProtoTruth.assertThat(pipeline.getErrors()).isEmpty();
  }
}
