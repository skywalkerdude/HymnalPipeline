package com.hymnsmobile.pipeline.hymnalnet;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.Datum;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private final HymnSanitizer sanitizer;
  private final Set<PipelineError> errors;

  @Inject
  public Converter(HymnSanitizer sanitizer, @HymnalNet Set<PipelineError> errors) {
    this.sanitizer = sanitizer;
    this.errors = errors;
  }

  public Hymn toHymn(HymnalDbKey key, HymnalNetJson hymn) {
    Hymn.Builder builder = Hymn.newBuilder().setReference(toSongReference(key))
        .setTitle(hymn.getTitle()).addAllLyrics(hymn.getLyricsList());

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

      if (metaDatumType.get() == MetaDatumType.LANGUAGES
          || metaDatumType.get() == MetaDatumType.RELEVANT) {
        metaDatum.getDataList().forEach(datum -> {
          Optional<HymnalDbKey> relatedKey = HymnalDbKey.extractFromPath(datum.getPath());
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
            builder.putLanguages(datum.getValue(), toSongReference(relatedKey.get()));
          }
        });
      } else {
        metaDatum.getDataList().stream().map(Datum::getValue).forEach(s -> builder.addRepeatedField(
            Hymn.Builder.getDescriptor()
                .findFieldByName(metaDatumType.get().name().toLowerCase()), s));
      }
    });
    LOGGER.info(String.format("%s successfully converted", key));
    return sanitizer.sanitize(key, builder).build();
  }

  public SongReference toSongReference(HymnalDbKey key) {
    HymnType hymnType = key.hymnType;
    Optional<String> queryParams = key.queryParams;

    SongReference.Builder builder = SongReference.newBuilder();
    if (hymnType == HymnType.CHINESE && queryParams.map(s -> s.equals("?gb=1")).orElse(false)) {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED);
    } else if (hymnType == HymnType.CHINESE_SUPPLEMENTAL && queryParams.map(s -> s.equals("?gb=1"))
        .orElse(false)) {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED);
    } else {
      builder.setType(com.hymnsmobile.pipeline.models.HymnType.valueOf(hymnType.name()));
    }
    return builder.setNumber(key.hymnNumber).build();
  }

  public HymnalDbKey toHymnalDbKey(SongReference reference) {
    String hymnNumber = reference.getNumber();

    if (reference.getType() == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED) {
      return HymnalDbKey.create(HymnType.CHINESE, hymnNumber, "?gb=1");
    }

    if (reference.getType()
        == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED) {
      return HymnalDbKey.create(HymnType.CHINESE_SUPPLEMENTAL, hymnNumber, "?gb=1");
    }

    return HymnalDbKey.create(HymnType.valueOf(reference.getType().name()), hymnNumber);
  }
}
