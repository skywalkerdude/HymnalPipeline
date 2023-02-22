package com.hymnsmobile.pipeline;

import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.dagger.PipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import java.io.IOException;

public class Pipeline {

  public static void main(String[] args) throws InterruptedException, IOException {
    PipelineComponent component = DaggerPipelineComponent.create();

    HymnalNetPipeline hymnalNetPipeline = component.hymnalNetPipeline();
    hymnalNetPipeline.run();
    System.out.println(hymnalNetPipeline.errors());
  }
}