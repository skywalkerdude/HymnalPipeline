package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Module
public interface HymnalNetPipelineTestModule {

  List<HymnalNetKey> SONGS_TO_FETCH = new ArrayList<>();

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<Hymn> hymns() {
    return new HashSet<>();
  }

  @Provides
  @HymnalNetPipelineScope
  static Set<HymnalNetJson> hymnalNetJsons() {
    return new HashSet<>();
  }

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }

  @Provides
  @HymnalNetPipelineScope
  static ImmutableList<HymnalNetKey> songsToFetch() {
    return ImmutableList.copyOf(SONGS_TO_FETCH);
  }
}
