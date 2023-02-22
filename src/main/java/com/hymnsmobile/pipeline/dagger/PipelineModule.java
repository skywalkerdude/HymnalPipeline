package com.hymnsmobile.pipeline.dagger;

import dagger.Module;
import dagger.Provides;
import java.net.http.HttpClient;

@Module
interface PipelineModule {

  @Provides
  static HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }
}
