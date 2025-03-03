package com.hymnsmobile.pipeline.storage;

import com.hymnsmobile.pipeline.models.*;
import com.hymnsmobile.pipeline.storage.dagger.StorageScope;
import com.hymnsmobile.pipeline.storage.models.Language;
import com.hymnsmobile.pipeline.storage.models.VerseType;
import com.hymnsmobile.pipeline.storage.models.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.hymnsmobile.pipeline.storage.models.HymnType.*;
import static com.hymnsmobile.pipeline.storage.models.Language.*;
import static com.hymnsmobile.pipeline.storage.models.VerseType.*;

@StorageScope
public class Converter {

  @Inject
  public Converter() {
  }

  public HymnEntity convert(Hymn hymn) {
    return HymnEntity.newBuilder()
        .setId(hymn.getId())
        .addAllReferences(hymn.getReferencesList().stream().map(this::convert).toList())
        .setTitle(hymn.getTitle())
        .setLyrics(convertLyrics(hymn.getVersesList()))
        .addAllCategory(hymn.getCategoryList())
        .addAllSubcategory(hymn.getSubCategoryList())
        .addAllAuthor(hymn.getAuthorList())
        .addAllComposer(hymn.getComposerList())
        .addAllKey(hymn.getKeyList())
        .addAllTime(hymn.getTimeList())
        .addAllMeter(hymn.getMeterList())
        .addAllScriptures(hymn.getScripturesList())
        .addAllHymnCode(hymn.getHymnCodeList())
        .setMusic(convertMusic(hymn.getMusicMap()))
        .setSvgSheet(convertSvgSheet(hymn.getSvgSheetMap()))
        .setPdfSheet(convertPdfSheet(hymn.getPdfSheetMap()))
        .setLanguages(convertLanguages(hymn.getLanguagesList()))
        .setRelevants(convertRelevants(hymn.getRelevantsList()))
        .setInlineChords(convertChords(hymn.getChordLinesList()))
        .addAllProvenance(hymn.getProvenanceList())
        .setFlattenedLyrics(hymn.getFlattenedLyrics())
        .setLanguage(convert(hymn.getLanguage()))
        .build();
  }

  private HymnIdentifierEntity convert(SongReference songReference) {
    return HymnIdentifierEntity.newBuilder()
        .setHymnType(convert(com.hymnsmobile.pipeline.merge.HymnType.fromString(songReference.getHymnType())))
        .setHymnNumber(songReference.getHymnNumber())
        .build();
  }

  private HymnType convert(com.hymnsmobile.pipeline.merge.HymnType hymnType) {
    return switch (hymnType) {
      case CLASSIC_HYMN -> CLASSIC;
      case NEW_TUNE -> NEW_TUNE;
      case NEW_SONG -> NEW_SONG;
      case CHILDREN_SONG -> CHILDREN;
      case HOWARD_HIGASHI -> HOWARD_HIGASHI;
      case DUTCH -> DUTCH;
      case GERMAN -> GERMAN;
      case CHINESE -> CHINESE;
      case CHINESE_SIMPLIFIED -> CHINESE_SIMPLIFIED;
      case CHINESE_SUPPLEMENTAL -> CHINESE_SUPPLEMENT;
      case CHINESE_SUPPLEMENTAL_SIMPLIFIED -> CHINESE_SUPPLEMENT_SIMPLIFIED;
      case CEBUANO -> CEBUANO;
      case TAGALOG -> TAGALOG;
      case FRENCH -> FRENCH;
      case SPANISH -> SPANISH;
      case HINOS -> HINOS;
      case HEBREW -> HEBREW;
      case INDONESIAN -> INDONESIAN;
      case KOREAN -> KOREAN;
      case FARSI -> FARSI;
      case RUSSIAN -> RUSSIAN;
      case JAPANESE -> JAPANESE;
      case ARABIC -> ARABIC;
      case ESTONIAN -> ESTONIAN;
      case PORTUGUESE -> PORTUGUESE;
      case BE_FILLED -> BE_FILLED;
      case LIEDERBUCH -> LIEDERBUCH;
      case SLOVAK -> SLOVAK;
      case BLUE_SONGBOOK -> BLUE_SONGBOOK;
      case LIEDBOEK -> LIEDBOEK;
      case SONGBASE_OTHER -> SONGBASE_OTHER;
    };
  }

  private LyricsEntity convertLyrics(List<Verse> verses) {
    return LyricsEntity.newBuilder()
        .addAllVerses(verses.stream().map(this::convert).toList())
        .build();
  }

  private VerseEntity convert(Verse verse) {
    return VerseEntity.newBuilder()
        .setVerseType(convert(verse.getVerseType()))
        .addAllLines(verse.getLinesList().stream().map(this::convert).toList())
        .build();
  }

  private VerseType convert(com.hymnsmobile.pipeline.models.VerseType verseType) {
    return switch (verseType) {
      case VERSE -> VERSE;
      case CHORUS -> CHORUS;
      case OTHER -> OTHER;
      case COPYRIGHT -> COPYRIGHT;
      case NOTE -> NOTE;
      case DO_NOT_DISPLAY -> DO_NOT_DISPLAY;
      case UNRECOGNIZED -> VerseType.UNRECOGNIZED;
    };
  }

  private LineEntity convert(Line line) {
    LineEntity.Builder builder = LineEntity.newBuilder().setLineContent(line.getLineContent());
    if (line.hasTransliteration() && !line.getTransliteration().isBlank()) {
      builder.setTransliteration(line.getTransliteration());
    }
    return builder.build();
  }

  private MusicEntity convertMusic(Map<String, String> music) {
    return MusicEntity.newBuilder()
        .putAllMusic(music)
        .build();
  }

  private SvgSheetEntity convertSvgSheet(Map<String, String> svgSheet) {
    return SvgSheetEntity.newBuilder()
        .putAllSvgSheet(svgSheet)
        .build();
  }

  private PdfSheetEntity convertPdfSheet(Map<String, String> pdfSheet) {
    return PdfSheetEntity.newBuilder()
        .putAllPdfSheet(pdfSheet)
        .build();
  }

  private LanguagesEntity convertLanguages(List<SongReference> languages) {
    return LanguagesEntity.newBuilder()
        .addAllLanguages(languages.stream().map(this::convert).toList())
        .build();
  }

  private RelevantsEntity convertRelevants(List<SongReference> relevants) {
    return RelevantsEntity.newBuilder()
        .addAllRelevants(relevants.stream().map(this::convert).toList())
        .build();
  }

  private InlineChordsEntity convertChords(List<ChordLine> chordLines) {
    return InlineChordsEntity.newBuilder()
        .addAllChordLines(chordLines.stream().map(this::convert).toList())
        .build();
  }

  private ChordLineEntity convert(ChordLine chordLine) {
    return ChordLineEntity.newBuilder()
        .addAllChordWords(chordLine.getChordWordsList().stream().map(this::convert).toList())
        .build();
  }

  private ChordWordEntity convert(ChordWord chordWord) {
    ChordWordEntity.Builder builder =
        ChordWordEntity.newBuilder()
            .setWord(chordWord.getWord());
    if (chordWord.hasChords()) {
      builder.setChords(chordWord.getChords());
    }
    return builder.build();
  }

  private Language convert(com.hymnsmobile.pipeline.models.Language language) {
    return switch (language) {
      case ENGLISH -> LANGUAGE_ENGLISH;
      case DUTCH -> LANGUAGE_DUTCH;
      case GERMAN -> LANGUAGE_GERMAN;
      case CHINESE_TRADITIONAL -> LANGUAGE_CHINESE_TRADITIONAL;
      case CHINESE_SIMPLIFIED -> LANGUAGE_CHINESE_SIMPLIFIED;
      case CEBUANO -> LANGUAGE_CEBUANO;
      case TAGALOG -> LANGUAGE_TAGALOG;
      case FRENCH -> LANGUAGE_FRENCH;
      case SPANISH -> LANGUAGE_SPANISH;
      case KOREAN -> LANGUAGE_KOREAN;
      case JAPANESE -> LANGUAGE_JAPANESE;
      case FARSI -> LANGUAGE_FARSI;
      case RUSSIAN -> LANGUAGE_RUSSIAN;
      case PORTUGUESE -> LANGUAGE_PORTUGUESE;
      case HEBREW -> LANGUAGE_HEBREW;
      case SLOVAK -> LANGUAGE_SLOVAK;
      case ESTONIAN -> LANGUAGE_ESTONIAN;
      case ARABIC -> LANGUAGE_ARABIC;
      case INDONESIAN -> LANGUAGE_INDONESIAN;
      case UNRECOGNIZED -> Language.UNRECOGNIZED;
    };
  }
}
