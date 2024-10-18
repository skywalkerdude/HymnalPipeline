package com.hymnsmobile.pipeline.dedup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hymnsmobile.pipeline.dedup.dagger.Dedup;
import com.hymnsmobile.pipeline.dedup.dagger.DedupScope;
import com.hymnsmobile.pipeline.models.*;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DedupScope
public class DedupPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final LevenshteinDistance levenshteinDistance;
  private final Set<PipelineError> errors;

  @Inject
  public DedupPipeline(LevenshteinDistance levenshteinDistance,
                       @Dedup Set<PipelineError> errors) {
    this.errors = errors;
    this.levenshteinDistance = levenshteinDistance;
  }

  public DuplicationResults run(ImmutableList<Hymn> hymns) {
    LOGGER.info("Deduplication starting");
    DuplicationResults.Builder builder =
        DuplicationResults.newBuilder()
            .setNoDifference(DuplicationResult.newBuilder().setCount(0))
            .setUnder5(DuplicationResult.newBuilder().setCount(0))
            .setUnder10(DuplicationResult.newBuilder().setCount(0))
            .setUnder50(DuplicationResult.newBuilder().setCount(0));
    for (Hymn hymn1 : hymns) {
      Hymn duplicateHymn = null;
      int leastDistance = Integer.MAX_VALUE;
      for (Hymn hymn2 : hymns) {
        // Skip if they are the exact same song.
        if (hymn1 == hymn2) {
          continue;
        }

        // Different languages are automatically not duplicates.
        if (hymn1.getLanguage() != hymn2.getLanguage()) {
          continue;
        }

        // Only do English for now
        if (hymn1.getLanguage() != Language.ENGLISH) {
          continue;
        }

        // If a song is a relevant song (i.e. New tune or Alternate tune), then we should skip it because, even though
        // they likely have 0 levenshtein distance, they are still not duplicates.
        if (!Sets.intersection(new HashSet<>(hymn1.getRelevantsList()), new HashSet<>(hymn2.getReferencesList())).isEmpty() &&
            !Sets.intersection(new HashSet<>(hymn2.getRelevantsList()), new HashSet<>(hymn1.getReferencesList())).isEmpty()) {
          continue;
        }

        int distance = levenshteinDistance.apply(hymn1.getFlattenedLyrics(), hymn2.getFlattenedLyrics());
        if (distance < leastDistance) {
          leastDistance = distance;
          duplicateHymn = hymn2;
        }
      }

      DuplicationResult.Builder duplicationResult = null;
      if (leastDistance < 50) {
        duplicationResult = builder.getUnder50Builder();
      }
      if (leastDistance < 10) {
        duplicationResult = builder.getUnder10Builder();
      }
      if (leastDistance < 5) {
        duplicationResult = builder.getUnder5Builder();
      }
      if (leastDistance == 0) {
        duplicationResult = builder.getNoDifferenceBuilder();
      }
      if (duplicationResult == null) {
        continue;
      }

      Duplication duplication =
          Duplication.newBuilder()
              .setDistance(leastDistance)
              .addAllReference1(hymn1.getReferencesList())
              .addAllReference2(duplicateHymn.getReferencesList())
              .setHymn1(hymn1)
              .setHymn2(duplicateHymn)
              .build();

      // Make sure the duplication doesn't already exist
      List<Set<SongReference>> existingReferences =
          duplicationResult
              .getDuplicationsList()
              .stream()
              .map(d -> Stream.concat(d.getReference1List().stream(),
                                      d.getReference2List().stream())
                              .collect(Collectors.toSet()))
              .toList();
      if (existingReferences.contains(
          Stream.concat(hymn1.getReferencesList().stream(),
                        duplicateHymn.getReferencesList().stream())
                .collect(Collectors.toSet()))) {
        continue;
      }

      duplicationResult.setCount(duplicationResult.getCount() + 1);
      duplicationResult.addDuplications(duplication);
    }

    builder.getUnder5Builder()
           .getDuplicationsList()
           .sort(Comparator.comparingInt(Duplication::getDistance));
    builder.getUnder10Builder()
           .getDuplicationsList()
           .sort(Comparator.comparingInt(Duplication::getDistance));
    builder.getUnder50Builder()
           .getDuplicationsList()
           .sort(Comparator.comparingInt(Duplication::getDistance));

    LOGGER.info("Deduplication completed");
    return builder.build();
  }

  public final ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }
}
