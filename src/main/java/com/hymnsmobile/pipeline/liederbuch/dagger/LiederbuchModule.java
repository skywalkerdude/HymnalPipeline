package com.hymnsmobile.pipeline.liederbuch.dagger;

import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchHymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import nl.siegmann.epublib.epub.EpubReader;

@Module
interface LiederbuchModule {

  String EPUB_PATH = "storage/liederbuch/input/liederbuch_v0.1.1.epub";

  @Liederbuch
  @Provides
  @LiederbuchScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }

  @Provides
  @LiederbuchScope
  static EpubReader reader() {
    return new EpubReader();
  }

  @Provides
  @LiederbuchScope
  static Set<LiederbuchHymn> songs() {
    return new LinkedHashSet<>();
  }
}
