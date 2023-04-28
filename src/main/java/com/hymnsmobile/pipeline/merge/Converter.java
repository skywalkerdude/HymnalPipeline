package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.models.HymnType.BLUE_SONGBOOK;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.FRENCH;
import static com.hymnsmobile.pipeline.models.HymnType.HOWARD_HIGASHI;
import static com.hymnsmobile.pipeline.models.HymnType.LIEDERBUCH;
import static com.hymnsmobile.pipeline.models.HymnType.SONGBASE_OTHER;
import static com.hymnsmobile.pipeline.models.HymnType.SPANISH;

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
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.Line;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.models.Verse;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;

@MergeScope
public class Converter {

  private static int nextHymnId = 1;

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

    // Songs that end with "c" are actually Chinese songs, so we change their type to being Chinese
    if (hymnNumber.matches("\\d+c")) {
      hymnNumber = hymnType.abbreviation + hymnNumber;
      hymnType = com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE;
    }

    SongReference.Builder builder = SongReference.newBuilder().setHymnNumber(hymnNumber);
    if (queryParamsOptional.isPresent()) {
      String queryParams = queryParamsOptional.get();
      if (queryParams.equals("?gb=1")) {
        if (hymnType == com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE) {
          return builder.setHymnType(HymnType.CHINESE_SIMPLIFIED)
              .build();
        }

        if (hymnType == com.hymnsmobile.pipeline.hymnalnet.HymnType.CHINESE_SUPPLEMENTAL) {
          return builder.setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).build();
        }
      }
      throw new IllegalArgumentException("Unexpected query param found");
    }
    return builder.setHymnType(HymnType.valueOf(hymnType.name()))
        .build();
  }

  public SongReference toSongReference(H4aKey key) {
    SongReference.Builder reference = SongReference.newBuilder();

    com.hymnsmobile.pipeline.h4a.HymnType type = com.hymnsmobile.pipeline.h4a.HymnType.fromString(
        key.getType()).orElseThrow();
    String number = key.getNumber();

    // Howard Higashi songs in H4a are NS10XX
    if (isHowardHigashi(type, number)) {
      return reference.setHymnType(HOWARD_HIGASHI)
          .setHymnNumber(Integer.toString(Integer.parseInt(number) - 1000)).build();
    }
    return reference.setHymnType(HymnType.valueOf(type.name())).setHymnNumber(number).build();
  }

  public SongReference toSongReference(LiederbuchKey key) {
    SongReference.Builder reference = SongReference.newBuilder().setHymnNumber(key.getNumber());

    com.hymnsmobile.pipeline.liederbuch.HymnType type =
        com.hymnsmobile.pipeline.liederbuch.HymnType.fromString(key.getType()).orElseThrow();

    if (type == com.hymnsmobile.pipeline.liederbuch.HymnType.GERMAN) {
      return reference.setHymnType(LIEDERBUCH).build();
    }

    int numberInt = Integer.parseInt(key.getNumber());
    if (type == com.hymnsmobile.pipeline.liederbuch.HymnType.NEW_SONG && numberInt > 1000) {
      return reference.setHymnType(HOWARD_HIGASHI).setHymnNumber(String.valueOf(numberInt - 1000)).build();
    }
    return reference.setHymnType(com.hymnsmobile.pipeline.models.HymnType.valueOf(type.name())).build();
  }

  public SongReference toSongReference(SongbaseKey key) {
    SongReference.Builder reference = SongReference.newBuilder().setHymnNumber(key.getHymnNumber());

    com.hymnsmobile.pipeline.songbase.HymnType type =
        com.hymnsmobile.pipeline.songbase.HymnType.fromString(key.getHymnType()).orElseThrow();

    final HymnType hymnType;
    switch (type) {
      case HYMNAL:
        hymnType = CLASSIC_HYMN;
        break;
      case BLUE_SONGBOOK:
        hymnType = BLUE_SONGBOOK;
        break;
      case HIMNOS:
        hymnType = SPANISH;
        break;
      case LIEDERBUCH:
        hymnType = LIEDERBUCH;
        break;
      case CANTIQUES:
        hymnType = FRENCH;
        break;
      case SONGBASE_OTHER:
        hymnType = SONGBASE_OTHER;
        break;
      default:
        throw new IllegalStateException("Unexpected hymn type: " + type);
    }
    return reference.setHymnType(hymnType).build();
  }

  /**
   * Converts an {@link SongReference} to a language {@link SongLink} by inferring the language text
   * from the hymn type.
   */
  public SongLink toLanguageLink(SongReference reference) {
    final String name;
    switch (reference.getHymnType()) {
      case TAGALOG:
        name = "Tagalog";
        break;
      case CEBUANO:
        name = "Cebuano";
        break;
      case GERMAN:
        name = "German";
        break;
      case CHINESE:
        // fall through
      case CHINESE_SUPPLEMENTAL:
        name = "詩歌(繁)";
        break;
      case CHINESE_SIMPLIFIED:
        // fall through
      case CHINESE_SUPPLEMENTAL_SIMPLIFIED:
        name = "诗歌(简)";
        break;
      case KOREAN:
        name = "Korean";
        break;
      case INDONESIAN:
        name = "Indonesian";
        break;
      case JAPANESE:
        name = "Japanese";
        break;
      case SPANISH:
        name = "Spanish";
        break;
      case FRENCH:
        name = "French";
        break;
      case FARSI:
        name = "Farsi";
        break;
      case CLASSIC_HYMN:
        // fall through
      case NEW_SONG:
        // fall through
      case HOWARD_HIGASHI:
        // fall through
      case CHILDREN_SONG:
      case BE_FILLED:
        name = "English";
        break;
      default:
        throw new IllegalArgumentException("Unexpected type encountered: " + reference);
    }
    return SongLink.newBuilder().setName(name).setReference(reference).build();
  }

  /**
   * Converts a {@link HymnalNetJson} to a {@link Hymn}.
   */
  public Hymn toHymn(HymnalNetJson hymn) {
    HymnalNetKey key = hymn.getKey();

    Hymn.Builder builder = Hymn.newBuilder()
        .setId(nextHymnId++)
        .addReferences(toSongReference(hymn.getKey()))
        .setTitle(hymn.getTitle());

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
        errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING)
            .setMessage(String.format("MetaDatum name not found for %s: %s", key, metaDatum))
            .build());
        return;
      }

      FieldDescriptor field = Hymn.Builder.getDescriptor()
          .findFieldByName(metaDatumType.get().name().toLowerCase());
      if (metaDatumType.get() == MetaDatumType.LANGUAGES
          || metaDatumType.get() == MetaDatumType.RELEVANT) {
        metaDatum.getDataList().forEach(datum -> {
          Optional<HymnalNetKey> relatedKey =
              com.hymnsmobile.pipeline.hymnalnet.Converter.extractFromPath(datum.getPath());
          if (relatedKey.isEmpty()) {
            errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING).setMessage(
                    String.format("%s had an unrecognized related song: %s", key, datum.getPath()))
                .build());
            return;
          }
          if (metaDatumType.get() == MetaDatumType.LANGUAGES) {
            builder.addLanguages(SongLink.newBuilder()
                .setName(datum.getValue())
                .setReference(toSongReference(relatedKey.get())).build());
          }
          if (metaDatumType.get() == MetaDatumType.RELEVANT) {
            builder.addRelevants(SongLink.newBuilder()
                .setName(datum.getValue())
                .setReference(toSongReference(relatedKey.get())).build());
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

  public Optional<Hymn> toHymn(H4aHymn hymn) {
    if (TextUtil.isEmpty(hymn.getFirstStanzaLine())) {
      this.errors.add(PipelineError.newBuilder().setSeverity(Severity.ERROR)
          .setMessage(String.format("Unable to convert to hymn: %s", hymn)).build());
      return Optional.empty();
    }

    Hymn.Builder builder = Hymn.newBuilder()
        .setId(nextHymnId++)
        .addReferences(toSongReference(hymn.getId()))
        .setTitle(hymn.getFirstStanzaLine())
        .addAllLyrics(hymn.getVersesList());

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
        .map(key -> toLanguageLink(toSongReference(key)))
        .forEach(builder::addLanguages);

    // Add the parent hymn if it's not already included in the related list.
    if (hymn.hasParentHymn()) {
      SongReference parentReference = toSongReference(hymn.getParentHymn());
      if (!builder.getLanguagesList().stream().map(SongLink::getReference)
          .collect(Collectors.toSet()).contains(parentReference)) {
        builder.addLanguages(toLanguageLink(parentReference));
      }
    }

    return Optional.of(builder.build());
  }

  private boolean isHowardHigashi(com.hymnsmobile.pipeline.h4a.HymnType type, String number) {
    if (type == com.hymnsmobile.pipeline.h4a.HymnType.NEW_SONG && TextUtil.isNumeric(number)) {
      return Integer.parseInt(number) >= 1001 && Integer.parseInt(number) <= 1087;
    }
    return false;
  }

  public Hymn toHymn(SongbaseHymn hymn) {
    return Hymn.newBuilder()
        .setId(nextHymnId++)
        .addAllReferences(
            hymn.getKeyList().stream().map(this::toSongReference).collect(toImmutableList()))
        .setTitle(hymn.getTitle())
        // Add into lyrics for FTS
        .addLyrics(
            Verse.newBuilder().setVerseType("do_not_display").addAllLines(
                hymn.getLyrics()
                    .lines()
                    .map(line -> line.replaceAll("\\[.*?]", ""))
                    .filter(line -> !TextUtil.isEmpty(line))
                    .map(String::trim)
                    .map(line -> Line.newBuilder().setLineContent(line))
                    .map(Line.Builder::build)
                    .collect(toImmutableList())))
        .setInlineChords(hymn.getLyrics()).build();
  }
}
