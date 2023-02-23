package com.hymnsmobile.pipeline;

import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.dagger.PipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.inject.Provider;

@PipelineScope
public class Pipeline {

  private final HymnalNetPipeline hymnalNetPipeline;

  @Inject
  Pipeline(Provider<HymnalNetPipelineComponent.Builder> hymnalNetPipelineBuilder) {
    this.hymnalNetPipeline = hymnalNetPipelineBuilder.get().build().pipeline();
  }

  public void run() throws IOException, InterruptedException, URISyntaxException {
    hymnalNetPipeline.run();
  }

  public static void main(String[] args)
      throws InterruptedException, IOException, URISyntaxException {
    DaggerPipelineComponent.create().pipeline().run();
  }
}