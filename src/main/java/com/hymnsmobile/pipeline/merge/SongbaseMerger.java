package com.hymnsmobile.pipeline.merge;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.Utilities.getHymnFrom;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Class that merges Songbase songs into the pipeline results.
 */
@MergeScope
public class SongbaseMerger {

  private static final String CHORDS_PATTERN = "\\[(.*?)]";

  private final Converter converter;
  private final Set<PipelineError> errors;

  @Inject
  public SongbaseMerger(Converter converter, @Merge Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
  }

  public ImmutableList<Hymn> merge(
      ImmutableList<SongbaseHymn> songbaseHymns, ImmutableList<Hymn> mergedHymns) {
    List<Hymn.Builder> builders =
        mergedHymns.stream().map(Hymn::toBuilder).collect(Collectors.toList());

    songbaseHymns.forEach(songbaseHymn -> {
      Hymn.Builder songbaseBuilder =
          converter.toHymn(songbaseHymn).toBuilder()
              .addProvenance("songbase");

      // Find a hymn that already matches one of the songbase song's references, if it exists
      ImmutableList<Hymn.Builder> matchingReference =
          songbaseBuilder.getReferencesList().stream()
              .map(reference -> getHymnFrom(reference, builders))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(toImmutableList());
      if (matchingReference.isEmpty()) {
        // No matching references, so add the songbase song
        builders.add(songbaseBuilder);
        return;
      }

      assert matchingReference.size() == 1;
      // Add the new references
      songbaseBuilder.getReferencesList().stream()
          .filter(reference -> !matchingReference.get(0).getReferencesList().contains(reference))
          .forEach(reference -> matchingReference.get(0).addReferences(reference));
      // Set inline chords property only if there are chords found in the song
      if (Pattern.compile(CHORDS_PATTERN).matcher(songbaseHymn.getLyrics()).find()) {
        matchingReference.get(0).setInlineChords(songbaseBuilder.getInlineChords());
      }
    });
    return builders.stream().map(Hymn.Builder::build).collect(toImmutableList());
  }
}
