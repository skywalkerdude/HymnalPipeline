package com.hymnsmobile.pipeline.storage;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import javax.inject.Inject;

public class StoragePipeline {

  private final DatabaseWriter databaseWriter;
  private final Set<PipelineError> errors;

  @Inject
  public StoragePipeline(DatabaseWriter databaseWriter, Set<PipelineError> errors) {
    this.databaseWriter = databaseWriter;
    this.errors = errors;
  }

  public void run(ImmutableList<Hymn> hymns) throws SQLException, IOException {
    databaseWriter.createDatabase();
    for (Hymn hymn : hymns) {
      if (!databaseWriter.writeHymn(hymn)) {
        errors.add(PipelineError.newBuilder()
            .setMessage(String.format("%s failed to write to database", hymn.getReference()))
            .setSeverity(Severity.ERROR).build());
      }
    }
    databaseWriter.closeDatabase();
  }
}
