package com.hymnsmobile.pipeline.liederbuch;

import com.hymnsmobile.pipeline.liederbuch.dagger.LiederbuchScope;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

@LiederbuchScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern ID_PATTERN = Pattern.compile("(\\D+)(\\d+)");

  @Inject
  public Converter() {
  }

  public Optional<LiederbuchKey> toKey(String id) {
    Optional<String> type = extractTypeFromId(id);
    Optional<String> number = extractNumberFromId(id);

    if (type.isEmpty() || number.isEmpty()) {
      LOGGER.severe(String.format("%s was unable to be parsed into a Liederbuch key", id));
      return Optional.empty();
    }

    LiederbuchKey.Builder builder = LiederbuchKey.newBuilder().setType(type.get())
        .setNumber(number.get());
    return Optional.of(builder.build());
  }

  private static Optional<String> extractTypeFromId(String path) {
    Matcher matcher = ID_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(1));
  }

  private static Optional<String> extractNumberFromId(String path) {
    Matcher matcher = ID_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(2));
  }
}
