package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.HymnType;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.Hymn;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface HymnalNetPipelineModule {

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<Hymn> hymns() {
    return new HashSet<>();
  }

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<HymnalNetJson> hymnalNetJsons() {
    return new HashSet<>();
  }

  @Provides
  @HymnalNetPipelineScope
  static ImmutableList<HymnalNetKey> songsToFetch() {
    ImmutableList.Builder<HymnalNetKey> builder = ImmutableList.builder();
    for (HymnType hymnType : HymnType.values()) {
      for (int hymnNumber = 1; hymnNumber < hymnType.maxNumber.orElse(1000); hymnNumber++) {
        builder.add(HymnalNetKey.newBuilder().setHymnType(hymnType.abbreviation)
            .setHymnNumber(String.valueOf(hymnNumber)).build());
      }
    }
    return builder.build();
  }
}
