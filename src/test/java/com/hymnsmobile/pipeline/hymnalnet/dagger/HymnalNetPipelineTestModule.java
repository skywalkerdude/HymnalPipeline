package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.Patcher;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import dagger.Module;
import dagger.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Module
public interface HymnalNetPipelineTestModule {

  List<HymnalNetKey> SONGS_TO_FETCH = new ArrayList<>();

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

  @Provides
  @HymnalNetPipelineScope
  static Patcher patcher(@HymnalNet Set<Hymn> hymns) {
    return new Patcher(hymns) {
      @Override
      public void patch() {
        Set<Hymn.Builder> builders = hymns.stream().map(Hymn::toBuilder).collect(Collectors.toSet());

        // Test patcher that clears the languages of hymn 40 and hymn 41
        SongReference h40 = SongReference.newBuilder().setType(HymnType.CLASSIC_HYMN)
            .setNumber("40").build();
        SongReference h41 = SongReference.newBuilder().setType(HymnType.CLASSIC_HYMN)
            .setNumber("41").build();

        builders.stream().filter(hymn -> ImmutableList.of(h40, h41).contains(hymn.getReference()))
            .forEach(
                Hymn.Builder::clearLanguages);

        hymns.clear();
        hymns.addAll(builders.stream().map(Hymn.Builder::build).collect(Collectors.toSet()));
      }
    };
  }
}
