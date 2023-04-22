package com.hymnsmobile.pipeline.songbase;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.dagger.Songbase;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

@SongbasePipelineScope
public class SongbasePipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Converter converter;
  private final Fetcher fetcher;
  private final Set<SongbaseHymn> songbaseHymns;
  private final Set<PipelineError> errors;
  private final Writer writer;

  @Inject
  public SongbasePipeline(
      Converter converter,
      Fetcher fetcher,
      Set<SongbaseHymn> songbaseHymns,
      Writer writer,
      @Songbase Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
    this.fetcher = fetcher;
    this.songbaseHymns = songbaseHymns;
    this.writer = writer;
  }

  public ImmutableList<SongbaseHymn> getSongbaseHymns() {
    return ImmutableList.copyOf(songbaseHymns);
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void run() throws IOException, InterruptedException {
    LOGGER.info("Songbase pipeline starting");
    String response = fetcher.fetch();
    songbaseHymns.addAll(converter.convert(response));
    writer.write(response);
    LOGGER.info("Songbase pipeline finished");
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    DaggerPipelineComponent.create().songbasePipelineComponent().build().pipeline().run();
  }
}
