package com.hymnsmobile.pipeline.hymnalnet;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineTestComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineTestModule;
import com.hymnsmobile.pipeline.hymnalnet.models.*;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
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
   * Song has a fully formed language and relevant graph, with 2 caveats:
   * <p>
   * 1) h/339b is a relevant song of h/339 and has a language pointer to ch/260. No other song in
   * the graph has a language pointer back to it, making it a dangling reference.
   * 2) de/1 points to h/339, but no other song in the graph has a language pointer back to it,
   * making it a dangling reference. However, since it is a German song, there is a special case
   * where we infer the name from the type, making it not a dangling reference.
   */
  // @Test TODO move to sanitization
  @FetchHymns(keysToFetch = {"h/339", "de/1"})
  public void songWithLanguagesAndTunes() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getHymnalNetJsons().stream()
        .map(hymnalNetJson -> hymnalNetJson.getKey().toString())
        .collect(toImmutableList())).containsExactly(
        "hymn_type: \"cb\"\nhymn_number: \"339\"\n",
        "hymn_type: \"h\"\nhymn_number: \"339b\"\n",
        "hymn_type: \"ch\"\nhymn_number: \"260\"\n",
        "hymn_type: \"h\"\nhymn_number: \"339\"\n",
        "hymn_type: \"ht\"\nhymn_number: \"339\"\n",
        "hymn_type: \"ch\"\nhymn_number: \"260\"\nquery_params: \"?gb=1\"\n",
        "hymn_type: \"nt\"\nhymn_number: \"339\"\n",
        "hymn_type: \"de\"\nhymn_number: \"1\"\n");
    assertThat(hymnalNetPipeline.getErrors()).isEmpty();

//    TODO test in sanitization pipeline
//    assertThat(hymnalNetPipeline.getErrors().get(0).getSeverity()).isEqualTo(Severity.ERROR);
//    assertThat(hymnalNetPipeline.getErrors())
//        .containsExactly(PipelineError.newBuilder()
//            .addMessages(
//                "Dangling reference: type: CLASSIC_HYMN\n"
//                    + "number: \"339b\"\n"
//                    + " in [name: \"\\350\\257\\227\\346\\255\\214(\\347\\256\\200)\"\n"
//                    + "reference {\n"
//                    + "  type: CHINESE_SIMPLIFIED\n"
//                    + "  number: \"260\"\n"
//                    + "}\n"
//                    + ", name: \"Cebuano\"\n"
//                    + "reference {\n"
//                    + "  type: CEBUANO\n"
//                    + "  number: \"339\"\n"
//                    + "}\n"
//                    + ", name: \"\\350\\251\\251\\346\\255\\214(\\347\\271\\201)\"\n"
//                    + "reference {\n"
//                    + "  type: CHINESE\n"
//                    + "  number: \"260\"\n"
//                    + "}\n"
//                    + ", name: \"English\"\n"
//                    + "reference {\n"
//                    + "  type: CLASSIC_HYMN\n"
//                    + "  number: \"339\"\n"
//                    + "}\n"
//                    + ", name: \"Tagalog\"\n"
//                    + "reference {\n"
//                    + "  type: TAGALOG\n"
//                    + "  number: \"339\"\n"
//                    + "}\n"
//                    + "]")
//            .setSeverity(Severity.ERROR).build());
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

  /**
   * ns/10 references ns/10a, which references it back. Typically, this should show an error saying
   * there are too many references, but we exempt songs like "ns/10a" (with a letter suffixed), so
   * this should not throw any errors.
   */
  // @Test TODO move to sanitization
  @FetchHymns(keysToFetch = {"ns/10", "ns/10a"})
  public void songHasLetterSuffixedReference_doNotAddError() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getErrors()).isEmpty();
  }

  /**
   * h/20 references h/11, which references it back. This should add an error because now the
   * language set has 2 Classical hymns, which is not allowed.
   */
  // @Test TODO move to sanitization
  @FetchHymns(keysToFetch = {"h/20", "h/21"})
  public void songHasTooManyReferences_addError() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getErrors()).containsExactly(
        PipelineError.newBuilder().setSeverity(Severity.ERROR)
            .addMessages("[type: CLASSIC_HYMN\n"
                + "number: \"20\"\n"
                + ", type: CLASSIC_HYMN\n"
                + "number: \"21\"\n"
                + "] has too many instances of CLASSIC_HYMN").build());
  }

  /**
   * h/30 references ns/30, which references it back. This should add an error because now the
   * language set has a classical hymn and a new song, which are not compatible.
   */
  // @Test TODO move to sanitization
  @FetchHymns(keysToFetch = {"h/30", "ns/31"})
  public void songHasIncompatibleTypes_addError() throws IOException {
    hymnalNetPipeline.run();

    assertThat(hymnalNetPipeline.getErrors()).containsExactly(
        PipelineError.newBuilder().setSeverity(Severity.ERROR)
            .addMessages("Incompatible languages types: [type: CLASSIC_HYMN\n"
                + "number: \"30\"\n"
                + ", type: NEW_SONG\n"
                + "number: \"30\"\n"
                + "]").build());
  }

  /**
   * h/40 references h/41, which references it back. This should add an error because now the
   * language set has 2 Classical hymns, but it is patched by the fake {@link Patcher} in
   * {@link HymnalNetPipelineTestModule}, so no error should be added.
   */
  // @Test TODO move to sanitization
  @FetchHymns(keysToFetch = {"h/40", "h/41"})
  public void songHasTooManyReferencesButIsPatched_doNotAddError() throws IOException {
    hymnalNetPipeline.run();
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
