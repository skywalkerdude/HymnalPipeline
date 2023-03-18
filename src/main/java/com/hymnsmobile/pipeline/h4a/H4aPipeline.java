package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import java.util.logging.Logger;
import javax.inject.Inject;

@H4aPipelineScope
public class H4aPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  @Inject
  public H4aPipeline() {
  }
}
