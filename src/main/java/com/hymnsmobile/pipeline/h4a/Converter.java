package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.dagger.H4a;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

@H4aPipelineScope
public class Converter {

  private static final Pattern ID_PATTERN = Pattern.compile("([A-Z]+)([a-z]?\\d+\\D*)");

  private final Set<PipelineError> errors;

  @Inject
  public Converter(@H4a Set<PipelineError> errors) {
    this.errors = errors;
  }

  public H4aKey toKey(String id) {
    return H4aKey.newBuilder().setType(extractType(id).abbreviation).setNumber(extractNumber(id))
        .build();
  }

  private HymnType extractType(String id) {
    Matcher matcher = ID_PATTERN.matcher(id);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Unable to extract type from " + id);
    }
    return HymnType.fromString(matcher.group(1)).orElseThrow();
  }

  private String extractNumber(String id) {
    Matcher matcher = ID_PATTERN.matcher(id);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Unable to extract number from " + id);
    }
    return matcher.group(2);
  }
}
