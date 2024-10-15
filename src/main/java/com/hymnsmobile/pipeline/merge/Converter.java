package com.hymnsmobile.pipeline.merge;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.MapEntry;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.hymnalnet.MetaDatumType;
import com.hymnsmobile.pipeline.hymnalnet.models.Datum;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.*;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.russian.RussianHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.utils.TextUtil;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.HymnType.*;

@MergeScope
public class Converter {

  public static final String CHORDS_PATTERN = "\\[(.*?)]";

  // Separates chord line out into words.
  // Note: ?: represents a non-matching group. i.e. the regex matches, but the range isn't extracted.
  private static final String SEPARATOR_PATTERN = "(\\S*(?:\\[.*?])\\S*|\\S+)";

  @VisibleForTesting static int nextHymnId = 1;

  private static final Logger LOGGER = Logger.getGlobal();

  private final Set<PipelineError> errors;

  @Inject
  public Converter(@Merge Set<PipelineError> errors) {
    this.errors = errors;
  }

  public SongReference toSongReference(HymnalNetKey key) {
    com.hymnsmobile.pipeline.hymnalnet.HymnType hymnType =
        com.hymnsmobile.pipeline.hymnalnet.HymnType.fromString(key.getHymnType()).orElseThrow();
    String hymnNumber = key.getHymnNumber();
    Optional<String> queryParamsOptional =
        key.hasQueryParams() ? Optional.of(key.getQueryParams()) : Optional.empty();

    // Songs that end with a letter (except 'a' and 'b') are usually in a different language, so we need to change their
    // types. 'a' and 'b' usually denotes alternate tunes, which are still in the same language (usually English).
    if (hymnNumber.matches("\\d+\\D+") &&
        !hymnNumber.matches("\\d+a") &&
        !hymnNumber.matches("\\d+b")) {
      Optional<com.hymnsmobile.pipeline.hymnalnet.HymnType> inferredType = inferHymnTypeFromNumber(hymnNumber);
      if (inferredType.isPresent()) {
        hymnNumber = hymnType.abbreviation + hymnNumber;
        hymnType = inferredType.get();
      }
    }

    SongReference.Builder builder = SongReference.newBuilder().setHymnNumber(hymnNumber);
    if (queryParamsOptional.isPresent()) {
      String queryParams = queryParamsOptional.get();
      if (queryParams.equals("?gb=1")) {
        if (hymnType == com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE) {
          return builder.setHymnType(HymnType.CHINESE_SIMPLIFIED.abbreviatedValue)
              .build();
        }

        if (hymnType == com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE_SUPPLEMENTAL) {
          return builder.setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue).build();
        }
      }
      throw new IllegalArgumentException("Unexpected query param found");
    }
    return builder.setHymnType(HymnType.valueOf(hymnType.name()).abbreviatedValue)
        .build();
  }

  private Optional<com.hymnsmobile.pipeline.hymnalnet.HymnType> inferHymnTypeFromNumber(String hymnNumber) {
    // Songs that end with "ar" are actually Arabic songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381ar
    if (hymnNumber.matches("\\d+ar")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.ARABIC);
    }

    // Songs that end with "cb" are actually Cebuano songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381cb
    if (hymnNumber.matches("\\d+cb")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.CEBUANO);
    }

    // Songs that end with "es" are actually Estonian songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381es
    if (hymnNumber.matches("\\d+es")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.ESTONIAN);
    }

    // Songs that end with "f" are actually French songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381f
    if (hymnNumber.matches("\\d+f")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.FRENCH);
    }

    // Songs that end with "p" are actually Portuguese songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381p
    if (hymnNumber.matches("\\d+p")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.HINOS);
    }

    // Songs that end with "htc" are actually Tagalog songs.
    // Example: https://www.hymnal.net/en/hymn/ns/381tc
    if (hymnNumber.matches("\\d+tc")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.TAGALOG);
    }

    // Songs that end with "t" are actually Tagalog songs.
    // Example: https://www.hymnal.net/en/hymn/ns/617t
    if (hymnNumber.matches("\\d+t")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.TAGALOG);
    }

    // Songs that end with "c" are actually Chinese songs.
    // Example: https://www.hymnal.net/en/hymn/ns/746c
    if (hymnNumber.matches("\\d+c")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE);
    }

    // Songs that end with "j" are actually Japanese songs.
    // Example: https://www.hymnal.net/en/hymn/ns/506j
    if (hymnNumber.matches("\\d+j")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.JAPANESE);
    }

    // Songs that end with "k" are actually Korean songs.
    // Example: https://www.hymnal.net/en/hymn/ns/506k
    if (hymnNumber.matches("\\d+k")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.KOREAN);
    }

    // Songs that end with "r" are actually Russian songs.
    // Example: https://www.hymnal.net/en/hymn/ns/506r
    if (hymnNumber.matches("\\d+r")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.RUSSIAN);
    }

    // Songs that end with "s" are actually Spanish songs.
    // Example: https://www.hymnal.net/en/hymn/ns/617s
    if (hymnNumber.matches("\\d+s")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.SPANISH);
    }

    // Songs that end with "ht" are actually Tagalog songs.
    // Example: https://www.hymnal.net/en/hymn/ns/151ht
    if (hymnNumber.matches("\\d+ht")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.TAGALOG);
    }

    // Songs that end with "de" are actually German songs.
    // Example: https://www.hymnal.net/en/hymn/ns/151de
    if (hymnNumber.matches("\\d+de")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.GERMAN);
    }

    // Songs that end with "i" are actually Indonesian songs.
    // Example: https://www.hymnal.net/en/hymn/ns/6i
    if (hymnNumber.matches("\\d+i")) {
      return Optional.of(com.hymnsmobile.pipeline.hymnalnet.HymnType.INDONESIAN);
    }

    errors.add(
        PipelineError.newBuilder()
            .setSeverity(Severity.WARNING)
            .setErrorType(ErrorType.UNRECOGNIZED_HYMN_TYPE)
            .setSource(PipelineError.Source.HYMNAL_NET)
            .addMessages(hymnNumber)
            .build());
    return Optional.empty();
  }

  public SongReference toSongReference(H4aKey key) {
    SongReference.Builder reference = SongReference.newBuilder();

    com.hymnsmobile.pipeline.h4a.HymnType type = com.hymnsmobile.pipeline.h4a.HymnType.fromString(
        key.getType()).orElseThrow();
    String number = key.getNumber();

    if (type == com.hymnsmobile.pipeline.h4a.HymnType.GERMAN) {
      return reference.setHymnType(LIEDERBUCH.abbreviatedValue).setHymnNumber(number).build();
    }
    return reference.setHymnType(HymnType.valueOf(type.name()).abbreviatedValue).setHymnNumber(number).build();
  }

  public SongReference toSongReference(LiederbuchKey key) {
    SongReference.Builder reference = SongReference.newBuilder().setHymnNumber(key.getNumber());

    com.hymnsmobile.pipeline.liederbuch.HymnType type =
        com.hymnsmobile.pipeline.liederbuch.HymnType.fromString(key.getType()).orElseThrow();

    if (type == com.hymnsmobile.pipeline.liederbuch.HymnType.GERMAN) {
      return reference.setHymnType(LIEDERBUCH.abbreviatedValue).build();
    }

    int numberInt = Integer.parseInt(key.getNumber());
    if (type == com.hymnsmobile.pipeline.liederbuch.HymnType.NEW_SONG && numberInt > 1000) {
      return reference.setHymnType(HOWARD_HIGASHI.abbreviatedValue).setHymnNumber(String.valueOf(numberInt - 1000)).build();
    }
    return reference.setHymnType(HymnType.valueOf(type.name()).abbreviatedValue).build();
  }

  public SongReference toSongReference(SongbaseKey key) {
    SongReference.Builder reference = SongReference.newBuilder().setHymnNumber(key.getHymnNumber());

    com.hymnsmobile.pipeline.songbase.HymnType type =
        com.hymnsmobile.pipeline.songbase.HymnType.fromString(key.getHymnType()).orElseThrow();

    final HymnType hymnType = switch (type) {
      case HYMNAL -> CLASSIC_HYMN;
      case BLUE_SONGBOOK -> BLUE_SONGBOOK;
      case HIMNOS -> SPANISH;
      case LIEDERBUCH -> LIEDERBUCH;
      case CANTIQUES -> FRENCH;
      case SONGBASE_OTHER -> SONGBASE_OTHER;
      case LIEDBOEK -> LIEDBOEK;
      case HINOS -> HINOS;
    };
    return reference.setHymnType(hymnType.abbreviatedValue).build();
  }

  public Language getLanguage(SongReference songReference) {
    return fromString(songReference.getHymnType()).language;
  }

  /**
   * Converts a {@link HymnalNetJson} to a {@link Hymn}.
   */
  public Hymn toHymn(HymnalNetJson hymn) {
    HymnalNetKey key = hymn.getKey();

    Hymn.Builder builder = Hymn.newBuilder()
        .setId(nextHymnId++)
        .addReferences(toSongReference(hymn.getKey()))
        .setTitle(hymn.getTitle())
        .setLanguage(getLanguage(toSongReference(key)))
        .addProvenance("hymnal.net");

    hymn.getLyricsList().forEach(verse -> builder.addLyrics(toVerse(key, verse)));

    hymn.getMetaDataList().forEach(metaDatum -> {
      String name = metaDatum.getName();
      // Ignore the "See Also" item because it changes every time you call it.
      // Also ignore the "Link" item because it links out to external links, which is not yet
      // supported. However, TODO we should support that eventually.
      if (name.equals("See Also") || name.equals("Link")) {
        return;
      }

      Optional<MetaDatumType> metaDatumType = MetaDatumType.fromJsonRepresentation(metaDatum);
      if (metaDatumType.isEmpty()) {
        errors.add(PipelineError.newBuilder()
            .setSource(PipelineError.Source.HYMNAL_NET)
            .setSeverity(Severity.ERROR)
            .setErrorType(ErrorType.PARSE_ERROR)
            .addMessages(String.format("MetaDatum name not found for %s: %s", key, metaDatum))
            .build());
        return;
      }

      FieldDescriptor field = Hymn.Builder.getDescriptor()
          .findFieldByName(metaDatumType.get().name().toLowerCase());
      if (metaDatumType.get() == MetaDatumType.LANGUAGES
          || metaDatumType.get() == MetaDatumType.RELEVANT) {
        metaDatum.getDataList().forEach(datum -> {
          Optional<HymnalNetKey> relatedKey = com.hymnsmobile.pipeline.hymnalnet.Converter.extractFromPath(
              datum.getPath(), hymn.getKey(), errors);
          if (relatedKey.isEmpty()) {
            // Errors added by the extractFromPath function if the related key was unparsable or unrecognized.
            return;
          }
          if (metaDatumType.get() == MetaDatumType.LANGUAGES) {
            builder.addLanguages(toSongReference(relatedKey.get()));
          }
          if (metaDatumType.get() == MetaDatumType.RELEVANT) {
            builder.addRelevants(toSongReference(relatedKey.get()));
          }
        });
      } else if (metaDatumType.get() == MetaDatumType.MUSIC
          || metaDatumType.get() == MetaDatumType.SVG_SHEET
          || metaDatumType.get() == MetaDatumType.PDF_SHEET) {

        metaDatum.getDataList().forEach(datum -> {
          String value = datum.getValue();
          String url = datum.getPath();

          Descriptor descriptor = field.getMessageType();
          MapEntry<String, String> entry = MapEntry.newDefaultInstance(descriptor,
              com.google.protobuf.WireFormat.FieldType.STRING, value,
              com.google.protobuf.WireFormat.FieldType.STRING, url);
          builder.addRepeatedField(field, entry);
        });
      } else {
        metaDatum.getDataList().stream().map(Datum::getValue)
            .forEach(value -> builder.addRepeatedField(field, value));
      }
    });

    builder.setFlattenedLyrics(flattenLyrics(builder.getLyricsList()));
    LOGGER.fine(String.format("%s successfully converted", key));
    return builder.build();
  }

  private com.hymnsmobile.pipeline.models.Verse toVerse(
      HymnalNetKey key, com.hymnsmobile.pipeline.hymnalnet.models.Verse verse) {
    com.hymnsmobile.pipeline.models.Verse.Builder verseBuilder = com.hymnsmobile.pipeline.models.Verse.newBuilder()
        .setVerseType(verse.getVerseType());

    for (int i = 0; i < verse.getVerseContentCount(); i++) {
      Line.Builder line = Line.newBuilder().setLineContent(verse.getVerseContentList().get(i));
      if (shouldTransliterate(key, verse)) {
        line.setTransliteration(verse.getTransliterationList().get(i));
      }
      verseBuilder.addLines(line);
    }
    return verseBuilder.build();
  }

  private boolean shouldTransliterate(
      HymnalNetKey key, com.hymnsmobile.pipeline.hymnalnet.models.Verse verse) {
    if (verse.getTransliterationCount() == 0) {
      return false;
    }

    if (verse.getVerseContentCount() != verse.getTransliterationCount()) {
      throw new IllegalArgumentException(
          String.format("%s has %s transliteration lines and %s verse lines", key,
              verse.getTransliterationCount(), verse.getVerseContentCount()));
    }
    return true;
  }

  public Hymn toHymn(H4aHymn hymn) {
    if (TextUtil.isEmpty(hymn.getFirstStanzaLine())) {
      throw new IllegalStateException(String.format("Unable to convert to hymn: %s", hymn));
    }

    Hymn.Builder builder = Hymn.newBuilder()
        .setId(nextHymnId++)
        .addReferences(toSongReference(hymn.getId()))
        .setTitle(hymn.getFirstStanzaLine())
        .addAllLyrics(hymn.getVersesList())
        .setLanguage(getLanguage(toSongReference(hymn.getId())))
        .addProvenance("h4a");

    if (hymn.hasMainCategory() && !TextUtil.isEmpty(hymn.getMainCategory())) {
      builder.addCategory(hymn.getMainCategory());
    }
    if (hymn.hasSubCategory() && !TextUtil.isEmpty(hymn.getSubCategory())) {
      builder.addSubCategory(hymn.getSubCategory());
    }
    if (hymn.hasAuthor() && !TextUtil.isEmpty(hymn.getAuthor())) {
      builder.addAuthor(hymn.getAuthor());
    }
    if (hymn.hasComposer() && !TextUtil.isEmpty(hymn.getComposer())) {
      builder.addComposer(hymn.getComposer());
    }
    if (hymn.hasKey() && !TextUtil.isEmpty(hymn.getKey())) {
      builder.addKey(hymn.getKey());
    }
    if (hymn.hasTime() && !TextUtil.isEmpty(hymn.getTime())) {
      builder.addTime(hymn.getTime());
    }
    if (hymn.hasMeter() && !TextUtil.isEmpty(hymn.getMeter())) {
      builder.addMeter(hymn.getMeter());
    }
    if (hymn.hasScriptures() && !TextUtil.isEmpty(hymn.getScriptures())) {
      builder.addScriptures(hymn.getScriptures());
    }
    if (hymn.hasHymnCode() && !TextUtil.isEmpty(hymn.getHymnCode())) {
      builder.addHymnCode(hymn.getHymnCode());
    }
    if (hymn.hasPianoSvg() && !TextUtil.isEmpty(hymn.getPianoSvg())) {
      builder.putSvgSheet("Piano", hymn.getPianoSvg());
    }
    if (hymn.hasGuitarSvg() && !TextUtil.isEmpty(hymn.getGuitarSvg())) {
      builder.putSvgSheet("Guitar", hymn.getGuitarSvg());
    }

    hymn.getRelatedList().stream()
        .map(this::toSongReference)
        .forEach(builder::addLanguages);

    // Add the parent hymn if it's not already included in the related list.
    if (hymn.hasParentHymn()) {
      SongReference parentReference = toSongReference(hymn.getParentHymn());
      if (!builder.getLanguagesList().contains(parentReference)) {
        builder.addLanguages(parentReference);
      }
    }
    builder.setFlattenedLyrics(flattenLyrics(builder.getLyricsList()));
    return builder.build();
  }

  /**
   * Converts a {@link HymnalNetJson} to a {@link Hymn}.
   */
  public Hymn toHymn(RussianHymn hymn) {
    Hymn.Builder builder
        = Hymn.newBuilder()
              .setId(nextHymnId++)
              .setLanguage(Language.RUSSIAN)
              .addAllLyrics(hymn.getLyricsList())
              .addReferences(SongReference.newBuilder()
                                          .setHymnType(RUSSIAN.abbreviatedValue)
                                          .setHymnNumber(String.valueOf(hymn.getNumber())))
              .setTitle(hymn.getTitle())
              .addCategory(hymn.getCategory())
              .addSubCategory(hymn.getSubCategory())
              .addMeter(hymn.getMeter())
              .addLanguages(hymn.getParent())
              .addProvenance("russian");
    builder.setFlattenedLyrics(flattenLyrics(builder.getLyricsList()));
    return builder.build();
  }

  public Hymn toHymn(SongbaseHymn hymn) {
    Hymn.Builder builder = Hymn.newBuilder()
        .setId(nextHymnId++)
        .addAllReferences(
            hymn.getKeyList().stream().map(this::toSongReference).collect(toImmutableList()))
        .setTitle(hymn.getTitle())
        .addAllInlineChords(createInlineChords(hymn.getLyrics()));

    // Check to see if there are mismatched languages
    Set<Language> languages =
        builder.getReferencesList().stream().map(this::getLanguage).collect(Collectors.toSet());
    if (languages.size() != 1) {
      errors.add(PipelineError.newBuilder()
              .setSeverity(Severity.ERROR)
              .setErrorType(ErrorType.DUPLICATE_LANGUAGE_MISMATCH)
              .setSource(PipelineError.Source.SONGBASE)
              .addMessages(builder.getReferencesList().toString())
              .build());
    }

    builder.setFlattenedLyrics(flattenInlineChords(builder.getInlineChordsList()));
    return builder.build();
  }

  private List<ChordLine> createInlineChords(String lyrics) {
    if (TextUtil.isEmpty(lyrics)) {
      errors.add(
          PipelineError
              .newBuilder()
              .setSeverity(PipelineError.Severity.ERROR)
              .setErrorType(PipelineError.ErrorType.INLINE_CHORDS_EMPTY)
              .setSource(PipelineError.Source.SONGBASE)
              .build());
      return new ArrayList<>();
    }
    return Arrays.stream(lyrics.split("\\n")).map(line -> {
      if (line.isEmpty()) {
        return Collections.singletonList(" ");
      }

      List<String> words = new ArrayList<>();
      Pattern p = Pattern.compile(SEPARATOR_PATTERN);
      Matcher m = p.matcher(line);
      while (m.find()) {
        words.add(m.group());
      }
      return words;
    }).map(line -> {
      Pattern p = Pattern.compile(CHORDS_PATTERN);
      boolean lineContainsChords = line.stream()
                                       .map(chordWord -> p.matcher(chordWord).find())
                                       .reduce(false, (bool1, bool2) -> bool1 || bool2);
      List<ChordWord> chordWords = line.stream().map(chordWord -> {
        if (!lineContainsChords) {
          return ChordWord.newBuilder().setWord(chordWord).build();
        }
        String word = chordWord;
        StringBuilder chordBuilder = new StringBuilder();
        Matcher m = p.matcher(word);
        while (m.find()) {
          String chord = m.group(1);
          int index = m.start();
          while (chordBuilder.length() < index) {
            chordBuilder.append(" ");
          }
          chordBuilder.append(chord);
          word = word.replaceFirst(CHORDS_PATTERN, "");
          m = p.matcher(word);
        }
        return ChordWord.newBuilder().setWord(word).setChord(chordBuilder.toString()).build();
      }).collect(Collectors.toList());
      return ChordLine.newBuilder().addAllChordWords(chordWords).build();
    }).collect(Collectors.toList());
  }

  private String flattenLyrics(List<Verse> lyrics) {
    return lyrics.stream()
        .filter(verse -> !verse.getVerseType().equals("copyright")) // Don't include copyright statement
        .map(verse -> verse.getLinesList().stream()
                           .map(Line::getLineContent)
                           .map(line -> line.getBytes(StandardCharsets.ISO_8859_1))
                           .map(line -> new String(line, StandardCharsets.UTF_8))
                           .map(line -> line.replaceAll("\\p{P}", "")) // remove punctuation
                           .map(String::toLowerCase)
                           .map(String::trim)
                           .collect(Collectors.joining(" "))
                           .trim())
        .collect(Collectors.joining(" "));
  }

  private String flattenInlineChords(List<ChordLine> inlineChords) {
    return inlineChords.stream()
        .map((Function<ChordLine, Optional<String>>) chordLine -> {
          String line = chordLine.getChordWordsList().stream()
                                 .map(chordWord -> chordWord.getWord().trim())
                                 .collect(Collectors.joining(" "))
                                 .toLowerCase();
          if (line.matches("\\d+")) {
            return Optional.empty();
          }
          if (line.contains("chorus")) {
            return Optional.empty();
          }
          if (line.contains("capo")) {
            return Optional.empty();
          }
          if (line.contains("#")) {
            return Optional.empty();
          }
          return Optional.of(line);
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.joining(" "));
  }
}
