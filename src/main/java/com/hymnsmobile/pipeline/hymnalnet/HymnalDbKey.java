package com.hymnsmobile.pipeline.hymnalnet;

import com.hymnsmobile.pipeline.utils.TextUtil;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HymnalDbKey {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+)/(c?\\d+[a-z]*)(\\?gb=1)?");

  public final HymnType hymnType;
  public final String hymnNumber;
  public final Optional<String> queryParams;

  public static HymnalDbKey create(HymnType hymnType, String hymnNumber) {
    return new HymnalDbKey(hymnType, hymnNumber, Optional.empty());
  }

  public static HymnalDbKey create(HymnType hymnType, String hymnNumber, String queryParams) {
    return new HymnalDbKey(hymnType, hymnNumber, Optional.of(queryParams));
  }

  public static Optional<HymnalDbKey> fromString(String string) {
    String[] parts = string.split("/");
    if (parts.length != 3) {
      return Optional.empty();
    }
    Optional<HymnType> hymnType = HymnType.fromString(parts[0]);
    if (hymnType.isEmpty()) {
      throw new RuntimeException(String.format("Hymn type %s was not found", parts[0]));
    }

    String hymnNumber = parts[1];
    Optional<String> queryParams =
        parts[2].equals("Optional.empty") ? Optional.empty() : Optional.of(parts[2]);
    return Optional.of(new HymnalDbKey(hymnType.get(), hymnNumber, queryParams));
  }

  private HymnalDbKey(HymnType hymnType, String hymnNumber, Optional<String> queryParams) {
    assert hymnType != null;
    assert !TextUtil.isEmpty(hymnNumber);

    this.hymnType = hymnType;
    this.hymnNumber = hymnNumber;
    this.queryParams = queryParams;
  }

  @Override
  public int hashCode() {
    return hymnType.hashCode() + hymnNumber.hashCode() + queryParams.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof HymnalDbKey) {
      HymnalDbKey key = (HymnalDbKey) obj;
      return hymnType == key.hymnType && hymnNumber.equals(key.hymnNumber) && queryParams.equals(
          key.queryParams);
    }
    return false;
  }

  @Override
  public String toString() {
    return hymnType + "/" + hymnNumber + "/" + queryParams;
  }

  public static Optional<HymnalDbKey> extractFromPath(String path) {
    Optional<HymnType> hymnType = extractTypeFromPath(path);
    Optional<String> hymnNumber = extractNumberFromPath(path);
    Optional<String> queryParam = extractQueryParamFromPath(path);

    if (hymnType.isEmpty() || hymnNumber.isEmpty()) {
      LOGGER.severe(String.format("%s was unable to be parsed into a HymnalDbKey", path));
      return Optional.empty();
    }

    return Optional.of(new HymnalDbKey(hymnType.get(), hymnNumber.get(), queryParam));
  }

  public static Optional<HymnType> extractTypeFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return HymnType.fromString(matcher.group(1));
  }

  public static Optional<String> extractNumberFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(2));
  }

  public static Optional<String> extractQueryParamFromPath(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (!matcher.find()) {
      return Optional.empty();
    }

    return Optional.ofNullable(matcher.group(3));
  }
}
