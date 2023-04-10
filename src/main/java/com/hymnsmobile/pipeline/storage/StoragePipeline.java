package com.hymnsmobile.pipeline.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineErrors;
import com.hymnsmobile.pipeline.models.SongReference;
import dagger.Lazy;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.inject.Inject;

public class StoragePipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final DatabaseWriter databaseWriter;
  private final Lazy<File> outputDirectory;
  private final FileReadWriter fileReadWriter;

  @Inject
  public StoragePipeline(DatabaseWriter databaseWriter, Lazy<File> outputDirectory,
      FileReadWriter fileReadWriter) {
    this.databaseWriter = databaseWriter;
    this.fileReadWriter = fileReadWriter;
    this.outputDirectory = outputDirectory;
  }

  public void run(
      ImmutableMap<ImmutableList<SongReference>, Hymn> hymns,
      ImmutableList<PipelineError> errors) throws SQLException, IOException {
    LOGGER.info("Storage pipeline starting");

    writeErrors(errors);

    Connection connection = databaseWriter.createDatabase();
    for (Entry<ImmutableList<SongReference>, Hymn> hymn : hymns.entrySet()) {
      databaseWriter.writeHymn(connection, hymn);
    }
    databaseWriter.closeDatabase(connection);
    LOGGER.info("Storage pipeline starting");
  }

  private void writeErrors(ImmutableList<PipelineError> errors) throws IOException {
    fileReadWriter.writeProto(outputDirectory.get().getPath() + "/errors.txt",
        PipelineErrors.newBuilder().addAllErrors(errors).build());
  }
}
