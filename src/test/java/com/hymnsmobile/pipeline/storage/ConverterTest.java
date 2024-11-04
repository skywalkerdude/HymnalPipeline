package com.hymnsmobile.pipeline.storage;

import com.hymnsmobile.pipeline.models.*;
import com.hymnsmobile.pipeline.storage.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.hymnsmobile.pipeline.storage.models.LanguageEntity.LANGUAGE_FARSI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterTest {

  private Converter target;

  @BeforeEach
  public void setUp() {
    this.target = new Converter();
  }

  @Test
  public void convertHymn() {
    Hymn hymn = Hymn.newBuilder()
        .setId(1)
        .addReferences(SongReference.newBuilder().setHymnType("h").setHymnNumber("2"))
        .addReferences(SongReference.newBuilder().setHymnType("ch").setHymnNumber("3"))
        .setTitle("title")
        .addVerses(Verse.newBuilder()
                .setVerseType(VerseType.VERSE)
                .addLines(Line.newBuilder().setLineContent("no transliteration"))
                .addLines(Line.newBuilder().setLineContent("empty transliteration").setTransliteration(""))
                .addLines(Line.newBuilder().setLineContent("blank transliteration").setTransliteration(" "))
                .addLines(Line.newBuilder().setLineContent("transliteration").setTransliteration("transliteration")))
        .addVerses(Verse.newBuilder()
                .setVerseType(VerseType.CHORUS)
                .addLines(Line.newBuilder().setLineContent("chorus line 1"))
                .addLines(Line.newBuilder().setLineContent("chorus line 2"))
                .addLines(Line.newBuilder().setLineContent("chorus line 3"))
                .addLines(Line.newBuilder().setLineContent("chorus line 4")))
        .addCategory("category 1")
        .addCategory("category 1") // duplicate category
        .addCategory("category 2")
        .addSubCategory("subcategory 1")
        .addSubCategory("subcategory 2")
        .addAuthor("author 1")
        .addComposer("composer 1")
        .addKey("key 1")
        .addTime("time 1")
        .addMeter("meter 1")
        .addScriptures("scripture 1")
        .addHymnCode("hymn code 1")
        .putMusic("music key 1", "music value 1")
        .putMusic("music key 1", "music value 3") // duplicate key
        .putSvgSheet("svg key 2", "svg value 2")
        .putSvgSheet("svg key 3", "svg value 2") // duplicate  value
        .putPdfSheet("pdf key 1", "pdf value 1")
        .addLanguages(SongReference.newBuilder().setHymnType("ns").setHymnNumber("2"))
        .addRelevants(SongReference.newBuilder().setHymnType("nt").setHymnNumber("2"))
        .addChordLines(ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("no chords"))
                .addChordWords(ChordWord.newBuilder().setWord("empty chord").setChords("")))
        .addChordLines(ChordLine.newBuilder()
                .addChordWords(ChordWord.newBuilder().setWord("blank chord").setChords(" ")))
        .addChordLines(ChordLine.newBuilder().addChordWords(ChordWord.newBuilder().setWord("chord").setChords(" C ")))
        .addProvenance("provenance")
        .setFlattenedLyrics("flattened lyrics")
        .setLanguage(Language.FARSI)
        .build();
    HymnEntity hymnEntity = HymnEntity
        .newBuilder()
        .setId(1)
        .addReferences(HymnIdentifierEntity.newBuilder().setHymnType(HymnTypeEntity.CLASSIC_HYMN).setHymnNumber("2"))
        .addReferences(HymnIdentifierEntity.newBuilder().setHymnType(HymnTypeEntity.CHINESE).setHymnNumber("3"))
        .setTitle("title")
        .setLyrics(LyricsEntity.newBuilder()
                .addVerses(
                    VerseEntity
                        .newBuilder()
                        .setVerseType(VerseType.VERSE)
                        .addLines(LineEntity.newBuilder().setLineContent("no transliteration"))
                        .addLines(LineEntity.newBuilder().setLineContent("empty transliteration"))
                        .addLines(LineEntity.newBuilder().setLineContent("blank transliteration"))
                        .addLines(LineEntity.newBuilder().setLineContent("transliteration").setTransliteration("transliteration")))
                .addVerses(VerseEntity.newBuilder()
                        .setVerseType(VerseType.CHORUS)
                        .addLines(LineEntity.newBuilder().setLineContent("chorus line 1"))
                        .addLines(LineEntity.newBuilder().setLineContent("chorus line 2"))
                        .addLines(LineEntity.newBuilder().setLineContent("chorus line 3"))
                        .addLines(LineEntity.newBuilder().setLineContent("chorus line 4"))))
        .addCategory("category 1")
        .addCategory("category 1") // duplicate category
        .addCategory("category 2")
        .addSubCategory("subcategory 1")
        .addSubCategory("subcategory 2")
        .addAuthor("author 1")
        .addComposer("composer 1")
        .addKey("key 1")
        .addTime("time 1")
        .addMeter("meter 1")
        .addScriptures("scripture 1")
        .addHymnCode("hymn code 1")
        .setMusic(MusicEntity.newBuilder().putMusic("music key 1", "music value 3"))
        .setSvgSheet(SvgSheetEntity.newBuilder()
                .putSvgSheet("svg key 2", "svg value 2")
                .putSvgSheet("svg key 3", "svg value 2"))
        .setPdfSheet(PdfSheetEntity.newBuilder().putPdfSheet("pdf key 1", "pdf value 1"))
        .setLanguages(LanguagesEntity.newBuilder()
                .addLanguages(HymnIdentifierEntity.newBuilder().setHymnType(HymnTypeEntity.NEW_SONG).setHymnNumber("2")))
        .setRelevants(RelevantsEntity.newBuilder()
                .addRelevants(HymnIdentifierEntity.newBuilder().setHymnType(HymnTypeEntity.NEW_TUNE).setHymnNumber("2")))
        .setInlineChords(InlineChordsEntity.newBuilder()
                .addChordLines(ChordLineEntity.newBuilder()
                        .addChordWords(ChordWordEntity.newBuilder().setWord("no chords"))
                        .addChordWords(ChordWordEntity.newBuilder().setWord("empty chord")))
                .addChordLines(ChordLineEntity.newBuilder()
                        .addChordWords(ChordWordEntity.newBuilder().setWord("blank chord")))
                .addChordLines(ChordLineEntity.newBuilder()
                        .addChordWords(ChordWordEntity.newBuilder().setWord("chord").setChords(" C "))))
        .addProvenance("provenance")
        .setFlattenedLyrics("flattened lyrics")
        .setLanguage(LANGUAGE_FARSI)
        .build();
    assertThat(target.convert(hymn)).isEqualTo(hymnEntity);
  }

  @Test
  public void convertHymn__unrecognizedHymnType__throwsError() {
    Hymn unrecognizedHymnType =
        Hymn.newBuilder()
            .addReferences(SongReference.newBuilder().setHymnType("unrecognized").setHymnNumber("1"))
            .build();

    assertThatThrownBy(
        () -> target.convert(unrecognizedHymnType))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Hymn type not found: unrecognized");
  }
}
