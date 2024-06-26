package com.hymnsmobile.pipeline.liederbuch;

import com.hymnsmobile.pipeline.liederbuch.dagger.LiederbuchScope;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;

import javax.inject.Inject;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LiederbuchScope
public class Converter {

  private static final Logger LOGGER = Logger.getGlobal();

  private static final Pattern ID_PATTERN = Pattern.compile("(\\D+)(\\d+)");

  @Inject
  public Converter() {
  }

  public LiederbuchKey toKey(String id) {
    return LiederbuchKey.newBuilder().setType(extractType(id).abbreviation).setNumber(extractNumber(id)).build();
  }

  private static HymnType extractType(String id) {
    Matcher matcher = ID_PATTERN.matcher(id);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Unable to extract type from " + id);
    }
    return HymnType.fromString(matcher.group(1)).orElseThrow();
  }

  private static String extractNumber(String id) {
    Matcher matcher = ID_PATTERN.matcher(id);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Unable to extract number from " + id);
    }
    return matcher.group(2);
  }
}
