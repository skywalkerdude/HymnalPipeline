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
  public void obsoleteBlockListEntries__addPipelineErrors() throws BadHanyuPinyinOutputFormatCombination, SQLException, IOException {
    H4aPipelineTestModule.overrideMiscBlockList.add("E14214124");

    // Need to add "E2" twice so it triggers the NON_EXISTENT case in related songs.
    H4aPipelineTestModule.overrideNonExistentRelatedSongs.add("E2");
    H4aPipelineTestModule.overrideNonExistentRelatedSongs.add("E2");

    H4aPipeline pipeline = DaggerPipelineTestComponent.create().h4aTestComponent().build().pipeline();
    pipeline.run();

    ProtoTruth.assertThat(pipeline.getErrors()).containsExactly(
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_KEY)
            .setSource(PipelineError.Source.H4A)
            .addMessages("I\'malwayscallingonYou.")
            .build(),
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_KEY)
            .setSource(PipelineError.Source.H4A)
            .addMessages("OJesusLord")
            .build(),
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_KEY)
            .setSource(PipelineError.Source.H4A)
            .addMessages("IloveYou")
            .build(),
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.WARNING)
            .setErrorType(PipelineError.ErrorType.PATCHER_OBSOLETE_BLOCK_LIST_ITEM)
            .setSource(PipelineError.Source.H4A)
            .addMessages("type: \"E\"\nnumber: \"2\"\n")
            .addMessages("E14214124")
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
