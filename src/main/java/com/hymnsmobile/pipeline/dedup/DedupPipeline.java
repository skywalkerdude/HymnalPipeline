package com.hymnsmobile.pipeline.dedup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hymnsmobile.pipeline.dedup.dagger.Dedup;
import com.hymnsmobile.pipeline.dedup.dagger.DedupScope;
import com.hymnsmobile.pipeline.models.*;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.inject.Inject;
import java.util.*;
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
    List<Duplication> noDifference = new ArrayList<>();
    List<Duplication> under5 = new ArrayList<>();
    List<Duplication> under10 = new ArrayList<>();
    List<Duplication> under50 = new ArrayList<>();
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
// TODO 25 is a good threshold
      List<Duplication> listToAddTo = null;
      if (leastDistance < 50) {
        listToAddTo = under50;
      }
      if (leastDistance < 10) {
        listToAddTo = under10;
      }
      if (leastDistance < 5) {
        listToAddTo = under5;
      }
      if (leastDistance == 0) {
        listToAddTo = noDifference;
      }
      if (listToAddTo == null) {
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
          listToAddTo.stream()
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

      listToAddTo.add(duplication);
    }

    under5.sort(Comparator.comparingInt(Duplication::getDistance));
    under10.sort(Comparator.comparingInt(Duplication::getDistance));
    under50.sort(Comparator.comparingInt(Duplication::getDistance));

    builder.getNoDifferenceBuilder().setCount(noDifference.size()).addAllDuplications(noDifference);
    builder.getUnder5Builder().setCount(under5.size()).addAllDuplications(under5);
    builder.getUnder10Builder().setCount(under10.size()).addAllDuplications(under10);
    builder.getUnder50Builder().setCount(under50.size()).addAllDuplications(under50);

    LOGGER.info("Deduplication completed");
    return builder.build();
  }

  public final ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }
}
