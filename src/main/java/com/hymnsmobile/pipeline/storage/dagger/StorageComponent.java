package com.hymnsmobile.pipeline.storage.dagger;

import com.hymnsmobile.pipeline.storage.StoragePipeline;
import dagger.Subcomponent;

@StorageScope
@Subcomponent(modules = StorageModule.class)
public interface StorageComponent {

  StoragePipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    StorageComponent build();
  }
}
