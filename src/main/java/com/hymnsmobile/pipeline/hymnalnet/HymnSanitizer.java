package com.hymnsmobile.pipeline.hymnalnet;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.Verse;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * Performs various sanitizations to ensure data is correct.
 */
@HymnalNetPipelineScope
public class HymnSanitizer {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Set<PipelineError> errors;

  @Inject
  public HymnSanitizer(@HymnalNet Set<PipelineError> errors) {
    this.errors = errors;
  }

  public Hymn.Builder sanitize(HymnalDbKey key, Hymn.Builder hymn) {
    int totalErrors = 0;
    totalErrors += ensureTransliterationHasSameNumberOfLinesAsVerse(key, hymn);
    if (totalErrors > 0) {
      LOGGER.warning(String.format("%s completed sanitization with %d errors", key, totalErrors));
    } else {
      LOGGER.info(String.format("%s completed sanitization with no errors", key));
    }
    return hymn;
  }

  private int ensureTransliterationHasSameNumberOfLinesAsVerse(HymnalDbKey key, Hymn.Builder hymn) {
    int totalErrors = 0;
    for (Verse verse : hymn.getLyricsList()) {
      if (verse.getTransliterationCount() == 0
          || verse.getTransliterationCount() == verse.getVerseContentCount()) {
        continue;
      }
      totalErrors++;
      errors.add(PipelineError.newBuilder()
          .setMessage(String.format("%s has %s transliteration lines and %s verse lines", key,
              verse.getTransliterationCount(), verse.getVerseContentCount()))
          .setSeverity(Severity.WARNING).build());
    }
    return totalErrors;
  }
}
