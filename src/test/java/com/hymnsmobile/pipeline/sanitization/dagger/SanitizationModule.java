package com.hymnsmobile.pipeline.sanitization.dagger;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.HymnalNetPatcher;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

@Module
interface SanitizationModule {

  @Sanitization
  @Provides
  @SanitizationScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }

  @Provides
  @SanitizationScope
  static HymnalNetPatcher patcher() {
    return new HymnalNetPatcher() {

      @Override
      public ImmutableMap<Hymn, ImmutableList<SongReference>> patch(
          ImmutableMap<Hymn, ImmutableList<SongReference>> mergedHymns) {
        ImmutableMap<Hymn.Builder, ImmutableList<SongReference>> builders = mergedHymns.entrySet()
            .stream().collect(toImmutableMap(
                entry -> entry.getKey().toBuilder(), Entry::getValue));

        // Test patcher that clears the languages of hymn 40 and hymn 41
        SongReference h40 = SongReference.newBuilder().setType(HymnType.CLASSIC_HYMN)
            .setNumber("40").build();
        SongReference h41 = SongReference.newBuilder().setType(HymnType.CLASSIC_HYMN)
            .setNumber("41").build();

        builders.keySet().stream()
            .filter(hymn -> ImmutableList.of(h40, h41).contains(hymn.getReference()))
            .forEach(
                Hymn.Builder::clearLanguages);

        return builders.entrySet().stream()
            .collect(toImmutableMap(entry -> entry.getKey().build(), Entry::getValue));
      }
    };
  }
}
