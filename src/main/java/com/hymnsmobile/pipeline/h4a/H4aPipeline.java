package com.hymnsmobile.pipeline.h4a;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.h4a.dagger.H4a;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

@H4aPipelineScope
public class H4aPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Reader reader;
  private final Set<PipelineError> errors;
  private final Set<H4aHymn> h4aHymns;

  @Inject
  public H4aPipeline(
      @H4a Set<PipelineError> errors,
      Set<H4aHymn> h4aHymns,
      Reader reader) {
    this.errors = errors;
    this.h4aHymns = h4aHymns;
    this.reader = reader;
  }

  public ImmutableList<H4aHymn> getH4aHymns() {
    return ImmutableList.copyOf(h4aHymns);
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void run() throws BadHanyuPinyinOutputFormatCombination, SQLException {
    LOGGER.info("H4a pipeline starting");
    reader.readDb();
    LOGGER.info("H4a pipeline finished");
  }

  public static void main(String[] args) throws BadHanyuPinyinOutputFormatCombination, SQLException {
    DaggerPipelineComponent.create().h4aComponent().build().pipeline().run();
  }
}
