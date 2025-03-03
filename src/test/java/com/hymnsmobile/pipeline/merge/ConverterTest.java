package com.hymnsmobile.pipeline.merge;

import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import com.hymnsmobile.pipeline.models.*;
import com.hymnsmobile.pipeline.russian.RussianHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterTest {

  private Set<PipelineError> errors;
  private Converter target;

  @BeforeEach
  public void setUp() {
    this.errors = new HashSet<>();
    this.target = new Converter(errors);

    Converter.nextHymnId = 1;
  }

  @Test
  public void toSongReference__fromHymnalNetKey_simpleKey__correctlyConverts() {
    assertThat(target.toSongReference(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("h").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_songNumberEndsWithA__keepsHymnTypeAndNumber() {
    assertThat(target.toSongReference(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12a").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("h").setHymnNumber("12a").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_songNumberEndsWithC__correctlyConvertsToChineseSong() {
    assertThat(target.toSongReference(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12c").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("ch").setHymnNumber("h12c").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_songNumberEndsWithUnrecognized__addsError() {
    assertThat(target.toSongReference(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12abc").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("h").setHymnNumber("12abc").build());
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.WARNING)
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .addMessages("12abc")
            .build());
  }

  @Test
  public void toSongReference__fromHymnalNetKey_chinese_gb1QueryParam__correctlyConvertsToChineseSimplified() {
    assertThat(target.toSongReference(
        HymnalNetKey.newBuilder().setHymnType("ch").setHymnNumber("12").setQueryParams("?gb=1").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("chx").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_chineseSupplement_gb1QueryParam__correctlyConvertsToChineseSupplementSimplified() {
    assertThat(target.toSongReference(
        HymnalNetKey.newBuilder().setHymnType("ts").setHymnNumber("12").setQueryParams("?gb=1").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("tsx").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_unrecognizedHymnType__throwsError() {
    assertThatThrownBy(
        () -> target.toSongReference(
            HymnalNetKey.newBuilder().setHymnType("unrecognized").setHymnNumber("12").build()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromHymnalNetKey_unrecognizedQueryParam__throwsError() {
    assertThatThrownBy(
        () -> target.toSongReference(
            HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12").setQueryParams("unrecognized").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unexpected query param found");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromH4aKey_simpleKey__correctlyConverts() {
    assertThat(target.toSongReference(H4aKey.newBuilder().setType("E").setNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("h").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromH4aKey_german__correctlyConverts() {
    assertThat(target.toSongReference(H4aKey.newBuilder().setType("G").setNumber("1001").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("lde").setHymnNumber("1001").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromH4aKey_unrecognizedHymnType__throwsError() {
    assertThatThrownBy(
        () -> target.toSongReference(
            H4aKey.newBuilder().setType("unrecognized").setNumber("12").build()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromLiederbuchKey_simpleKey__correctlyConverts() {
    assertThat(target.toSongReference(LiederbuchKey.newBuilder().setType("G").setNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("lde").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromLiederbuchKey_howardHiagshi__correctlyConverts() {
    assertThat(target.toSongReference(LiederbuchKey.newBuilder().setType("NS").setNumber("1012").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("lb").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromLiederbuchKey_unrecognizedHymnType__throwsError() {
    assertThatThrownBy(
        () -> target.toSongReference(
            LiederbuchKey.newBuilder().setType("unrecognized").setNumber("12").build()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_english__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("english_hymnal").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("h").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_blueSongbook__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("blue_songbook").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("sb").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_spanish__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("spanish_hymnal").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("S").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_german__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("german_hymnal").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("lde").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_french__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("french_hymnal").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("hf").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_songbaseOther__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("songbase_other").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("sbx").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_portuguese__correctlyConverts() {
    assertThat(target.toSongReference(SongbaseKey.newBuilder().setHymnType("hinos").setHymnNumber("12").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("pt").setHymnNumber("12").build());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toSongReference__fromSongbaseKey_unrecognizedHymnType__throwsError() {
    assertThatThrownBy(
        () -> target.toSongReference(
            SongbaseKey.newBuilder().setHymnType("unrecognized").setHymnNumber("12").build()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromHymnalNetJson__standardSong__correctlyConverts() throws IOException {
    HymnalNetJson input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/c60_hymnalnet.textproto",
            HymnalNetJson.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/c60.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
            .addMessages("MetaDatum name not found for hymn_type: \"c\"\nhymn_number: \"60\"\n: name: \"unrecognized meta datum\"\ndata {\n  value: \"value\"\n  path: \"https://www.hymnal.net/path\"\n}\n")
            .build(),
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
            .addMessages("/en/hymn/hf/, a related song of hymn_type: \"c\"\nhymn_number: \"60\"\n")
            .build(),
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .addMessages("/en/hymn/12, a related song of hymn_type: \"c\"\nhymn_number: \"60\"\n")
            .build(),
        PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .addMessages("/en/hymn/unrecognized/12, a related song of hymn_type: \"c\"\nhymn_number: \"60\"\n")
            .build());
  }

  @Test
  public void toHymn__fromHymnalNetJson__includesTransliteration__correctlyConverts() throws IOException {
    HymnalNetJson input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/ch1_hymnalnet.textproto",
            HymnalNetJson.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/ch1.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromHymnalNetJson__includesTransliteration_lineCountMismatch__throwsException() throws IOException {
    HymnalNetJson input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/ch1_hymnalnet_transliterationMismatch.textproto",
            HymnalNetJson.newBuilder());

    assertThatThrownBy(() -> target.toHymn(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("hymn_type: \"ch\"\nhymn_number: \"1\"\n has 2 transliteration lines and 4 verse lines");
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromH4a__standardSong__correctlyConverts() throws IOException {
    H4aHymn input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/E1_h4a.textproto",
            H4aHymn.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/h1.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromH4a__emptyFirstLine__throwsException() throws IOException {
    H4aHymn input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/E1_h4a_missingFirstLine.textproto",
            H4aHymn.newBuilder());

    assertThatThrownBy(() -> target.toHymn(input))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to convert to hymn: " + input.toString());
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromRussian__standardSong__correctlyConverts() throws IOException {
    RussianHymn input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/E2_russian.textproto",
            RussianHymn.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/h2.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromSongbase__standardSong__correctlyConverts() throws IOException {
    SongbaseHymn input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/songbase1.textproto",
            SongbaseHymn.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/sb1.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).isEmpty();
  }

  @Test
  public void toHymn__fromSongbase__mismatchedReferenceLanguages__addsError() throws IOException {
    SongbaseHymn input =
        TestUtils.readTextProto(
            "src/test/resources/merge/input/songbase_mismatch.textproto",
            SongbaseHymn.newBuilder());
    Hymn actual = target.toHymn(input);

    Hymn expected =
        TestUtils.readTextProto(
            "src/test/resources/merge/output/sb_mismatch.textproto",
            Hymn.newBuilder());
    assertThat(actual).isEqualTo(expected);
    assertThat(errors).containsExactly(
        PipelineError.newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.DUPLICATE_LANGUAGE_MISMATCH)
            .setSource(PipelineError.Source.SONGBASE)
            .addMessages("[hymn_type: \"sb\"\nhymn_number: \"1\"\n, hymn_type: \"sbx\"\nhymn_number: \"3\"\n]")
            .build());
  }


  @Test
  public void createInlineChords__emptyString__shouldBeConvertedToEmptyList() {
    assertThat(target.createInlineChords("")).isEmpty();
    assertThat(errors).containsExactly(
        PipelineError
            .newBuilder()
            .setSeverity(PipelineError.Severity.ERROR)
            .setErrorType(PipelineError.ErrorType.INLINE_CHORDS_EMPTY)
            .setSource(PipelineError.Source.SONGBASE)
            .build());
  }

  @Test
  public void createInlineChords__noChordFound__shouldBeConvertedToChordWordsWithNullChords() {
    assertThat(target.createInlineChords("With Christ in my vessel I will"))
        .isEqualTo(List.of(
            ChordLine.newBuilder()
                     .addChordWords(ChordWord.newBuilder().setWord("With"))
                     .addChordWords(ChordWord.newBuilder().setWord("Christ"))
                     .addChordWords(ChordWord.newBuilder().setWord("in"))
                     .addChordWords(ChordWord.newBuilder().setWord("my"))
                     .addChordWords(ChordWord.newBuilder().setWord("vessel"))
                     .addChordWords(ChordWord.newBuilder().setWord("I"))
                     .addChordWords(ChordWord.newBuilder().setWord("will"))
                     .build()));
    assertThat(errors).isEmpty();
  }

  @Test
  public void createInlineChords__emptyChordsFound__shouldExtractTheWordsOutWithEmptyChords() {
    assertThat(target.createInlineChords("[]With Christ in my vessel I will"))
        .isEqualTo(List.of(
            ChordLine.newBuilder()
                     .addChordWords(ChordWord.newBuilder().setWord("With").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("Christ").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("in").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("my").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("vessel").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("I").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("will").setChords(""))
                     .build()));
    assertThat(errors).isEmpty();
  }

  @Test
  public void createInlineChords__longChordsInMiddleOfWord__shouldParseOutChords() {
    assertThat(target.createInlineChords("  [G]Exercise your [A]spirit in this w[D  G-D  G-D  G-D]ay."))
        .isEqualTo(List.of(
            ChordLine.newBuilder()
                     .addChordWords(ChordWord.newBuilder().setWord("Exercise").setChords("G"))
                     .addChordWords(ChordWord.newBuilder().setWord("your").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("spirit").setChords("A"))
                     .addChordWords(ChordWord.newBuilder().setWord("in").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("this").setChords(""))
                     .addChordWords(ChordWord.newBuilder().setWord("way.").setChords(" D  G-D  G-D  G-D"))
                     .build()));
    assertThat(errors).isEmpty();
  }

  @Test
  public void createInlineChords__chordsFound__shouldExtractTheChordsOutIntoChordWords() {
    assertThat(target.createInlineChords("1\n" +
                                             "[C]Loving You Lord’s [G]all I’m " +
                                             "living [Am - C]for.\n" +
                                             "[F]Loving You Lord [G]to the " +
                                             "utter[C]most.\n" +
                                             "[G]Lord, forgive me[Am] if I’ve " +
                                             "left my first [F]love," +
                                             "\n" +
                                             "That’s the [Dm]bridal love for " +
                                             "[G]You.\n" +
                                             "Brideg[G]room.\n" +
                                             "\n" +
                                             "  []I give You the first place\n" +
                                             "  Infuse [F]me with [G]Yourself " +
                                             "[Am]abundant[F]ly\n" +
                                             "  Till we [C]meet, dear [Am - " +
                                             "F]Lord, [G]\n" +
                                             "\n" +
                                             "2\n" +
                                             "You’ve the right to take all that" +
                                             " I love,\n" +
                                             "Loving one another’s sweet."))
        .isEqualTo(List.of(
            ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord("1")).build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Loving").setChords("C"))
                .addChordWords(ChordWord.newBuilder().setWord("You").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("Lord’s").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("all").setChords("G"))
                .addChordWords(ChordWord.newBuilder().setWord("I’m").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("living").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("for.").setChords("Am - C"))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Loving").setChords("F"))
                .addChordWords(ChordWord.newBuilder().setWord("You").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("Lord").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("to").setChords("G"))
                .addChordWords(ChordWord.newBuilder().setWord("the").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("uttermost.").setChords("     C"))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Lord,").setChords("G"))
                .addChordWords(ChordWord.newBuilder().setWord("forgive").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("me").setChords("  Am"))
                .addChordWords(ChordWord.newBuilder().setWord("if").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("I’ve").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("left").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("my").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("first").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("love,").setChords("F"))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("That’s").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("the").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("bridal").setChords("Dm"))
                .addChordWords(ChordWord.newBuilder().setWord("love").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("for").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("You.").setChords("G"))
                .build(),
            ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord("Bridegroom.").setChords("      G")).build(),
            ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord(" ")).build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("I").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("give").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("You").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("the").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("first").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("place").setChords(""))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Infuse").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("me").setChords("F"))
                .addChordWords(ChordWord.newBuilder().setWord("with").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("Yourself").setChords("G"))
                .addChordWords(ChordWord.newBuilder().setWord("abundantly").setChords("Am      F"))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Till").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("we").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("meet,").setChords("C"))
                .addChordWords(ChordWord.newBuilder().setWord("dear").setChords(""))
                .addChordWords(ChordWord.newBuilder().setWord("Lord,").setChords("Am - F"))
                .addChordWords(ChordWord.newBuilder().setWord("").setChords("G"))
                .build(),
            ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord(" ")).build(),
            ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord("2")).build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("You’ve"))
                .addChordWords(ChordWord.newBuilder().setWord("the"))
                .addChordWords(ChordWord.newBuilder().setWord("right"))
                .addChordWords(ChordWord.newBuilder().setWord("to"))
                .addChordWords(ChordWord.newBuilder().setWord("take"))
                .addChordWords(ChordWord.newBuilder().setWord("all"))
                .addChordWords(ChordWord.newBuilder().setWord("that"))
                .addChordWords(ChordWord.newBuilder().setWord("I"))
                .addChordWords(ChordWord.newBuilder().setWord("love,"))
                .build(),
            ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("Loving"))
                .addChordWords(ChordWord.newBuilder().setWord("one"))
                .addChordWords(ChordWord.newBuilder().setWord("another’s"))
                .addChordWords(ChordWord.newBuilder().setWord("sweet."))
                .build()
        ));
    assertThat(errors).isEmpty();
  }
}
