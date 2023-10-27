package com.hymnsmobile.pipeline;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.dagger.PipelineScope;
import com.hymnsmobile.pipeline.h4a.H4aPipeline;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import com.hymnsmobile.pipeline.liederbuch.LiederbuchPipeline;
import com.hymnsmobile.pipeline.liederbuch.dagger.LiederbuchPipelineComponent;
import com.hymnsmobile.pipeline.merge.MergePipeline;
import com.hymnsmobile.pipeline.merge.dagger.MergeComponent;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.russian.RussianPipeline;
import com.hymnsmobile.pipeline.russian.dagger.RussianPipelineComponent;
import com.hymnsmobile.pipeline.songbase.SongbasePipeline;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineComponent;
import com.hymnsmobile.pipeline.storage.StoragePipeline;
import com.hymnsmobile.pipeline.storage.dagger.StorageComponent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.parsers.ParserConfigurationException;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.xml.sax.SAXException;

@PipelineScope
public class Pipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final HymnalNetPipeline hymnalNetPipeline;
  private final H4aPipeline h4aPipeline;
  private final LiederbuchPipeline liederbuchPipeline;
  private final MergePipeline mergePipeline;
  private final RussianPipeline russianPipeline;
  private final SongbasePipeline songbasePipeline;
  private final StoragePipeline storagePipeline;

  @Inject
  Pipeline(
      Provider<HymnalNetPipelineComponent.Builder> hymnalNetPipelineBuilder,
      Provider<H4aPipelineComponent.Builder> h4aPipelineBuilder,
      Provider<LiederbuchPipelineComponent.Builder> liederbuchPipelineBuilder,
      Provider<MergeComponent.Builder> mergePipelineBuilder,
      Provider<RussianPipelineComponent.Builder> russianPipelineComponent,
      Provider<SongbasePipelineComponent.Builder> songbasePipelineComponentBuilder,
      Provider<StorageComponent.Builder> storagePipelineBuilder) {
    this.hymnalNetPipeline = hymnalNetPipelineBuilder.get().build().pipeline();
    this.h4aPipeline = h4aPipelineBuilder.get().build().pipeline();
    this.liederbuchPipeline = liederbuchPipelineBuilder.get().build().pipeline();
    this.mergePipeline = mergePipelineBuilder.get().build().pipeline();
    this.russianPipeline = russianPipelineComponent.get().build().pipeline();
    this.songbasePipeline = songbasePipelineComponentBuilder.get().build().pipeline();
    this.storagePipeline = storagePipelineBuilder.get().build().pipeline();
  }

  public void run()
      throws IOException, InterruptedException, SQLException, BadHanyuPinyinOutputFormatCombination {
    hymnalNetPipeline.run();
    h4aPipeline.run();
    liederbuchPipeline.run();
    songbasePipeline.run();
    russianPipeline.run();

    ImmutableList<Hymn> mergedHymns =
        mergePipeline.convertHymnalNet(hymnalNetPipeline.getHymnalNetJsons());
    mergedHymns = mergePipeline.mergeH4a(h4aPipeline.getH4aHymns(), mergedHymns);
    mergedHymns = mergePipeline.mergeLiederbuch(liederbuchPipeline.getLiederbuchSong(), mergedHymns);
    mergedHymns = mergePipeline.mergeRussian(russianPipeline.getRussianHymns(), mergedHymns);
    mergedHymns = mergePipeline.mergeSongbase(songbasePipeline.getSongbaseHymns(), mergedHymns);

    ImmutableList<PipelineError> allErrors = mergePipeline.mergeErrors(
        hymnalNetPipeline.getErrors(), h4aPipeline.getErrors(), mergePipeline.getErrors(),
        songbasePipeline.getErrors());
    storagePipeline.run(mergedHymns, allErrors);
    LOGGER.info("Pipeline completed successfully");
    System.exit(0);
  }

  public static void main(String[] args)
      throws InterruptedException, IOException, URISyntaxException, SQLException, BadHanyuPinyinOutputFormatCombination, ParserConfigurationException, SAXException {
    DaggerPipelineComponent.create().pipeline().run();
  }
}
