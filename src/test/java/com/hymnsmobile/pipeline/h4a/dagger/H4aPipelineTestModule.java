package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Module
public interface H4aPipelineTestModule {

  /**
   * Override the miscBlockList in tests.
   */
  List<String> overrideMiscBlockList = new ArrayList<>();

  /**
   * Override the nonExistentRelatedSongs in tests.
   */
  List<String> overrideNonExistentRelatedSongs = new ArrayList<>();

  /**
   * Override the oneOffCases in tests.
   */
  List<String> overrideOneOffCases = new ArrayList<>();

  @Provides
  @H4aPipelineScope
  static Set<H4aHymn> h4aHymns() {
    return H4aPipelineModule.h4aHymns();
  }

  @H4a
  @Provides
  @H4aPipelineScope
  static Set<PipelineError> errors() {
    return H4aPipelineModule.errors();
  }

  /**
   * Songs that are blocked for miscellaneous reasons.
   */
  @MiscBlockList
  @Provides
  @H4aPipelineScope
  static List<String> miscBlockList() {
    if (!overrideMiscBlockList.isEmpty()) {
      return overrideMiscBlockList;
    }
    return H4aPipelineModule.miscBlockList();
  }

  /**
   * Songs that show up in "related" column but don't actually exist in the h4a db. These should be ignored since they
   * map to nothing.
   */
  @NonExistentRelatedSongs
  @Provides
  @H4aPipelineScope
  static List<String> nonExistentRelatedSongs() {
    if (!overrideNonExistentRelatedSongs.isEmpty()) {
      return overrideNonExistentRelatedSongs;
    }
    return H4aPipelineModule.nonExistentRelatedSongs();
  }

  /**
   * Songs that require special one-off attention.
   */
  @OneOff
  @Provides
  @H4aPipelineScope
  static List<String> oneOffCases() {
    if (!overrideOneOffCases.isEmpty()) {
      return overrideOneOffCases;
    }
    return H4aPipelineModule.oneOffCases();
  }
}
