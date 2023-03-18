package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static com.hymnsmobile.pipeline.dagger.PipelineTestModule.MOCK_FILE_WRITER;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.hymnalnet.models.Datum;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.hymnalnet.models.MetaDatum;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.Line;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.models.Verse;
import com.hymnsmobile.pipeline.testutil.FetchHymns;
import com.hymnsmobile.pipeline.testutil.FetchHymnsExtension;
import com.hymnsmobile.pipeline.testutil.ReadFromStorage;
import com.hymnsmobile.pipeline.testutil.ReadFromStorageExtension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith({FetchHymnsExtension.class, ReadFromStorageExtension.class})
class HymnalNetPipelineTest {

  private HymnalNetPipeline hymnalNetPipeline;

  @BeforeEach
  public void setUp() {
    Logger.getGlobal().setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    hymnalNetPipeline =
        DaggerPipelineTestComponent.create().hymnalNetTestComponent().build().pipeline();
  }

  @Test
  @FetchHymns(keysToFetch = {"h/1336"})
  public void singleSong() throws IOException, URISyntaxException, InterruptedException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymns()).hasSize(1);
    assertThat(hymnalNetPipeline.getHymns().get(0)).isEqualTo(Hymn.newBuilder()
        .setReference(SongReference.newBuilder().setType(HymnType.CLASSIC_HYMN).setNumber("1336"))
        .setTitle("Hymn: What shall I give unto the Lord")
        .addLyrics(
            Verse.newBuilder().setVerseType("verse")
                .addAllLines(
                    ImmutableList.of("What shall I give unto the Lord",
                            "For all, for all, for all He’s done for me?",
                            "I’ll take the cup of salvation,",
                            "And call, and call, and call upon the name",
                            "  of the Lord.").stream()
                        .map(line -> Line.newBuilder().setLineContent(line).build())
                        .collect(toImmutableList())))
        .addCategory("Scriptures for Singing")
        .addComposer("Adapted by Tony Rosmarin")
        .addKey("D Major")
        .addTime("4/4")
        .addMeter("Peculiar Meter.")
        .addScriptures("Psalms 116:12-13")
        .addHymnCode("3334454321")
        .putMusic("mp3", "https://www.hymnal.net/Hymns/Hymnal/mp3/e1336_i.mp3")
        .putMusic("MIDI", "https://www.hymnal.net/Hymns/Hymnal/midi/e1336_i.mid")
        .putMusic("Tune (MIDI)", "https://www.hymnal.net/Hymns/Hymnal/midi/tunes/e1336_tune.midi")
        .putSvgSheet("Guitar", "https://www.hymnal.net/Hymns/Hymnal/svg/e1336_g.svg")
        .putSvgSheet("Piano", "https://www.hymnal.net/Hymns/Hymnal/svg/e1336_p.svg")
        .putPdfSheet("Guitar", "https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_g.pdf")
        .putPdfSheet("Piano", "https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_p.pdf")
        .putPdfSheet("Text", "https://www.hymnal.net/Hymns/Hymnal/pdfs/e1336_gt.pdf")
        .addLanguages(SongLink.newBuilder().setName("French")
            .setReference(SongReference.newBuilder().setType(HymnType.FRENCH).setNumber("9336")))
        .build());

    assertThat(hymnalNetPipeline.getHymnalNetJsons()).hasSize(1);
    assertThat(hymnalNetPipeline.getHymnalNetJsons().get(0)).isEqualTo(HymnalNetJson.newBuilder()
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
            MetaDatum.newBuilder().setName("Languages")
                .addData(Datum.newBuilder().setValue("French")
                    .setPath("/en/hymn/hf/9336")))
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
  @FetchHymns(keysToFetch = {"h/339"})
  public void songWithLanguagesAndTunes()
      throws IOException, URISyntaxException, InterruptedException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymns().stream().map(hymn -> hymn.getReference().toString())
        .collect(toImmutableList())).containsExactly(
        "type: CEBUANO\nnumber: \"339\"\n",
        "type: NEW_TUNE\nnumber: \"339\"\n",
        "type: CLASSIC_HYMN\nnumber: \"339\"\n",
        "type: CHINESE_SIMPLIFIED\nnumber: \"260\"\n",
        "type: CHINESE\nnumber: \"260\"\n",
        "type: CLASSIC_HYMN\nnumber: \"339b\"\n",
        "type: TAGALOG\nnumber: \"339\"\n");
    assertThat(hymnalNetPipeline.getHymnalNetJsons().stream()
        .map(hymnalNetJson -> hymnalNetJson.getKey().toString())
        .collect(toImmutableList())).containsExactly(
        "hymn_type: \"cb\"\nhymn_number: \"339\"\n",
        "hymn_type: \"h\"\nhymn_number: \"339b\"\n",
        "hymn_type: \"ch\"\nhymn_number: \"260\"\n",
        "hymn_type: \"h\"\nhymn_number: \"339\"\n",
        "hymn_type: \"ht\"\nhymn_number: \"339\"\n",
        "hymn_type: \"ch\"\nhymn_number: \"260\"\nquery_params: \"?gb=1\"\n",
        "hymn_type: \"nt\"\nhymn_number: \"339\"\n");
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  /**
   * Test Chinese songs of the form "ns/510c"
   */
  @Test
  @FetchHymns(keysToFetch = {"ns/510c"})
  public void songsWithNewSongChinese__shouldConvertToChineseSongs()
      throws IOException, URISyntaxException, InterruptedException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymns().stream().map(hymn -> hymn.getReference().toString())
        .collect(toImmutableList())).containsExactly(
        "type: NEW_SONG\nnumber: \"510\"\n",
        "type: CHINESE_SIMPLIFIED\nnumber: \"ns510c\"\n",
        "type: CHINESE\nnumber: \"ns510c\"\n");
    assertThat(hymnalNetPipeline.getHymnalNetJsons().stream()
        .map(hymnalNetJson -> hymnalNetJson.getKey().toString())
        .collect(toImmutableList())).containsExactly(
        "hymn_type: \"ns\"\nhymn_number: \"510c\"\n",
        "hymn_type: \"ns\"\nhymn_number: \"510c\"\nquery_params: \"?gb=1\"\n",
        "hymn_type: \"ns\"\nhymn_number: \"510\"\n");
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  @Test
  @FetchHymns(keysToFetch = {
      "h/1",
      "nt/12",
      "ns/1",
      "c/1",
      "hd/1",
      "de/1",
      "ch/1",
      "ch/1/?gb=1",
      "ts/1",
      "ts/1/?gb=1",
      "cb/1",
      "ht/1",
      "hf/1",
      "hs/1",
      "ns/510", // chinese languages of this song are of the form "ns/510c" and "ns/510c?gb=1"
      "c/60"// should be read from stored file
  })
  @ReadFromStorage
  public void run() throws IOException, URISyntaxException, InterruptedException {
    hymnalNetPipeline.run();

    File storedResult = new File(
        "src/test/resources/hymnalnet/output/hymnal_net_pipeline_test_run.txt");
    HymnalNet expected = HymnalNet.parseFrom(new FileInputStream(storedResult));

    assertThat(hymnalNetPipeline.getHymns()).hasSize(18);
    assertThat(hymnalNetPipeline.getHymnalNetJsons()).containsExactlyElementsIn(
        expected.getHymnanlNetJsonList());
    assertThat(hymnalNetPipeline.getErrors()).containsExactlyElementsIn(expected.getErrorsList());

    ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HymnalNet> proto = ArgumentCaptor.forClass(HymnalNet.class);
    verify(MOCK_FILE_WRITER).writeProto(fileName.capture(), proto.capture());
    assertThat(fileName.getValue()).isEqualTo("storage/hymnalnet/1993-07-17_10-10-00_PDT.txt");
    assertThat(proto.getValue()).ignoringRepeatedFieldOrder().isEqualTo(expected);
  }
}
