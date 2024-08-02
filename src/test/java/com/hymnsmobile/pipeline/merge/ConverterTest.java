package com.hymnsmobile.pipeline.merge;

import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.russian.RussianHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

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
  public void toSongReference__fromHymnalNetKey_songNumberEndsWithC__correctlyConvertsToChineseSong() {
    assertThat(target.toSongReference(HymnalNetKey.newBuilder().setHymnType("h").setHymnNumber("12c").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("ch").setHymnNumber("h12c").build());
    assertThat(errors).isEmpty();
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
  public void toSongReference__fromH4aKey_howardHigashi__correctlyConverts() {
    assertThat(target.toSongReference(H4aKey.newBuilder().setType("NS").setNumber("1001").build()))
        .isEqualTo(SongReference.newBuilder().setHymnType("lb").setHymnNumber("1").build());
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
        PipelineError
            .newBuilder()
            .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
            .addMessages("MetaDatum name not found for hymn_type: \"c\"\nhymn_number: \"60\"\n: name: \"unrecognized meta datum\"\ndata {\n  value: \"value\"\n  path: \"https://www.hymnal.net/path\"\n}\n")
            .build(),
        PipelineError
            .newBuilder()
            .setErrorType(PipelineError.ErrorType.PARSE_ERROR)
            .addMessages("/en/hymn/hf/, a related song of hymn_type: \"c\"\nhymn_number: \"60\"\n")
            .build(),
        PipelineError
            .newBuilder()
            .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .addMessages("/en/hymn/12, a related song of hymn_type: \"c\"\nhymn_number: \"60\"\n")
            .build(),
        PipelineError
            .newBuilder()
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
}
