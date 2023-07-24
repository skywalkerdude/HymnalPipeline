package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.List;
import java.util.Optional;

public class Utilities {

  /**
   * Gets the hymn that matches the passed-in song reference.
   */
  static Optional<Hymn.Builder> getHymnFrom(
      SongReference songReference, List<Hymn.Builder> mergedHymns) {
    ImmutableList<Hymn.Builder> connections =
        mergedHymns.stream()
            .filter(hymn -> hymn.getReferencesList().contains(songReference))
            .collect(toImmutableList());

    if (connections.isEmpty()) {
      return Optional.empty();
    }
    if (connections.size() > 1) {
      throw new IllegalStateException(
          "There should be at most be one hymn matching each reference.");
    }
    return Optional.of(connections.get(0));
  }
}
