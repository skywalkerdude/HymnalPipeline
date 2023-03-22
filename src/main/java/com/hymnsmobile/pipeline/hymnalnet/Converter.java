package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+)/(c?\\d+[a-z]*)(\\?gb=1)?");

  @Inject
  public Converter() {}

  public ImmutableList<HymnalNetKey> getRelated(String field, HymnalNetJson hymn) {
    return hymn.getMetaDataList().stream()
        .filter(metaDatum -> metaDatum.getName().equals(field))
        .flatMap(metaDatum -> metaDatum.getDataList().stream()
            .map(datum -> extractFromPath(datum.getPath()))
            .filter(Optional::isPresent)
            .map(Optional::get))
        .collect(toImmutableList());
  }

  // public HymnalNetKey toHymnalNetKey(SongReference reference) {
  //   HymnalNetKey.Builder builder = HymnalNetKey.newBuilder();
  //
  //   String hymnNumber = reference.getNumber();
  //
  //   if (reference.getType() == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED) {
  //     builder.setHymnType(HymnType.CHINESE.abbreviation).setQueryParams("?gb=1").build();
  //   } else if (reference.getType()
  //       == com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED) {
  //     builder.setHymnType(HymnType.CHINESE_SUPPLEMENTAL.abbreviation).setQueryParams("?gb=1").build();
  //   } else {
  //     builder.setHymnType(HymnType.valueOf(reference.getType().name()).abbreviation);
  //   }
  //
  //   // Some Hymnal.net songs that are of the form NS/618c are actually Chinese songs, and are thus
  //   // converted into an internal representation of CH/ns618c. So here, we need to convert them back
  //   // into the Hymnal.net form.
  //   Pattern pattern = Pattern.compile("(\\D+)(\\d+c)");
  //   Matcher matcher = pattern.matcher(hymnNumber);
  //   if (matcher.find()) {
  //     builder.setHymnType(matcher.group(1));
  //     builder.setHymnNumber(matcher.group(2));
  //   } else {
  //     builder.setHymnNumber(hymnNumber);
  //   }
  //   return builder.build();
  // }

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


}
