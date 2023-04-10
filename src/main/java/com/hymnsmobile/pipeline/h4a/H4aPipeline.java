package com.hymnsmobile.pipeline.h4a;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.h4a.dagger.H4a;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineScope;
import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import com.hymnsmobile.pipeline.models.PipelineError;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

@H4aPipelineScope
public class H4aPipeline {

  private static final Logger LOGGER = Logger.getGlobal();

  private final Reader reader;
  private final Set<PipelineError> errors;
  private final Set<H4aHymn> h4aHymns;

  @Inject
  public H4aPipeline(
      @H4a Set<PipelineError> errors,
      Set<H4aHymn> h4aHymns,
      Reader reader,
      ZonedDateTime currentTime) {
    this.errors = errors;
    this.h4aHymns = h4aHymns;
    this.reader = reader;
  }

  public ImmutableList<H4aHymn> getH4aHymns() {
    return ImmutableList.copyOf(h4aHymns);
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void run() throws BadHanyuPinyinOutputFormatCombination, SQLException {
    LOGGER.info("H4a pipeline starting");
    reader.readDb();
    fixGermanSongs();
    LOGGER.info("H4a pipeline finished");
  }

  /**
   * The numbering system for German songs in H4A is different from the numbering system in hymnal
   * db. This results in situations like E15, which is linked to de/15 in hymnal db, but is linked
   * to G10 in H4a. If we were to continue processing without rectifying this, there will be
   * collisions down the road. In our example, G15 is linked to E21, but we can't add G15 into the
   * database because it's already taken by de/15.
   * <p>
   * This code basically goes through each German song and rekeys it to match its parent. So for
   * instance, G10 will be changed to G15 since its parent is E15.
   */
  private void fixGermanSongs() {
    ImmutableList<H4aHymn.Builder> builders =
        h4aHymns.stream()
            .map(H4aHymn::toBuilder)
            .collect(toImmutableList());

    for (H4aHymn.Builder hymn : builders) {
      H4aKey hymnKey = hymn.getId();
      HymnType hymnType = HymnType.fromString(hymnKey.getType()).orElseThrow();
      String hymnNumber = hymnKey.getNumber();

      // Not a German song, so skip.
      if (hymnType != HymnType.GERMAN) {
        continue;
      }

      // No parent hymn, so no way to reconcile
      if (!hymn.hasParentHymn()) {
        continue;
      }

      H4aKey parentKey = hymn.getParentHymn();
      HymnType parentType = HymnType.fromString(parentKey.getType()).orElseThrow();
      String parentNumber = parentKey.getNumber();
      H4aHymn.Builder parentHymn = getHymnFrom(parentKey, builders).orElseThrow();
      // number is already correct
      if (parentType == HymnType.CLASSIC_HYMN && hymnNumber.equals(parentNumber)) {
        continue;
      }
      // number is already correct
      if (parentType == HymnType.NEW_SONG && hymnNumber.equals(parentNumber + "de")) {
        continue;
      }

      if (parentType != HymnType.CLASSIC_HYMN) {
        throw new IllegalArgumentException(
            hymnKey + "'s parent is not a classic hymn: " + parentKey);
      }

      H4aKey newId = H4aKey.newBuilder().setType(HymnType.GERMAN.abbreviation)
          .setNumber(parentNumber).build();
      hymn.setId(newId);
      parentHymn.removeRelated(parentHymn.getRelatedList().indexOf(hymnKey));
      parentHymn.addRelated(newId);
    }

    // audit top make sure there are no duplicate ID after fixing all the German songs.
    builders.stream().map(H4aHymn.Builder::getId)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .forEach((key, value) -> {
          if (value > 1) {
            throw new IllegalStateException("Duplicate keys found: " + key);
          }
        });

    h4aHymns.clear();
    h4aHymns.addAll(builders.stream().map(H4aHymn.Builder::build).collect(Collectors.toSet()));
  }

  private Optional<H4aHymn.Builder> getHymnFrom(H4aKey key, ImmutableList<H4aHymn.Builder> hymns) {
    Set<H4aHymn.Builder> possibilities = hymns.stream()
        .filter(hymn -> hymn.getId().equals(key))
        .collect(Collectors.toSet());
    if (possibilities.size() > 1) {
      throw new IllegalStateException(
          "There should be at most be one hymn matching each reference.");
    }
    return possibilities.stream().findFirst();
  }
}
