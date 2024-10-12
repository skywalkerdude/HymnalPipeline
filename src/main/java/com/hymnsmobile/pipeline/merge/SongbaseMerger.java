package com.hymnsmobile.pipeline.merge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.merge.Converter.CHORDS_PATTERN;
import static com.hymnsmobile.pipeline.merge.Utilities.getHymnFrom;

/**
 * Class that merges Songbase songs into the pipeline results.
 */
@MergeScope
public class SongbaseMerger {

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

      if (matchingReference.size() != 1) {
        throw new IllegalStateException("Wrong number of matching references");
      }
      // Check to see if there are mismatched languages
      Set<HymnLanguage> matchingReferenceLanguages =
          matchingReference.get(0).getReferencesList().stream().map(converter::getLanguage).collect(Collectors.toSet());
      Set<HymnLanguage> songbaseLanguages =
          songbaseBuilder.getReferencesList().stream().map(converter::getLanguage).collect(Collectors.toSet());
      if (Sets.intersection(matchingReferenceLanguages, songbaseLanguages).size() != 1) {
        errors.add(PipelineError.newBuilder()
                .setSeverity(PipelineError.Severity.ERROR)
                .setErrorType(PipelineError.ErrorType.DUPLICATE_LANGUAGE_MISMATCH)
                .setSource(PipelineError.Source.SONGBASE)
                .addMessages(songbaseBuilder.getReferencesList().toString())
                .addMessages(matchingReference.get(0).getReferencesList().toString())
                .build());
      }

      // Add the new references
      songbaseBuilder.getReferencesList().stream()
          .filter(reference -> !matchingReference.get(0).getReferencesList().contains(reference))
          .forEach(reference -> matchingReference.get(0).addReferences(reference));
      // Set inline chords property only if there are chords found in the song
      if (Pattern.compile(CHORDS_PATTERN).matcher(songbaseHymn.getLyrics()).find()) {
        matchingReference.get(0).addAllInlineChords(songbaseBuilder.getInlineChordsList());
      }
    });
    return builders.stream().map(Hymn.Builder::build).collect(toImmutableList());
  }
}
