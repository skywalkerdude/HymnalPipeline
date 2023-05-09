package com.hymnsmobile.pipeline.russian.dagger;

import com.hymnsmobile.pipeline.russian.RussianHymn;
import dagger.Module;
import dagger.Provides;
import java.util.LinkedHashSet;
import java.util.Set;

@Module
interface RussianModule {

  @Provides
  @RussianScope
  static Set<RussianHymn> songs() {
    return new LinkedHashSet<>();
  }
}
