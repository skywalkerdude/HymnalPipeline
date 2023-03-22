package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import dagger.Module;
import dagger.Provides;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Module(subcomponents = HymnalNetPipelineComponent.class)
public interface PipelineModule {

  @Provides
  static HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }

  @Provides
  static ZonedDateTime currentTime() {
    return LocalDateTime.now().atZone(ZoneId.of("America/Los_Angeles"));
  }
}
