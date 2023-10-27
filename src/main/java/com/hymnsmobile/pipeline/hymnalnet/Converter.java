package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.hymnalnet.models.MetaDatum;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;


@HymnalNetPipelineScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern PATH_PATTERN = Pattern.compile(
      "(\\w+)/([c|s]?\\d+[a-z]*)(\\?gb=1)?");
  private static final Pattern HINARIO_PATH_PATTERN = Pattern.compile("hymn=(\\d+[a-z]*)");

  @Inject
  public Converter() {
  }

  public static Optional<HymnalNetKey> extractFromPath(String path, HymnalNetKey parentHymn,
      Set<PipelineError> errors) {
    Optional<String> hymnType = extractTypeFromPath(path);
    Optional<String> hymnNumber = extractNumberFromPath(path);
    Optional<String> queryParam = extractQueryParamFromPath(path);

    if (hymnType.isEmpty() || hymnNumber.isEmpty()) {
      errors.add(PipelineError.newBuilder()
          .setErrorType(ErrorType.PARSE_ERROR)
          .addMessages(String.format("%s, a related song of %s", path, parentHymn))
          .build());
      return Optional.empty();
    }

    if (HymnType.fromString(hymnType.get()).isEmpty()) {
      errors.add(PipelineError.newBuilder()
          .setErrorType(ErrorType.UNRECOGNIZED_HYMN_TYPE)
          .addMessages(String.format("%s, a related song of %s", path, parentHymn))
          .build());
      return Optional.empty();
    }

    HymnalNetKey.Builder builder = HymnalNetKey.newBuilder().setHymnType(hymnType.get())
        .setHymnNumber(hymnNumber.get());
    queryParam.ifPresent(builder::setQueryParams);

    return Optional.of(builder.build());
  }

  private static Optional<String> extractTypeFromPath(String path) {
    if (path.contains("hinario")) {
      return Optional.of(HymnType.HINOS.abbreviation);
    }

    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }
    return Optional.of(matcher.group(1));
  }

  private static Optional<String> extractNumberFromPath(String path) {
    if (path.contains("hinario")) {
      Matcher matcher = HINARIO_PATH_PATTERN.matcher(path);
      if (!matcher.find()) {
        return Optional.empty();
      }
      return Optional.of(matcher.group(1));
    }

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
