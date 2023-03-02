package com.hymnsmobile.pipeline.hymnalnet;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.MapEntry;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.Datum;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.hymnalnet.models.Verse;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.Line;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+)/(c?\\d+[a-z]*)(\\?gb=1)?");

  private final Set<PipelineError> errors;

  @Inject
  public Converter(Set<PipelineError> errors) {
    this.errors = errors;
  }

  public Hymn toHymn(HymnalNetJson hymn) {
    HymnalNetKey key = hymn.getKey();
    Hymn.Builder builder = Hymn.newBuilder().setReference(toSongReference(key))
        .setTitle(hymn.getTitle());

    hymn.getLyricsList().forEach(verse -> builder.addLyrics(toVerse(key, verse)));

    hymn.getMetaDataList().forEach(metaDatum -> {
      String name = metaDatum.getName();
      // Ignore the "See Also" item because it changes every time you call it.
      if (name.equals("See Also")) {
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
          Optional<HymnalNetKey> relatedKey = extractFromPath(datum.getPath());
          if (relatedKey.isEmpty()) {
            errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING).setMessage(
                    String.format("%s had an unrecognized related song: %s", key, datum.getPath()))
                .build());
            return;
          }
          if (metaDatumType.get() == MetaDatumType.LANGUAGES) {
            builder.putLanguages(datum.getValue(), toSongReference(relatedKey.get()));
          }
          if (metaDatumType.get() == MetaDatumType.RELEVANT) {
            builder.putRelevants(datum.getValue(), toSongReference(relatedKey.get()));
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
    LOGGER.info(String.format("%s successfully converted", key));
    return builder.build();
  }

  public SongReference toSongReference(HymnalNetKey key) {
    HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
    Optional<String> queryParams =
        key.hasQueryParams() ? Optional.of(key.getQueryParams()) : Optional.empty();

    SongReference.Builder builder = SongReference.newBuilder();
    if (hymnType == HymnType.CHINESE && queryParams.map(s -> s.equals("?gb=1")).orElse(false)) {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED);
    } else if (hymnType == HymnType.CHINESE_SUPPLEMENTAL && queryParams.map(s -> s.equals("?gb=1"))
        .orElse(false)) {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED);
    } else {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.valueOf(hymnType.name()));
    }
    return builder.setNumber(key.getHymnNumber()).build();
  }

  public HymnalNetKey toHymnalNetKey(SongReference reference) {
    String hymnNumber = reference.getNumber();

    if (reference.getType() == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED) {
      return HymnalNetKey.newBuilder().setHymnType(HymnType.CHINESE.abbreviation)
          .setHymnNumber(hymnNumber).setQueryParams("?gb=1").build();
    }

    if (reference.getType()
        == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED) {
      return HymnalNetKey.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL.abbreviation)
          .setHymnNumber(hymnNumber).setQueryParams("?gb=1").build();
    }

    return HymnalNetKey.newBuilder()
        .setHymnType(HymnType.valueOf(reference.getType().name()).abbreviation)
        .setHymnNumber(hymnNumber).build();
  }

  public static Optional<HymnalNetKey> extractFromPath(String path) {
    Optional<String> hymnType = extractTypeFromPath(path);
    Optional<String> hymnNumber = extractNumberFromPath(path);
    Optional<String> queryParam = extractQueryParamFromPath(path);

    if (hymnType.isEmpty() || hymnNumber.isEmpty()) {
      LOGGER.severe(String.format("%s was unable to be parsed into a HymnalDbKey", path));
      return Optional.empty();
    }

    HymnalNetKey.Builder builder = HymnalNetKey.newBuilder().setHymnType(hymnType.get())
        .setHymnNumber(hymnNumber.get());
    queryParam.ifPresent(builder::setQueryParams);

    return Optional.of(builder.build());
  }

  private static Optional<String> extractTypeFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(1));
  }

  private static Optional<String> extractNumberFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(2));
  }

  private static Optional<String> extractQueryParamFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.ofNullable(matcher.group(3));
  }

  private com.hymnsmobile.pipeline.models.Verse toVerse(HymnalNetKey key, Verse verse) {
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

  private boolean shouldTransliterate(HymnalNetKey key, Verse verse) {
    if (verse.getTransliterationCount() == 0) {
      return false;
    }

    if (verse.getVerseContentCount() != verse.getTransliterationCount()) {
      errors.add(PipelineError.newBuilder().setMessage(
              String.format("%s has %s transliteration lines and %s verse lines", key,
                  verse.getTransliterationCount(), verse.getVerseContentCount()))
          .setSeverity(Severity.WARNING).build());
      return false;
    }
    return true;
  }
}
