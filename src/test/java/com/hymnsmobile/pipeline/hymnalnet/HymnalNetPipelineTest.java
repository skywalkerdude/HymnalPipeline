package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.hymnalnet.models.*;
import com.hymnsmobile.pipeline.testutil.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static com.hymnsmobile.pipeline.dagger.PipelineTestModule.MOCK_FILE_WRITER;
import static org.mockito.Mockito.verify;

@ExtendWith({FetchHymnsExtension.class, ReadFromStorageExtension.class})
class HymnalNetPipelineTest {

  private HymnalNetPipeline hymnalNetPipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    hymnalNetPipeline = DaggerPipelineTestComponent.create().hymnalNetTestComponent().build().pipeline();
  }

  @Test
  @FetchHymns(keysToFetch = {"h/1336"})
  public void fetchSingleSongFromNetwork__shouldFetchSong() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymnalNetJsons()).hasSize(1);
    assertThat(hymnalNetPipeline.getHymnalNetJsons().get(0)).ignoringRepeatedFieldOrder()
        .isEqualTo(HymnalNetJson.newBuilder()
        .setKey(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("1336"))
        .setTitle("Hymn: What shall I give unto the Lord")
        .addLyrics(
            com.hymnsmobile.pipeline.hymnalnet.models.Verse.newBuilder().setVerseType("verse")
                .addAllVerseContent(
                    ImmutableList.of("What shall I give unto the Lord",
                        "For all, for all, for all He’s done for me?",
                        "I’ll take the cup of salvation,",
                        "And call, and call, and call upon the name",
                        "  of the Lord.")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Category")
                .addData(Datum.newBuilder().setValue("Scriptures for Singing")
                    .setPath("/en/search/all/category/Scriptures+for+Singing")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Music")
                .addData(Datum.newBuilder().setValue("Adapted by Tony Rosmarin")
                    .setPath("/en/search/all/composer/Tony+Rosmarin?t=h&n=1336")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Key").addData(
                Datum.newBuilder().setValue("D Major")
                    .setPath("/en/search/all/key/D+Major")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Time").addData(
                Datum.newBuilder().setValue("4/4").setPath("/en/search/all/time/4%2F4")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Meter")
                .addData(Datum.newBuilder().setValue("Peculiar Meter.")
                    .setPath("/en/search/all/meter/Peculiar+Meter.")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Hymn Code")
                .addData(Datum.newBuilder().setValue("3334454321")
                    .setPath("/en/search/all/hymncode/3334454321")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Scriptures")
                .addData(
                    Datum.newBuilder().setValue("Psalms 116:12-13")
                        .setPath("http://text.recoveryversion.bible/19_Psalms_116.htm#Psa116-12")))
        .addMetaData(
            MetaDatum.newBuilder().setName("See Also")
                .addData(Datum.newBuilder()
                    .setValue("When the Lord turned again the captivity of Zion")
                    .setPath("/en/hymn/h/1337"))
                .addData(Datum.newBuilder()
                    .setValue("Great is the Lord, and greatly to be praised")
                    .setPath("/en/hymn/h/1335"))
                .addData(Datum.newBuilder()
                    .setValue("Thou hast turned my mourning into dancing for me")
                    .setPath("/en/hymn/h/1334"))
                .addData(Datum.newBuilder()
                    .setValue("Behold how good and how pleasant it is")
                    .setPath("/en/hymn/h/1339"))
                .addData(Datum.newBuilder().setValue("This is My rest forever")
                    .setPath("/en/hymn/h/1338")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Link")
                .addData(Datum.newBuilder()
                    .setValue("Is Jesus in Your Boat?")
                    .setPath("https://gospel.biblesforamerica.org/is-jesus-in-your-boat/")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Lead Sheet")
                .addData(Datum.newBuilder().setValue("Piano")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_p.pdf"))
                .addData(Datum.newBuilder().setValue("Guitar")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_g.pdf"))
                .addData(Datum.newBuilder().setValue("Text")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_gt.pdf")))
        .addMetaData(
            MetaDatum.newBuilder().setName("Music")
                .addData(Datum.newBuilder().setValue("mp3")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/mp3/e1336_i.mp3"))
                .addData(Datum.newBuilder().setValue("MIDI")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/midi/e1336_i.mid"))
                .addData(Datum.newBuilder().setValue("Tune (MIDI)")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/midi/tunes/e1336_tune.midi")))
        .addMetaData(
            MetaDatum.newBuilder().setName("svg")
                .addData(Datum.newBuilder().setValue("Piano")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/svg/e1336_p.svg"))
                .addData(Datum.newBuilder().setValue("Guitar")
                    .setPath("https://www.hymnal.net/Hymns/Hymnal/svg/e1336_g.svg"))).build());
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  @Test
  @FetchHymns(keysToFetch = {"c/60"}) // should be read from stored file via ReadFromStorageExtension
  @ReadFromStorage
  public void fetchSingleSongFromStorage__shouldFetchSong() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymnalNetJsons()).hasSize(1);
    assertThat(hymnalNetPipeline.getHymnalNetJsons().get(0)).ignoringRepeatedFieldOrder()
        .isEqualTo(HymnalNetJson.newBuilder()
            .setKey(HymnalNetKey.newBuilder().setHymnType("c").setHymnNumber("60"))
            .setTitle("Hymn: I’ve got the joy, joy, joy, joy")
            .addLyrics(
                com.hymnsmobile.pipeline.hymnalnet.models.Verse.newBuilder()
                    .setVerseType("verse")
                    .addAllVerseContent(
                        ImmutableList.of("I’ve got the joy, joy, joy, joy",
                            "Down in my heart,",
                            "Down in my heart,",
                            "Down in my heart,",
                            "I’ve got the joy, joy, joy, joy",
                            "Down in my heart,",
                            "Down in my heart to stay.")))
            .addLyrics(
                com.hymnsmobile.pipeline.hymnalnet.models.Verse.newBuilder()
                    .setVerseType("verse")
                    .addAllVerseContent(
                        ImmutableList.of("And I’m so happy,",
                            "So very happy;",
                            "I’ve got the love of Jesus in my heart.",
                            "And I’m so happy,",
                            "So very happy,",
                            "I’ve got the love of Jesus in my heart.")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Category")
                    .addData(
                        Datum.newBuilder()
                            .setValue("Assurance and Joy of Salvation")
                            .setPath("/en/search/all/category/Assurance+and+Joy+of+Salvation")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Subcategory")
                    .addData(
                        Datum.newBuilder()
                            .setValue("General")
                            .setPath("/en/search/all/category/Assurance+and+Joy+of+Salvation%E2%80%94General")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Lyrics")
                    .addData(Datum.newBuilder()
                        .setValue("George Willis Cooke\u00a0(1848-1923)")
                        .setPath("/en/search/all/author/George+Willis+Cooke?t=c&n=60")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Music")
                    .addData(Datum.newBuilder()
                        .setValue("George Willis Cooke\u00a0(1848-1923)")
                        .setPath("/en/search/all/composer/George+Willis+Cooke?t=c&n=60")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Key")
                    .addData(Datum.newBuilder()
                        .setValue("A Major")
                        .setPath("/en/search/all/key/A+Major")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Time")
                    .addData(Datum.newBuilder()
                        .setValue("4/4")
                        .setPath("/en/search/all/time/4%2F4")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Hymn Code")
                    .addData(Datum.newBuilder()
                        .setValue("56717653321")
                        .setPath("/en/search/all/hymncode/56717653321")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("See Also")
                    .addData(Datum.newBuilder()
                        .setValue("Sing praise to Christ Who lives in us")
                        .setPath("/en/hymn/h/1130"))
                    .addData(Datum.newBuilder()
                        .setValue("Loved with everlasting love (Alternate)")
                        .setPath("/en/hymn/nt/284b"))
                    .addData(Datum.newBuilder()
                        .setValue("How firm a foundation (Alternate Tune)")
                        .setPath("/en/hymn/h/339b"))
                    .addData(Datum.newBuilder()
                        .setValue("Jesus has loved me, wonderful Savior")
                        .setPath("/en/hymn/h/336"))
                    .addData(Datum.newBuilder()
                        .setValue("What joy fills my heart")
                        .setPath("/en/hymn/h/1357")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Lead Sheet")
                    .addData(Datum.newBuilder()
                        .setValue("Piano")
                        .setPath("https://www.hymnal.net/Hymns/Children/pdfs/child0060_p.pdf"))
                    .addData(Datum.newBuilder()
                        .setValue("Guitar")
                        .setPath("https://www.hymnal.net/Hymns/Children/pdfs/child0060_g.pdf"))
                    .addData(Datum.newBuilder()
                        .setValue("Text")
                        .setPath("https://www.hymnal.net/Hymns/Children/pdfs/child0060_gt.pdf")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("Music")
                    .addData(Datum.newBuilder()
                        .setValue("mp3")
                        .setPath("https://www.hymnal.net/Hymns/Children/mp3/c0060.mp3"))
                    .addData(Datum.newBuilder()
                        .setValue("MIDI")
                        .setPath("https://www.hymnal.net/Hymns/Children/midi/c0060.mid"))
                    .addData(Datum.newBuilder()
                        .setValue("Tune (MIDI)")
                        .setPath("https://www.hymnal.net/Hymns/Children/midi/tunes/child0060_tune.midi")))
            .addMetaData(
                MetaDatum.newBuilder()
                    .setName("svg")
                    .addData(Datum.newBuilder()
                        .setValue("Piano")
                        .setPath("https://www.hymnal.net/Hymns/Children/svg/child0060_p.svg"))
                    .addData(Datum.newBuilder()
                        .setValue("Guitar")
                        .setPath("https://www.hymnal.net/Hymns/Children/svg/child0060_g.svg")))
            .build());
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  /**
   * Test Chinese songs of the form "ns/510c"
   */
  @Test
  @FetchHymns(keysToFetch = {"ns/510c"})
  public void songsWithNewSongChinese__shouldConvertToChineseSongs() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymnalNetJsons().stream()
        .map(hymnalNetJson -> hymnalNetJson.getKey().toString())
        .collect(toImmutableList()))
        .containsExactly(
            "hymn_type: \"ns\"\nhymn_number: \"510c\"\n",
            "hymn_type: \"ns\"\nhymn_number: \"510c\"\nquery_params: \"?gb=1\"\n",
            "hymn_type: \"ns\"\nhymn_number: \"510\"\n");
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  @Test
  @FetchHymns(keysToFetch = {
      "h/1",
      "ns/510", // chinese languages of this song are of the form "ns/510c" and "ns/510c?gb=1"
      "c/60"// should be read from stored file
  })
  @ReadFromStorage
  public void runEndToEnd() throws IOException {
    hymnalNetPipeline.run();

    HymnalNet expected =
        TestUtils.readTextProto(
            "src/test/resources/hymnalnet/output/hymnal_net_pipeline_test_run.textproto",
            HymnalNet.newBuilder());

    assertThat(hymnalNetPipeline.getHymnalNetJsons()).hasSize(11);
    assertThat(hymnalNetPipeline.getHymnalNetJsons()).containsExactlyElementsIn(
        expected.getHymnanlNetJsonList());
    assertThat(hymnalNetPipeline.getErrors()).containsExactlyElementsIn(expected.getErrorsList());

    ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HymnalNet> proto = ArgumentCaptor.forClass(HymnalNet.class);
    verify(MOCK_FILE_WRITER).writeProto(fileName.capture(), proto.capture());
    assertThat(fileName.getValue()).isEqualTo("storage/hymnalnet/1993-07-17_10-10-00_PDT");
    assertThat(proto.getValue()).ignoringRepeatedFieldOrder().isEqualTo(expected);
  }
}
