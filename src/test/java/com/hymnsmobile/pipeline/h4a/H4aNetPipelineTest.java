package com.hymnsmobile.pipeline.h4a;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;

class H4aNetPipelineTest {

  private H4aPipeline h4aPipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    h4aPipeline = DaggerPipelineTestComponent.create().h4aComponent().build().pipeline();
  }

  @Test
  public void runEndToEnd() throws BadHanyuPinyinOutputFormatCombination, SQLException, IOException {
    h4aPipeline.run();

    ImmutableList<H4aHymn> h4aHymns = h4aPipeline.getH4aHymns();
    assertThat(h4aHymns.size()).isEqualTo(12488);

    H4aHymn expected =
        TestUtils.readTextProto("src/test/resources/h4a/storage/E1336.textproto", H4aHymn.newBuilder());

    ProtoTruth.assertThat(
            h4aHymns.stream()
                .filter
                    (h4aHymn ->
                        h4aHymn.getId().equals(H4aKey.newBuilder().setType("E").setNumber("1336").build()))
                .findFirst().orElseThrow())
        .isEqualTo(expected);
    ProtoTruth.assertThat(h4aPipeline.getErrors()).isEmpty();
  }
}
