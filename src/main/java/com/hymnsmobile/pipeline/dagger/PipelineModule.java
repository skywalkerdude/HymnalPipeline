package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

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

  @PipelineScope
  @Provides
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}
