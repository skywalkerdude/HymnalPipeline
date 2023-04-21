package com.hymnsmobile.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.SanitizationPipeline;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationComponent;
import com.hymnsmobile.pipeline.storage.StoragePipeline;
import com.hymnsmobile.pipeline.storage.dagger.StorageComponent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
  private final SanitizationPipeline sanitizationPipeline;
  private final StoragePipeline storagePipeline;

  @Inject
  Pipeline(
      Provider<HymnalNetPipelineComponent.Builder> hymnalNetPipelineBuilder,
      Provider<H4aPipelineComponent.Builder> h4aPipelineBuilder,
      Provider<LiederbuchPipelineComponent.Builder> liederbuchPipelineBuilder,
      Provider<MergeComponent.Builder> mergePipelineBuilder,
      Provider<SanitizationComponent.Builder> sanitizationPipelineBuilder,
      Provider<StorageComponent.Builder> storagePipelineBuilder) {
    this.hymnalNetPipeline = hymnalNetPipelineBuilder.get().build().pipeline();
    this.h4aPipeline = h4aPipelineBuilder.get().build().pipeline();
    this.liederbuchPipeline = liederbuchPipelineBuilder.get().build().pipeline();
    this.mergePipeline = mergePipelineBuilder.get().build().pipeline();
    this.sanitizationPipeline = sanitizationPipelineBuilder.get().build().pipeline();
    this.storagePipeline = storagePipelineBuilder.get().build().pipeline();
  }

  public void run()
      throws IOException, InterruptedException, URISyntaxException, SQLException, BadHanyuPinyinOutputFormatCombination {
    hymnalNetPipeline.run();
    h4aPipeline.run();
    liederbuchPipeline.run();

    ImmutableMap<ImmutableList<SongReference>, Hymn> allHymns = mergePipeline.mergeHymns(
        hymnalNetPipeline.getHymnalNetJsons(), h4aPipeline.getH4aHymns(),
        liederbuchPipeline.getLiederbuchSong());

    allHymns = sanitizationPipeline.sanitize(allHymns);

    ImmutableList<PipelineError> allErrors = mergePipeline.mergeErrors(
        hymnalNetPipeline.getErrors(), h4aPipeline.getErrors(), sanitizationPipeline.getErrors());
    storagePipeline.run(allHymns, allErrors);
    LOGGER.info("Pipeline completed succesfully");
    System.exit(0);
  }

  public static void main(String[] args)
      throws InterruptedException, IOException, URISyntaxException, SQLException, BadHanyuPinyinOutputFormatCombination, ParserConfigurationException, SAXException {
    DaggerPipelineComponent.create().pipeline().run();
  }
}
