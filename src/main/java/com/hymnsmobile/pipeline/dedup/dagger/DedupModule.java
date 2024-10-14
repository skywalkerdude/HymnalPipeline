package com.hymnsmobile.pipeline.dedup.dagger;

import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.LinkedHashSet;
import java.util.Set;

@Module
interface DedupModule {

  @Provides
  @DedupScope
  static LevenshteinDistance levenshteinDistance() {
    return LevenshteinDistance.getDefaultInstance();
  }

  @Dedup
  @Provides
  @DedupScope
  static Set<PipelineError> errors() {
    return new LinkedHashSet<>();
  }
}
