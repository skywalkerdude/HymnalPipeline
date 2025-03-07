package com.hymnsmobile.pipeline.storage;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.FileReadWriter;
import com.hymnsmobile.pipeline.models.DuplicationResults;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineErrors;
import com.hymnsmobile.pipeline.storage.models.HymnEntity;
import dagger.Lazy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class StoragePipeline {

  private static final Logger LOGGER = Logger.getGlobal();
  private static final String DATABASE_PATH_FORMAT = "jdbc:sqlite:%s/hymnaldb-v%d.sqlite";
  private static final String EXPANDED_DATABASE_PATH_FORMAT = "jdbc:sqlite:%s/hymnaldb-v%d-expanded.sqlite";
  public static final int DATABASE_VERSION = 30;

  private final Converter converter;
  private final DatabaseWriter databaseWriter;
  private final Lazy<File> outputDirectory;
  private final FileReadWriter fileReadWriter;

  @Inject
  public StoragePipeline(Converter converter, DatabaseWriter databaseWriter, Lazy<File> outputDirectory,
      FileReadWriter fileReadWriter) {
    this.converter = converter;
    this.databaseWriter = databaseWriter;
    this.fileReadWriter = fileReadWriter;
    this.outputDirectory = outputDirectory;
  }

  public void run(ImmutableList<Hymn> hymns, ImmutableList<PipelineError> errors, DuplicationResults duplicationResults)
      throws SQLException, IOException {
    LOGGER.info("Storage pipeline starting");

    writeErrors(errors);
    writeDuplicationResults(duplicationResults);

    String databasePath = String.format(DATABASE_PATH_FORMAT, outputDirectory.get().getPath(), DATABASE_VERSION);
    Connection connection = databaseWriter.createDatabase(databasePath, true);

    String expandedDatabasePath =
        String.format(EXPANDED_DATABASE_PATH_FORMAT, outputDirectory.get().getPath(), DATABASE_VERSION);
    Connection expandedConnection = databaseWriter.createDatabase(expandedDatabasePath, false);

    for (Hymn hymn : hymns) {
      HymnEntity entity = converter.convert(hymn);
      databaseWriter.writeHymn(connection, entity, true);
      databaseWriter.writeHymn(expandedConnection, entity, false);
    }

    databaseWriter.closeDatabase(connection);
    databaseWriter.closeDatabase(expandedConnection);

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
