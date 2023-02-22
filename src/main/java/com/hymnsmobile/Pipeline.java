package com.hymnsmobile;

import com.hymnsmobile.common.dagger.DaggerPipelineComponent;
import com.hymnsmobile.common.dagger.PipelineComponent;
import com.hymnsmobile.hymnalnet.HymnalNetPipeline;
import java.io.IOException;

public class Pipeline {

  public static void main(String[] args) throws InterruptedException, IOException {
    PipelineComponent component = DaggerPipelineComponent.create();

    HymnalNetPipeline hymnalNetPipeline = component.hymnalNetPipeline();
    hymnalNetPipeline.run();
    System.out.println(hymnalNetPipeline.errors());
  }
}