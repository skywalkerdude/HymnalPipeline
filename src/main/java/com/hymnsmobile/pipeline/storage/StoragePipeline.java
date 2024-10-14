package com.hymnsmobile.pipeline.storage;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.models.*;
import dagger.Lazy;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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

  public void run(ImmutableList<Hymn> hymns, ImmutableList<PipelineError> errors, DuplicationResults duplicationResults)
      throws SQLException, IOException {
    LOGGER.info("Storage pipeline starting");

    writeErrors(errors);
    writeDuplicationResults(duplicationResults);

    Connection connection = databaseWriter.createDatabase();
    for (Hymn hymn : hymns) {
      databaseWriter.writeHymn(connection, hymn);
    }
    databaseWriter.closeDatabase(connection);
    LOGGER.info("Storage pipeline starting");
  }

  private void writeErrors(ImmutableList<PipelineError> errors) throws IOException {
    fileReadWriter.writeString(outputDirectory.get().getPath() + "/errors.textproto",
        PipelineErrors.newBuilder().setCount(errors.size()).addAllErrors(errors).build()
            .toString());
  }

  private void writeDuplicationResults(DuplicationResults duplicationResults) throws IOException {
    fileReadWriter.writeString(outputDirectory.get().getPath() + "/duplications.textproto",
                               duplicationResults.toString());
  }
}
