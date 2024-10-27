package com.hymnsmobile.pipeline.dedup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hymnsmobile.pipeline.dedup.dagger.Dedup;
import com.hymnsmobile.pipeline.dedup.dagger.DedupScope;
import com.hymnsmobile.pipeline.merge.HymnType;
import com.hymnsmobile.pipeline.models.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

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

  public Pair<ImmutableList<Hymn>, DuplicationResults> run(ImmutableList<Hymn> hymns) {
    LOGGER.info("Deduplication starting");
    List<Hymn.Builder> builders = new ArrayList<>(hymns.stream().map(Hymn::toBuilder).toList());
    DuplicationResults.Builder duplicationResults =
        DuplicationResults.newBuilder()
            .setNoDifference(DuplicationResult.newBuilder().setCount(0))
            .setUnder5(DuplicationResult.newBuilder().setCount(0))
            .setUnder10(DuplicationResult.newBuilder().setCount(0))
            .setUnder50(DuplicationResult.newBuilder().setCount(0));
    List<Duplication> noDifference = new ArrayList<>();
    List<Duplication> under5 = new ArrayList<>();
    List<Duplication> under10 = new ArrayList<>();
    List<Duplication> under50 = new ArrayList<>();

    // Make a shallow copy because we may change the list from within the loop.
    List<Hymn.Builder> copy = new ArrayList<>(builders);
    for (int i = 0; i < copy.size(); i++) {
      Hymn.Builder hymn1 = copy.get(i);
      // Hymn has been removed due to being a duplicate.
      if (!builders.contains(hymn1)) {
        continue;
      }

      Hymn.Builder duplicateHymn = null;
      int leastDistance = Integer.MAX_VALUE;
      for (int j = i + 1; j < copy.size(); j++) {
        Hymn.Builder hymn2 = copy.get(j);

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

        // Hymn has been removed due to being a duplicate.
        if (!builders.contains(hymn2)) {
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

      // Make sure the duplication doesn't already exist
      List<Set<SongReference>> existingReferences =
          listToAddTo.stream()
                     .map(d -> Stream.concat(d.getSong1().getReferencesList().stream(),
                                             d.getSong2().getReferencesList().stream())
                                     .collect(Collectors.toSet()))
                     .toList();
      if (existingReferences.contains(
          Stream.concat(hymn1.getReferencesList().stream(),
                        duplicateHymn.getReferencesList().stream())
                .collect(Collectors.toSet()))) {
        continue;
      }

      Duplication duplication =
          Duplication.newBuilder()
              .setDistance(leastDistance)
              .setSong1(DuplicateSong.newBuilder()
                      .setSongId(hymn1.getId())
                      .addAllReferences(hymn1.getReferencesList())
                      .setLanguage(hymn1.getLanguage())
                      .setFlattenedLyrics(hymn1.getFlattenedLyrics())
                      .addAllExemptions(hymn1.getRelevantsList()))
              .setSong2(DuplicateSong.newBuilder()
                      .setSongId(duplicateHymn.getId())
                      .addAllReferences(duplicateHymn.getReferencesList())
                      .setLanguage(duplicateHymn.getLanguage())
                      .setFlattenedLyrics(duplicateHymn.getFlattenedLyrics())
                      .addAllExemptions(duplicateHymn.getRelevantsList()))
              .build();
      listToAddTo.add(duplication);

      if (leastDistance <= 25) {
        merge(hymn1, duplicateHymn, builders);
      }
    }

    under5.sort(Comparator.comparingInt(Duplication::getDistance));
    under10.sort(Comparator.comparingInt(Duplication::getDistance));
    under50.sort(Comparator.comparingInt(Duplication::getDistance));

    duplicationResults.getNoDifferenceBuilder().setCount(noDifference.size()).addAllDuplications(noDifference);
    duplicationResults.getUnder5Builder().setCount(under5.size()).addAllDuplications(under5);
    duplicationResults.getUnder10Builder().setCount(under10.size()).addAllDuplications(under10);
    duplicationResults.getUnder50Builder().setCount(under50.size()).addAllDuplications(under50);

    LOGGER.info("Deduplication completed");

    return Pair.of(builders.stream().map(Hymn.Builder::build).collect(toImmutableList()), duplicationResults.build());
  }

  private void merge(Hymn.Builder hymn1, Hymn.Builder hymn2, List<Hymn.Builder> builders) {
    int hymn1Priority =
        hymn1.getReferencesList().stream()
             .map(SongReference::getHymnType)
             .map(HymnType::fromString)
             .map(this::getPriority)
             .reduce(Integer.MIN_VALUE, Integer::max);
    int hymn2Priority =
        hymn2.getReferencesList().stream()
             .map(SongReference::getHymnType)
             .map(HymnType::fromString)
             .map(this::getPriority)
             .reduce(Integer.MIN_VALUE, Integer::max);
    if (hymn1Priority > hymn2Priority) {
      hymn1.addAllReferences(hymn2.getReferencesList());
      builders.remove(hymn2);
    } else if (hymn2Priority > hymn1Priority) {
      hymn2.addAllReferences(hymn1.getReferencesList());
      builders.remove(hymn1);
    }
  }

  /**
   * Prioritizes all the hymn types, with the highest number being the highest priority.
   */
  private int getPriority(HymnType hymnType) {
    switch (hymnType) {
      case CLASSIC_HYMN -> {
        return 100;
      }
      case NEW_SONG -> {
        return 80;
      }
      case HOWARD_HIGASHI -> {
        return 70;
      }
      case CHILDREN_SONG -> {
        return 60;
      }
      case NEW_TUNE -> {
        return 50;
      }
      case BLUE_SONGBOOK -> {
        return 40;
      }
      case SONGBASE_OTHER -> {
        return 30;
      }
      case BE_FILLED -> {
        return 20;
      }
      default -> {
        return 0;
      }
    }
  }

  public final ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }
}
