package com.hymnsmobile.pipeline;

import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.dagger.PipelineScope;
import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import com.hymnsmobile.pipeline.storage.StoragePipeline;
import com.hymnsmobile.pipeline.storage.dagger.StorageComponent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Provider;

@PipelineScope
public class Pipeline {

  private final HymnalNetPipeline hymnalNetPipeline;
  private final StoragePipeline storagePipeline;

  @Inject
  Pipeline(Provider<HymnalNetPipelineComponent.Builder> hymnalNetPipelineBuilder,
           Provider<StorageComponent.Builder> storagePipelineBuilder) {
    this.hymnalNetPipeline = hymnalNetPipelineBuilder.get().build().pipeline();
    this.storagePipeline = storagePipelineBuilder.get().build().storagePipeline();
  }

  public void run() throws IOException, InterruptedException, URISyntaxException, SQLException {
    // Logger.getGlobal()
    //     .setFilter(record -> record.getLevel().intValue() > Level.ERROR.getSeverity());
    hymnalNetPipeline.run();
    storagePipeline.run(hymnalNetPipeline.getHymns());
  }

  public static void main(String[] args)
      throws InterruptedException, IOException, URISyntaxException, SQLException {
    DaggerPipelineComponent.create().pipeline().run();
  }
}
