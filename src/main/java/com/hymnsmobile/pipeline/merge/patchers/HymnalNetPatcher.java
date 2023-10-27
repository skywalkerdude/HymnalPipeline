package com.hymnsmobile.pipeline.merge.patchers;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.hymnsmobile.pipeline.merge.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.merge.HymnType.DUTCH;
import static com.hymnsmobile.pipeline.merge.HymnType.HOWARD_HIGASHI;
import static com.hymnsmobile.pipeline.merge.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.merge.HymnType.PORTUGUESE;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.HymnType;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

/**
 * Performs one-off patches to the set of hymns from Hymnal.net that are unfixable with a general
 * algorithm.
 */
@MergeScope
public class HymnalNetPatcher extends Patcher {

  /**
   * When the song on Hymnal.net is so wrong that we just have to purge it. As these songs get
   * added/fixed, we can remove them from the block list.
   */
  private static final ImmutableSet<SongReference> BLOCK_LIST = ImmutableSet.of(
      // Non-existent songs
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("31").build(),
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("37").build(),
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("95").build(),
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("1345").build(),
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("9").build(),
      SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("49").build(),
      SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8808")
          .build());

  @Inject
  public HymnalNetPatcher(@Merge Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performPatch() {
    addChineseSimplifiedToAllPortugueseSongs();
    purgeBlockList();

    // Some songs are alternate/new tunes but aren't in the NEW TUNE category. Instead, a "b" is
    // appended to the hymn number (e.g. h/81b, h/12b, etc). In these cases, we should clear their
    // languages because it shouldn't be part of the languages map, but should be part of the
    // relevants map.
    this.builders.stream()
        .filter(builder ->
            builder.getReferencesList().stream()
                .anyMatch(reference -> reference.getHymnNumber().matches("\\d+b")))
        .forEach(Hymn.Builder::clearLanguages);

    // fix_danglingReferences();
    // Fix Languages
    //   fix_h1351();
    fix_ch1090();
    fix_h445_h1359();
    fix_hf15();
    fix_h79_h8079();
    fix_h267_h1360();
    fix_ts253();
    //   fix_ts142();
    //   fix_h720_h8526();
    fix_h379();
    fix_ch643();
    fix_pt528();
    fix_h480();
    // fix_ns154();
    //   fix_ts438();
    //   fix_nt723();
    //   fix_nt1307();
    //   fix_de10_h10b();
    //   fix_de786b_h786b();
    //   fix_ch9166();
    //   fix_ns54de();
    //   fix_hd31();
    fix_ch632();
    fix_ts248();
    //
    //   // Fix SONG_META_DATA_RELEVANT
    //   fix_nt_477b();
    //   fix_nt377();
    //   fix_ns98();
    //   fix_nt575_ns34_h711();
    //   fix_ch9575_chnt575c();
    //   fix_h635_h481_h631();
    //   fix_ns59_ns110_ns111();
    //   fix_ns2();
    //   fix_ns4();
    // fix_ns10_ns142();
    // fix_h1033();
    //   fix_h1162_h1163();
    //   fix_ns73();
    //   fix_ns53();
    // fix_ns1();
    //   fix_ns8();
    //   fix_ns12();
    //   fix_ns22();
    //   fix_c31();
    //   fix_c113();
    // fix_h396_ns313();
    //   fix_h383();
    //   fix_c162();
    //   fix_h163();
  }

  /**
   * Portuguese songs currently only map to Chinese songs (if they exist) and don't map to chinese
   * simplified songs. This adds the chinese simplified versions whenever regular Chinese songs
   * appear.
   * <p/>
   * This is necessary because, when we patch songs, we assume that if there is a Chinese song, the
   * simplified version is there as well, so it breaks the patcher if only one of them exists.
   */
  private void addChineseSimplifiedToAllPortugueseSongs() {
    this.builders.forEach(builder -> {
      // If the song is not a portuguese, return early.
      if (builder.getReferencesList().stream().noneMatch(
          songReference -> HymnType.fromString(songReference.getHymnType()) == PORTUGUESE)) {
        return;
      }

      // If the portuguese song already contains simplified Chinese, then there this patcher may
      // be obsolete, so log an error.
      if (builder.getReferencesList().stream().anyMatch(songReference -> {
        HymnType hymnType = HymnType.fromString(songReference.getHymnType());
        return hymnType == CHINESE_SIMPLIFIED || hymnType == CHINESE_SUPPLEMENTAL_SIMPLIFIED;
      })) {
        this.errors.add(
            PipelineError.newBuilder()
                .setSeverity(Severity.WARNING)
                .setErrorType(ErrorType.PATCHER_ADD_ERROR)
                .addMessages("Portuguese song already contains simplified Chinese songs:")
                .addMessages(builder.build().toString())
                .build());
        return;
      }

      // Find the Chinese song and also add in the simplified version.
      builder.getLanguagesList().forEach(songReference -> {
        if (HymnType.fromString(songReference.getHymnType()) == CHINESE) {
          builder.addLanguages(
              SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
                  .setHymnNumber(songReference.getHymnNumber()).build());
        } else if (HymnType.fromString(songReference.getHymnType()) == CHINESE_SUPPLEMENTAL) {
          builder.addLanguages(
              SongReference.newBuilder()
                  .setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
                  .setHymnNumber(songReference.getHymnNumber()).build());
        }
      });
    });
  }

  /**
   * Go through block list and purge songs on there.
   */
  private void purgeBlockList() {
    // Create a seen list to ensure that all the songs on the block list were indeed see (i.e. used
    // to figure out if any block listed songs are obsolete.
    Set<SongReference> seen = new HashSet<>();

    // Delete song references on block list.
    for (Hymn.Builder builder : new ArrayList<>(this.builders)) {
      new ArrayList<>(builder.getReferencesList()).forEach(reference -> {
        if (BLOCK_LIST.contains(reference)) {
          builder.removeReferences(builder.getReferencesList().indexOf(reference));
          seen.add(reference);
        }
      });
      // If, after removing the reference, there are no more references, then remove the entire hymn
      if (builder.getReferencesCount() == 0) {
        this.builders.remove(builder);
      }
    }

    // Delete related songs on block list.
    this.builders.forEach(builder -> {
      new ArrayList<>(builder.getLanguagesList()).forEach(language -> {
        if (BLOCK_LIST.contains(language)) {
          builder.removeLanguages(builder.getLanguagesList().indexOf(language));
          seen.add(language);
        }
      });
      new ArrayList<>(builder.getRelevantsList()).forEach(relevant -> {
        if (BLOCK_LIST.contains(relevant)) {
          builder.removeReferences(builder.getReferencesList().indexOf(relevant));
          seen.add(relevant);
        }
      });
    });

    // If there are any elements in the block list that weren't seen during the purge, then we know
    // that those block list entries are obsolete.
    Set<SongReference> unseen = BLOCK_LIST.stream()
        .filter(songReference -> !seen.contains(songReference)).collect(toImmutableSet());
    if (!unseen.isEmpty()) {
      errors.add(
          PipelineError.newBuilder()
              .setSeverity(Severity.WARNING)
              .setErrorType(ErrorType.PATCHER_OBSOLETE_BLOCK_LIST_ITEM)
              .addMessages(unseen.toString())
              .build());
    }
  }

  // /**
  //  * h/1351 should also map to ht/1351,so we need to update all songs in that graph.
  //  */
  // void fix_h1351() {
  //   addLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1351"),
  //       SongLink.newBuilder().setName("Tagalog")
  //           .setReference(SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("1351")));
  //   addLanguages(SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("1351"),
  //       SongLink.newBuilder().setName("Tagalog")
  //           .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1351")));
  // }

  /**
   * ch/1090 and chx/1090 should map to the english and tagalog 1090, not 1089
   */
  void fix_ch1090() {
    removeLanguages("ch/1090", "h/1089", "ht/1089", "cb/1089", "pt/1089");
    removeLanguages("chx/1090", "h/1089", "ht/1089", "cb/1089", "pt/1089");

    addLanguages("ch/1090", "h/1090");
    addLanguages("chx/1090", "h/1090");
  }

  /**
   *  h/445 and h/1359 are related. However, the language mappings for each is all messed up.
   *  Here is the current mapping:
   *    h/445->cb/445,ch/339,ht/1359,hs/192,pt/445,hf/79;
   *    cb/445->h/445,ch/339,ht/1359,hf/79,hs/192,pt/445;
   *    ht/445->cb/445,ch/339,h/445,hf/79,hs/192,pt/445;
   *    de/445->cb/445,ch/339,h/445,hf/79,ht/1359,hs/192,pt/445;
   *    hf/79->h/445,cb/445,ch/339,de/445,ht/1359,hs/192,pt/445;
   *    hs/192->cb/445,ch/339,h/1359,de/445,ht/1359;
   *    pt/445->h/445,hs/192,ch/339;
   * </p>
   *    h/1359->cb/445,ch/339,ht/1359,hs/192;
   *    ch/339->h/1359,cb/445,ht/1359,hs/192;
   *    ht/1359->h/1359,cb/445,ch/339,hs/192;
   *    hs/190->h/445,cb/445,ch/339,ht/1359,hf/79,pt/445;
   * </p>
   *  Here is the correct mapping:
   *    h/445->cb/445,ht/445,hf/79,de/445,hs/190,pt/445;
   *    cb/445->h/445,ht/445,hf/79,de/445,hs/190,pt/445;
   *    ht/445->h/445,cb/445,hf/79,de/445,hs/190,pt/445;
   *    hf/79->h/445,cb/445,ht/445,de/445,hs/190,pt/445;
   *    de/445->h/445,cb/445,ht/445,hf/79,hs/190,pt/445;
   *    hs/190->h/445,cb/445,ht/445,hf/79,de/445,pt/445;
   *    pt/445->h/445,cb/445,ht/445,hf/79,de/445,hs/190;
   * </p>
   *    h/1359->ch/339,ht/1359,hs/192;
   *    ch/339->h/1359,ht/1359,hs/192;
   *    ht/1359->h/1359,ch/339,hs/192;
   *    hs/192->h/1359,ch/339,ht/1359;
   */
  void fix_h445_h1359() {
    // Remove all current mappings
    removeLanguages("h/445", "cb/445", "ch/339", "ht/1359", "S/192", "pt/445", "hf/79");
    removeLanguages("cb/445", "h/445", "ch/339", "ht/1359", "hf/79", "S/192", "pt/445");
    removeLanguages("ht/445", "cb/445", "ch/339", "h/445", "hf/79", "S/192", "pt/445");
    removeLanguages("de/445", "cb/445", "ch/339", "h/445", "hf/79", "ht/1359", "S/192", "pt/445");
    removeLanguages("hf/79", "h/445", "cb/445", "ch/339", "de/445", "ht/1359", "S/192", "pt/445");
    removeLanguages("S/192", "cb/445", "ch/339", "h/1359", "de/445", "ht/1359");
    removeLanguages("pt/445", "h/445", "S/192", "ch/339");

    removeLanguages("h/1359", "cb/445", "ch/339", "ht/1359", "S/192");
    removeLanguages("ch/339", "h/1359", "cb/445", "ht/1359", "S/192");
    removeLanguages("chx/339", "h/1359", "cb/445", "ht/1359", "S/192");
    removeLanguages("ht/1359", "h/1359", "cb/445", "ch/339", "S/192");
    removeLanguages("S/190", "h/445", "cb/445", "ch/339", "ht/1359", "hf/79", "pt/445");

    // Add all correct mappings
    addLanguages("h/445", "cb/445", "ht/445", "hf/79", "de/445", "S/190", "pt/445");
    addLanguages("cb/445", "h/445", "ht/445", "hf/79", "de/445", "S/190", "pt/445");
    addLanguages("ht/445", "h/445", "cb/445", "hf/79", "de/445", "S/190", "pt/445");
    addLanguages("hf/79", "h/445", "cb/445", "ht/445", "de/445", "S/190", "pt/445");
    addLanguages("de/445", "h/445", "cb/445", "ht/445", "hf/79", "S/190", "pt/445");
    addLanguages("S/190", "h/445", "cb/445", "ht/445", "hf/79", "de/445", "pt/445");
    addLanguages("pt/445", "h/445", "cb/445", "ht/445", "hf/79", "de/445", "S/190");

    addLanguages("h/1359", "ch/339", "ht/1359", "S/192");
    addLanguages("ch/339", "h/1359", "ht/1359", "S/192");
    addLanguages("chx/339", "h/1359", "ht/1359", "S/192");
    addLanguages("ht/1359", "h/1359", "ch/339", "S/192");
    addLanguages("S/192", "h/1359", "ch/339", "ht/1359");
  }

  /**
   * hf/15 is mapped to h/473 for some reason, but it actually is the French version of h/1084. So
   * we need to update the languages json and copy over Author(null), Composer(null), Key(F Major),
   * Time(3/4), Meter(8.8.8.8), Hymn Code(51712165172321), Scriptures (Song of Songs) from h/1084.
   * Category and subcategory seem to be correct, though.
   */
  void fix_hf15() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.FRENCH.abbreviatedValue).setHymnNumber("15"))
        .clearAuthor()
        .clearComposer()
        .clearKey()
        .clearTime()
        .clearMeter()
        .clearHymnCode()
        .clearScriptures()
        .addKey("F Major").addTime("3/4").addMeter("8.8.8.8")
        .addHymnCode("51712165172321").addScriptures("Song of Songs");

    removeLanguages("hf/15", "cb/473", "ch/355", "hd/473", "h/473", "de/473", "S/196", "ht/473",
        "pt/473");
    addLanguages("hf/15", "h/1084");
  }

  /**
   * h/8079 and h/79 are related. However, the language mappings for each is all messed up.
   * </p>
   *  Here is the current mapping:
   *    h/79->cb/79,ch/68,ht/79,hs/44,pt/79;
   *    cb/79->h/79,ch/68,ht/79,hs/44,pt/79;
   *    hs/44->h/79,ch/68,ht/79,cb/79,pt/79;
   *    pt/79->h/79,ch/68,hs/44;
   * </p>
   *    h/8079->cb/79,ch/68,ht/79,hs/44;
   *    ht/79->h/79,ch/68,cb/79,hs/44,pt/79;
   *    ch/68->h/8079,cb/79,ht/79,hs/44;
   * </p>
   *  Here is the correct mapping:
   *    h/79->cb/79,hs/44,pt/79;
   *    cb/79->h/79,hs/44,pt/79;
   *    hs/44->h/79,cb/79,pt/79;
   *    pt/79->h/79,cb/79,hs/44;
   * </p>
   *    h/8079->ch/68,ht/79;
   *    ch/68->h/8079,ht/79;
   *    ht/79->h/8079,ch/68;
   */
  void fix_h79_h8079() {
    // Remove all current mappings
    removeLanguages("h/79", "cb/79", "ch/68", "ht/79", "S/44", "pt/79");
    removeLanguages("cb/79", "h/79", "ch/68", "ht/79", "S/44", "pt/79");
    removeLanguages("S/44", "h/79", "ch/68", "ht/79", "cb/79", "pt/79");
    removeLanguages("pt/79", "h/79", "ch/68", "S/44");

    removeLanguages("h/8079", "cb/79", "ch/68", "ht/79", "S/44");
    removeLanguages("ht/79", "h/79", "ch/68", "cb/79", "S/44", "pt/79");
    removeLanguages("ch/68", "h/8079", "cb/79", "ht/79", "S/44");
    removeLanguages("chx/68", "h/8079", "cb/79", "ht/79", "S/44");

    // Add all correct mappings
    addLanguages("h/79", "cb/79", "S/44");
    addLanguages("cb/79", "h/79", "S/44");
    addLanguages("S/44", "h/79", "cb/79");
    addLanguages("pt/79", "h/79", "cb/79", "S/44");

    addLanguages("h/8079", "ch/68", "ht/79");
    addLanguages("ch/68", "h/8079", "ht/79");
    addLanguages("chx/68", "h/8079", "ht/79");
    addLanguages("ht/79", "h/8079", "ch/68");
  }

  /**
   * h/267 and h/1360 are related. However, the language mappings for each is all messed up.
   * </p>
   *  Here is the current mapping:
   *    h/267->cb/267,ch/217,hf/46,ht/267,hs/127,pt/267;
   *    cb/267->h/267,ch/217,hf/46,ht/267,hs/127,pt/267;
   *    hf/46->cb/267,ch/217,h/267,de/267,ht/267,hs/127,pt/267;
   *    ch/217->h/1360,cb/267,ht/1360,hs/127,pt/267;
   *    ht/267->h/267,cb/267,ch/217,hf/46,hs/127,pt/267;
   *    hs/127->cb/267,ch/217,h/267,hf/46,de/267,ht/267,pt/267;
   *    de/267->cb/267,ch/217,h/267,hf/46,hs/127,ht/267,pt/267;
   *    pt/267->h/267,ch/217,hs/127;
   * </p>
   *    h/1360->cb/267,ch/217,hs/127,ht/1360;
   *    ht/1360->cb/267,ch/217,h/1360,hs/127;
   * </p>
   *  Here is the correct mapping:
   *    h/267->cb/267,ht/267,de/267,hs/127;
   *    cb/267->h/267,ht/267,de/267,hs/127;
   *    de/267->cb/267,h/267,ht/267,hs/127;
   *    ht/267->h/267,cb/267,de/267,hs/127;
   *    hs/127->h/267,cb/267,ht/267,de/267;
   *    pt/267->h/267,hs/127;
   * </p>
   *    h/1360->ch/217,ht/1360,hf/46;
   *    ch/217->h/1360,ht/1360,hf/46;
   *    ht/1360->ch/217,h/1360,hf/46;
   *    hf/46->ch/217,h/1360,ht/1360;
   */
  void fix_h267_h1360() {
    // Remove all current mappings
    removeLanguages("h/267", "cb/267", "ch/217", "hf/46", "ht/267", "S/127", "pt/267");
    removeLanguages("cb/267", "h/267", "ch/217", "hf/46", "ht/267", "S/127", "pt/267");
    removeLanguages("hf/46", "cb/267", "ch/217", "h/267", "de/267", "ht/267", "S/127", "pt/267");
    removeLanguages("ch/217", "h/1360", "cb/267", "ht/1360", "S/127");
    removeLanguages("chx/217", "h/1360", "cb/267", "ht/1360", "S/127");
    removeLanguages("ht/267", "h/267", "cb/267", "ch/217", "hf/46", "S/127", "pt/267");
    removeLanguages("S/127", "cb/267", "ch/217", "h/267", "hf/46", "de/267", "ht/267", "pt/267");
    removeLanguages("de/267", "cb/267", "ch/217", "h/267", "hf/46", "S/127", "ht/267", "pt/267");
    removeLanguages("pt/267", "h/267", "ch/217", "S/127");

    removeLanguages("h/1360", "cb/267", "ch/217", "S/127", "ht/1360");
    removeLanguages("ht/1360", "cb/267", "ch/217", "h/1360", "S/127");

    // Add all correct mappings
    addLanguages("h/267", "cb/267", "ht/267", "de/267", "S/127");
    addLanguages("cb/267", "h/267", "ht/267", "de/267", "S/127");
    addLanguages("de/267", "cb/267", "h/267", "ht/267", "S/127");
    addLanguages("ht/267", "h/267", "cb/267", "de/267", "S/127");
    addLanguages("S/127", "h/267", "cb/267", "ht/267", "de/267");
    addLanguages("pt/267", "h/267", "S/127");

    addLanguages("h/1360", "ch/217", "ht/1360", "hf/46");
    addLanguages("ch/217", "h/1360", "ht/1360", "hf/46");
    addLanguages("ht/1360", "ch/217", "h/1360", "hf/46");
    addLanguages("hf/46", "ch/217", "h/1360", "ht/1360");
  }

  /**
   * ts/253 and ts/253?gb=1 -> mapped to h/754 for some reason, but it actually is the chinese
   * version of h/1164. So it should map to h/1164 and its related songs
   */
  void fix_ts253() {
    removeLanguages("ts/253", "h/754", "pt/754");
    removeLanguages("tsx/253", "h/754", "pt/754");
    addLanguages("ts/253", "h/1164");
    addLanguages("tsx/253", "h/1164");
  }

  // /**
  //  *  ts/142 and ts/142?gb=1 -> mapped to h/1193 for some reason, but it actually is the chinese
  //  *  version of h/1198. So it should map to h/1198 and its related songs
  //  */
  // void fix_ts142() {
  //   getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("142"))
  //       .clearLanguages()
  //       .addLanguages(SongLink.newBuilder().setName("English")
  //           .setReference(
  //               SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1198")));
  //   getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue).setHymnNumber("142"))
  //       .clearLanguages()
  //       .addLanguages(SongLink.newBuilder().setName("English")
  //           .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1198")));
  // }

  // /**
  //  *  h/8526 is an alternate tune of h/720. In Hymnal.net, ch/526 and ch/526?gb=1 play the same tune
  //  *  as h/8526, while cb/720, ht/720, and de/720 play the same tune as h/720.
  //  *  So here is the correct mapping:
  //  *    h/720->cb/720,ht/720,de/720;
  //  *    cb/720->h/720,ht/720,de/720;
  //  *    ht/720->h/720,cb/720,de/720;
  //  *    de/720->h/720,cb/720,ht/720;
  //  * </p>
  //  *    h/8526-> ch/526,chx/526;
  //  *    ch/526-> h/8526,chx/526;
  //  *    chx/526-> h/8526,ch/526;
  //  * </p>
  //  *  In {@link Exceptions}, there is an exception, but that is for the relevants list. But here
  //  *  we still need to clean up the languages list.
  //  * </p>
  //  *  TODO figure out if this should really be changed, or if it should be just an exception, since
  //  *    everything is the same song with the same lyrics, all the translations technically are
  //  *    translations of each other
  //  */
  // void fix_h720_h8526() {
  //   resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("720"),
  //       SongLink.newBuilder().setName("Cebuano")
  //           .setReference(SongReference.newBuilder().setHymnType(CEBUANO.abbreviatedValue).setHymnNumber("720")),
  //       SongLink.newBuilder().setName("Tagalog")
  //           .setReference(SongReference.newBuilder().setHymnType(HymnType.TAGALOG.abbreviatedValue).setHymnNumber("720")),
  //       SongLink.newBuilder().setName("German")
  //           .setReference(SongReference.newBuilder().setHymnType(HymnType.GERMAN.abbreviatedValue).setHymnNumber("720")));
  //
  //   resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8526"),
  //       SongLink.newBuilder().setName("詩歌(繁)")
  //           .setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE.abbreviatedValue).setHymnNumber("526")),
  //       SongLink.newBuilder().setName("诗歌(简)")
  //           .setReference(
  //               SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("526")));
  // }

  /**
   * h/379 and pt/379 an incorrect Chinese song mappings. The Chinese song they're linked
   * to (ch/385) is actually translated by h/8385.
   */
  void fix_h379() {
    removeLanguages("h/379", "ch/385");
    removeLanguages("pt/379", "ch/385");
  }

  /**
   * h/1017, ch/693, and ch/643 are all kind of related. h/1017 is the full English song, ch/693 is
   * the full Chinese song, while ch/643 is just the verse repeated a few times. I'm making a
   * judgement call here to say that just having the chorus does NOT constitute a translation of the
   * song, and therefore, I am going to set ch/643 and ch/643?gb=1 to just have each other as
   * languages.
   */
  void fix_ch643() {
    removeLanguages("ch/643", "cb/1017", "hd/1017", "h/1017", "hf/199", "S/474", "ht/1017", "pt/1017");
    removeLanguages("chx/643", "cb/1017", "hd/1017", "h/1017", "hf/199", "S/474", "ht/1017", "pt/1017");
  }

  /**
   * pt/528 has an incorrect Chinese song mapping. The Chinese song it's linked to (ch/444) is
   * actually translated by h/8444.
   */
  void fix_pt528() {
    removeLanguages("pt/528", "ch/444");
  }

  /**
   * h/480 and pt/480 an incorrect Chinese song mappings. The Chinese song they're linked
   * to (ch/357) is actually translated by h/8357.
   */
  void fix_h480() {
    removeLanguages("h/480", "ch/357");
    removeLanguages("pt/480", "ch/357");
  }

  // /**
  //  * Both ns/154 and h/8330 are translations of ch/330. However, there is only a mapping of ch/330
  //  * to h/8330. So we need to add that mapping to both ns/154 and ch/330.
  //  */
  // void fix_ns154() {
  //   addLanguages(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("154"),
  //       SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("330")),
  //       SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("330")));
  //   addLanguages(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("330"),
  //       SongLink.newBuilder().setName("English").setReference(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("154")));
  //   addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("330"),
  //       SongLink.newBuilder().setName("English").setReference(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("154")));
  // }

  // /**
  //  * Both ns/19 and ns/474 are translations of ts/428. However, ts/428 only references ns/19. So we
  //  * need to add ns/474 as well.
  //  */
  // void fix_ts438() {
  //   addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("428"),
  //       SongLink.newBuilder().setName("English")
  //           .setReference(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("474")));
  //   addLanguages(
  //       SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue).setHymnNumber("428"),
  //       SongLink.newBuilder().setName("English")
  //           .setReference(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("474")));
  // }
  //
  // /**
  //  * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/723, we are going to keep the language
  //  * mapping of the original song of h/723 and remove the language mapping for nt/723. Even though
  //  * this is technically incorrect, it will be easier navigate to the Chinese version from the
  //  * classic hymn than from the new tune.
  //  */
  // void fix_nt723() {
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("723").build()).clearLanguages();
  // }
  //
  // /**
  //  * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/1307, we are going to keep the
  //  * language mapping of the original song of h/723 and remove the language mapping for nt/1307.
  //  * Even though this is technically incorrect, it will be easier navigate to the Chinese version
  //  * from the classic hymn than from the new tune.
  //  */
  // void fix_nt1307() {
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("1307").build()).clearLanguages();
  // }
  //
  // /**
  //  * de/10 and de/10b are the German translations of h/10 and h/10b. However, the mapping is kind of
  //  * messed up. de/10 has the same tune is h/10b and de/10b has the tune of h/10. It makes it very
  //  * difficult to do the mapping here, so we are just going to keep it simple and map h/10 to de/10
  //  * and h/10b to de/10b, even though it's technically incorrect. The mitigation here is that it
  //  * will still be possible to navigate between then because of the Relevant relationship between
  //  * each of the two songs.
  //  * </p>
  //  * The good news though, is that there exists only a one-way mapping from h/10b to the original
  //  * tunes, so we can just remove all languages from h/10b and set it only to de/10b and that should
  //  * solve any invalid reference checks.
  //  */
  // void fix_de10_h10b() {
  //   resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("10b"),
  //       SongLink.newBuilder().setName("German").setReference(
  //           SongReference.newBuilder().setHymnType(GERMAN.abbreviatedValue).setHymnNumber("10b").build()));
  // }
  //
  // /**
  //  * de/786b is the German translation of h/786 and h/786b, but adheres to the tune of h/786b. So we should remove its
  //  * references to songs with the same tune as h/786 and only keep h/786b.
  //  */
  // void fix_de786b_h786b() {
  //   resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("786b"),
  //       SongLink.newBuilder().setName("German")
  //           .setReference(SongReference.newBuilder().setHymnType(GERMAN.abbreviatedValue).setHymnNumber("786b")));
  // }
  //
  // /**
  //  * ch/9166 incorrectly maps to h/157 and ht/157 when it should map to h/166 and ht/166. Everything
  //  * else is correct. so just change ch/9166 to the correct mapping.
  //  */
  // void fix_ch9166() {
  //   getHymn(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("9166")).clearLanguages()
  //       .addLanguages(
  //           SongLink.newBuilder().setName("English")
  //               .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("166")));
  //   getHymn(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("9166")).clearLanguages()
  //       .addLanguages(
  //           SongLink.newBuilder().setName("English")
  //               .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("166")));
  // }
  //
  // /**
  //  * ns/54de is the German song of ns/54. ns/54 references ns/54de as German, but ns/54de just
  //  * references itself also as German. So we need to fix ns/54de to point back to ns/54.
  //  */
  // void fix_ns54de() {
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("54de")).clearLanguages()
  //       .addLanguages(
  //           SongLink.newBuilder().setName("English")
  //               .setReference(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("54")));
  // }
  //
  // /**
  //  * hd/31 is the Dutch song of ns/79 but is referenced by h/31. So we need to remove that mapping.
  //  */
  // void fix_hd31() {
  //   resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("31"),
  //       SongLink.newBuilder().setName("詩歌(繁)")
  //           .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("29")),
  //       SongLink.newBuilder().setName("诗歌(简)")
  //           .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("29")));
  // }

  /**
   * ch/632 is the Chinese song of h/870, but it references h/870b instead, which is a new tune.
   */
  void fix_ch632() {
    removeLanguages("ch/632", "h/870b", "pt/870b");
    removeLanguages("chx/632", "h/870b", "pt/870b");

    addLanguages("ch/632", "h/870", "pt/870");
    addLanguages("chx/632", "h/870", "pt/870");
  }

  /**
   * ts/248 is the Chinese song of h/300, but it references h/300b instead, which is a new tune.
   */
  void fix_ts248() {
    removeLanguages("ts/248", "h/300b", "pt/300b");
    removeLanguages("tsx/248", "h/300b", "pt/300b");

    addLanguages("ts/248", "h/300", "pt/300");
    addLanguages("tsx/248", "h/300", "pt/300");
  }

  // /**
  //  * nt/477b should link to nt/477 as a relevant song, but instead contains a self-link.
  //  */
  // void fix_nt_477b() {
  //   // Replace self link with nt/477
  //   SongReference nt477b = SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("477b").build();
  //   List<SongLink> newRelevants = getHymn(nt477b).getRelevantsList().stream()
  //       .map(
  //           songLink -> {
  //             if (songLink.getReference().equals(nt477b)) {
  //               return SongLink.newBuilder().setName(songLink.getName()).setReference(
  //                   SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("477").build()).build();
  //             }
  //             return songLink;
  //           }).collect(Collectors.toList());
  //   getHymn(nt477b).clearRelevants().addAllRelevants(newRelevants);
  // }
  //
  // /**
  //  * nt/377 has a relevant song of nt/1079, but they are not really related. So remove it from the relevant list
  //  */
  // void fix_nt377() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("377"),
  //       SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("1079"));
  // }
  //
  // /**
  //  * ns/98 has a relevant song of ns/80, but they are not really related. So remove it from the relevant list
  //  */
  // void fix_ns98() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("98"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("80"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("80"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("98"));
  // }
  //
  // /**
  //  * ns/575, ns/34, and h/711 are in a large web of interconnected songs that need to be unraveled.
  //  * They all reference each other (and others) in some way, but aren't really related to each other
  //  */
  // void fix_nt575_ns34_h711() {
  //   fix_nt575();
  //   fix_ns34();
  //   fix_h711();
  // }
  //
  // /**
  //  * nt/575 is the new tune for h/575, but it also has a bunch of relevant songs (ns/34, nt/711, nt/1079)
  //  * that it is not related to, so we should remove them.
  //  */
  // void fix_nt575() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("575"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("34"),
  //       SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("711"),
  //       SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("1079"));
  // }
  //
  // /**
  //  * ch/9575 is the Chinese translation of h/575. The new tune of h/575 is nt/575, and that song in
  //  * Chinese is ch/nt575c
  //  */
  // void fix_ch9575_chnt575c() {
  //   resetRelevants(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("9575"),
  //       SongLink.newBuilder().setName("New Tune")
  //           .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("nt575c")));
  //
  //   resetRelevants(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("9575"),
  //       SongLink.newBuilder().setName("New Tune")
  //           .setReference(
  //               SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("nt575c")));
  // }
  //
  // /**
  //  * ns/34 doesn't have any new/alternaete tunes, but just references a bunch of songs that it isn't
  //  * related to. So we can just clear the relevants list there.
  //  */
  // void fix_ns34() {
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("34").build()).clearRelevants();
  // }
  //
  // /**
  //  * h/711 has two new tunes: nt/711 and nt/711b, but also references ns/34, even though it's not
  //  * related.
  //  */
  // void fix_h711() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("711"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("34"));
  // }
  //
  // /**
  //  * h/635, h/481, h/631 reference each other, but they're not really related
  //  */
  // void fix_h635_h481_h631() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("635"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("481"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("631"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("481"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("635"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("631"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("631"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("481"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("635"));
  // }
  //
  // /**
  //  * ns/59, ns/110, ns/111 reference each other, but they're not really related
  //  */
  // void fix_ns59_ns110_ns111() {
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("59")).clearRelevants();
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("110")).clearRelevants();
  //   getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("111")).clearRelevants();
  // }
  //
  // /**
  //  * ns/2 references ns/3, but they're not really related
  //  */
  // void fix_ns2() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("2"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("3"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("3"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("2"));
  // }
  //
  // /**
  //  * ns/4 references ns/5 and h/36, but they're not really related
  //  */
  // void fix_ns4() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("4"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("36"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("4"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("5"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("5"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("4"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("5"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("36"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("36"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("4"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("36"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("5"));
  // }

  /**
   * ns/10, ns/142 reference each other, but they're not really related
   */
  void fix_ns10_ns142() {
    removeRelevants(
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("10"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("142"));
    removeRelevants(
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("142"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("10"));
  }

  /**
   * h/1033 references h/1007, but they're not really related
   */
  void fix_h1033() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1033"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1007"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1007"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1033"));
  }

  // /**
  //  * h/1162, h/1163 reference each other, but they're not really related
  //  */
  // void fix_h1162_h1163() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1162"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1163"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1163"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1162"));
  // }
  //
  // /**
  //  * ns/73 references ns/34 and nt/711 but is not really related to those
  //  */
  // void fix_ns73() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("73"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("34"),
  //       SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("711"));
  // }
  //
  // /**
  //  * ns/53 references ns/34, but they're not really related
  //  */
  // void fix_ns53() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("53"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("34"));
  // }

  /**
   * ns/1 references h/278, but they're not really related, apart from having the same tune
   */
  void fix_ns1() {
    removeRelevants(
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("1"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("278"));
  }

  // /**
  //  * ns/8 references h/1282, but they're not really related, apart from having the same tune
  //  */
  // void fix_ns8() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("8"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1282"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1282"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("8"));
  // }
  //
  // /**
  //  * ns/12 references h/661, but they're not really related, apart from having the same tune
  //  */
  // void fix_ns12() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("12"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("661"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("661"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("12"));
  // }
  //
  // /**
  //  * ns/22 references h/313, but they're not really related, apart from having the same tune
  //  */
  // void fix_ns22() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("22"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("313"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("313"),
  //       SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("22"));
  // }
  //
  // /**
  //  * c/31 references h/1014, but they're not really related, apart from having the same tune in the chorus
  //  */
  // void fix_c31() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("31"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1014"));
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1014"),
  //       SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("31"));
  // }
  //
  // /**
  //  * c/113 references h/556, but they're not really related, apart from having the same tune
  //  */
  // void fix_c113() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("113"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("556"));
  // }

  /**
   * h/396 and ns/313 references each other, but they're not really related, apart from having the
   * same tune
   */
  void fix_h396_ns313() {
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue)
        .setHymnNumber("396")).clearRelevants();
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue)
        .setHymnNumber("313")).clearRelevants();
  }

  // /**
  //  * h/383 references itself when it should really be referencing nt/383
  //  */
  // void fix_h383() {
  //   resetRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("383"),
  //       SongLink.newBuilder().setName("New Tune")
  //           .setReference(SongReference.newBuilder().setHymnType(NEW_TUNE.abbreviatedValue).setHymnNumber("383")));
  // }
  //
  // /**
  //  * c/162 has a relevant song of h/993, but they are not really related. So remove it from the relevant list
  //  */
  // void fix_c162() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("162"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("993"));
  // }
  //
  // /**
  //  * h/163 references itself when it should really be referencing nothing
  //  */
  // void fix_h163() {
  //   removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("163"),
  //       SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("163"));
  // }
  //

  /**
   * Fix references where something points to a hymn, but it doesn't point back (i.e. dangling).
   * </p>
   * This is difficult to automate because we don't often know what the language of the dangling
   * reference is. For instance, ns/568 references ns/568c and ns/568?gb=1. However, those songs
   * don't reference it back. It is difficult in code to know that ns/568c is the Chinese version of
   * ns/568, so we won't know how to set the "value" field in the Datum. Thankfully, there are only
   * ~20 of these, so we can fix them manually.
   */
  void fix_danglingReferences() {
    // ns/568 references ch/ns568c and chx/ns568. However, those songs don't reference it back. Fix the mapping so they reference each other.
    addLanguages(
        SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("ns568c"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("568"));
    addLanguages(
        SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
            .setHymnNumber("ns568c"),
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue)
            .setHymnNumber("568"));

    // ns/195 references ts/228 and ts/228?gb=1 but those songs don't reference it back. Fix the
    // mapping so they reference each other.
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue)
            .setHymnNumber("228"),
        SongReference.newBuilder().setHymnType(HymnType.NEW_SONG.abbreviatedValue)
            .setHymnNumber("195"));
    addLanguages(
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
            .setHymnNumber("228"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("195"));

    // hd/4 references ns/257 but ns/257 doesn't reference it back. Fix the mapping so it does.
    addLanguages(
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("257"),
        SongReference.newBuilder().setHymnType(DUTCH.abbreviatedValue).setHymnNumber("4"));

    // ns/7 references h/18 because it is an adaptation of it. However, h/18 doesn't reference it
    // back. Fix h/18 to reference it back.
    addRelevants(
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("18"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("7"));

    // ns/20 references lb/12 because it is an adaptation of it. However, lb/12 doesn't reference it back.
    addRelevants(
        SongReference.newBuilder().setHymnType(HOWARD_HIGASHI.abbreviatedValue).setHymnNumber("12"),
        SongReference.newBuilder().setHymnType(NEW_SONG.abbreviatedValue).setHymnNumber("20"));

    // c/21 references h/70 because it is a shortened version of it. Since they are essentially the same song, we
    // should change the name to "Related" instead of the song title, because otherwise it'll just be the same as
    // the current song title.
    removeRelevants(
        SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("21"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("70"));
    removeRelevants(SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue)
            .setHymnNumber("70"),
        SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("21"));
    addRelevants(
        SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("21"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("70"));
    addRelevants(SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN.abbreviatedValue)
            .setHymnNumber("70"),
        SongReference.newBuilder().setHymnType(CHILDREN_SONG.abbreviatedValue).setHymnNumber("21"));
  }
}
