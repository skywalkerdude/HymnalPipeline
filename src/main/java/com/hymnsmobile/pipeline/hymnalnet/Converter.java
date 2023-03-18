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
import com.hymnsmobile.pipeline.models.SongLink;
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

  public Optional<Hymn> toHymn(HymnalNetJson hymn) {
    HymnalNetKey key = hymn.getKey();
    Optional<SongReference> songReferenceOptional = toSongReference(key);
    if (songReferenceOptional.isEmpty()) {
      return Optional.empty();
    }
    SongReference songReference = songReferenceOptional.get();

    Hymn.Builder builder = Hymn.newBuilder().setReference(songReference).setTitle(hymn.getTitle());

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
          Optional<HymnalNetKey> relatedKey = extractFromPath(datum.getPath());
          if (relatedKey.isEmpty()) {
            errors.add(PipelineError.newBuilder().setSeverity(Severity.WARNING).setMessage(
                    String.format("%s had an unrecognized related song: %s", key, datum.getPath()))
                .build());
            return;
          }
          if (metaDatumType.get() == MetaDatumType.LANGUAGES) {
            toSongReference(relatedKey.get()).ifPresent(
                languageReference -> builder.addLanguages(SongLink.newBuilder()
                    .setName(datum.getValue())
                    .setReference(languageReference).build()));
          }
          if (metaDatumType.get() == MetaDatumType.RELEVANT) {
            toSongReference(relatedKey.get()).ifPresent(
                relevantReference -> builder.addRelevants(SongLink.newBuilder()
                    .setName(datum.getValue())
                    .setReference(relevantReference).build()));
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
    return Optional.of(builder.build());
  }

  public Optional<SongReference> toSongReference(HymnalNetKey key) {
    HymnType hymnType = HymnType.fromString(key.getHymnType()).orElseThrow();
    String hymnNumber = key.getHymnNumber();

    Optional<String> queryParamsOptional =
        key.hasQueryParams() ? Optional.of(key.getQueryParams()) : Optional.empty();

    // Songs that end with "c" are actually Chinese songs, so we change their type to being Chinese
    if (hymnNumber.matches("\\d+c")) {
      hymnNumber = hymnType.abbreviation + hymnNumber;
      hymnType = HymnType.CHINESE;
    }

    SongReference.Builder builder = SongReference.newBuilder().setNumber(hymnNumber);
    if (queryParamsOptional.isEmpty()) {
      return Optional.of(
          builder.setType(com.hymnsmobile.pipeline.models.HymnType.valueOf(hymnType.name()))
              .build());
    }

    String queryParams = queryParamsOptional.get();;
    if (queryParams.equals("?gb=1")) {
      if (hymnType == HymnType.CHINESE) {
        return Optional.of(
            builder.setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED).build());
      }

      if (hymnType == HymnType.CHINESE_SUPPLEMENTAL) {
        return Optional.of(
            builder.setType(
                com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED).build());
      }
    }
    errors.add(PipelineError.newBuilder().setSeverity(Severity.ERROR)
        .setMessage(String.format("%s is an invalid key", key)).build());
    return Optional.empty();
  }

  public HymnalNetKey toHymnalNetKey(SongReference reference) {
    HymnalNetKey.Builder builder = HymnalNetKey.newBuilder();

    String hymnNumber = reference.getNumber();

    if (reference.getType() == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED) {
      builder.setHymnType(HymnType.CHINESE.abbreviation).setQueryParams("?gb=1").build();
    } else if (reference.getType()
        == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED) {
      builder.setHymnType(HymnType.CHINESE_SUPPLEMENTAL.abbreviation).setQueryParams("?gb=1").build();
    } else {
      builder.setHymnType(HymnType.valueOf(reference.getType().name()).abbreviation);
    }

    // Some Hymnal.net songs that are of the form NS/618c are actually Chinese songs, and are thus
    // converted into an internal representation of CH/ns618c. So here, we need to convert them back
    // into the Hymnal.net form.
    Pattern pattern = Pattern.compile("(\\D+)(\\d+c)");
    Matcher matcher = pattern.matcher(hymnNumber);
    if (matcher.find()) {
      builder.setHymnType(matcher.group(1));
      builder.setHymnNumber(matcher.group(2));
    } else {
      builder.setHymnNumber(hymnNumber);
    }
    return builder.build();
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
