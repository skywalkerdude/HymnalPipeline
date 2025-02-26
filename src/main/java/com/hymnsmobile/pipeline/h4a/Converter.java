package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.dagger.H4a;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.utils.TextUtil;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@H4aPipelineScope
public class Converter {

  private static final Pattern ID_PATTERN = Pattern.compile("([A-Z]+)([a-z]?\\d+\\D*)");

  private final Set<PipelineError> errors;

  @Inject
  public Converter(@H4a Set<PipelineError> errors) {
    this.errors = errors;
  }

  public Optional<H4aKey> toKey(String id) {
    Matcher matcher = ID_PATTERN.matcher(id);
    if (!matcher.find()) {
      errors.add(PipelineError.newBuilder()
                     .setSource(PipelineError.Source.H4A)
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_KEY)
                     .addMessages(id)
                     .build());
      return Optional.empty();
    }

    Optional<HymnType> hymnType = HymnType.fromString(matcher.group(1));
    if (hymnType.isEmpty()) {
      errors.add(PipelineError.newBuilder()
                     .setSource(PipelineError.Source.H4A)
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.UNRECOGNIZED_HYMN_TYPE)
                     .addMessages(id)
                     .build());
      return Optional.empty();
    }

    String hymnNumber = matcher.group(2);
    if (TextUtil.isEmpty(hymnNumber)) {
      errors.add(PipelineError.newBuilder()
                     .setSource(PipelineError.Source.H4A)
                     .setSeverity(PipelineError.Severity.ERROR)
                     .setErrorType(PipelineError.ErrorType.UNPARSEABLE_HYMN_NUMBER)
                     .addMessages(id)
                     .build());
      return Optional.empty();
    }
    return hymnType.map(type -> {
      String number = hymnNumber;
      if (isHowardHigashi(type, number)) {
        type = HymnType.HOWARD_HIGASHI;
        number = Integer.toString(Integer.parseInt(number) - 1000);
      }
      return H4aKey.newBuilder()
                   .setType(type.abbreviation)
                   .setNumber(number)
                   .build();
    });
  }

  /**
   * Howard Higashi songs in H4a are NS10XX
   */
  private boolean isHowardHigashi(HymnType type, String number) {
    if (type == com.hymnsmobile.pipeline.h4a.HymnType.NEW_SONG && TextUtil.isNumeric(number)) {
      return Integer.parseInt(number) >= 1001 && Integer.parseInt(number) <= 1087;
    }
    return false;
  }
}
