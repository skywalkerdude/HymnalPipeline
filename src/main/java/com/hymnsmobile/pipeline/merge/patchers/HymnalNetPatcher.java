package com.hymnsmobile.pipeline.merge.patchers;

import com.google.common.collect.ImmutableSet;
import com.hymnsmobile.pipeline.merge.HymnType;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.PipelineError.ErrorType;
import com.hymnsmobile.pipeline.models.PipelineError.Severity;
import com.hymnsmobile.pipeline.models.SongReference;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.hymnsmobile.pipeline.merge.HymnType.*;

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
  private static final ImmutableSet<SongReference> BLOCK_LIST = ImmutableSet.of();

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

    // Fix Languages
    fix_h1351();
    fix_ch1090();
    fix_h445_h1359();
    fix_h79_h8079();
    fix_h267_h1360();
    fix_h379();
    fix_ch643();
    fix_pt528();
    fix_h480();
    fix_ns154();
    fix_ch9166();
    fix_ht62();
    fix_ht1182();
    fix_hf881();
    fix_ch632();
    fix_ts248();
    fix_pt1001();
    fix_pt131();
    fix_pt598();
    fix_pt1007();
    fix_pt1248();
    fix_pt737();
    fix_pt601();
    fix_pt1232();
    fix_pt1258();
    fix_pt1222();
    fix_pt1243();
    fix_pt921();
    fix_pt340();
    fix_pt1297();
    fix_ht1358();
    fix_h6864();
    fix_ns54de();
    fix_ns381p_ns381tc_ns381cb();
    fix_ns506p_ns506r();
    fix_ns180de();
    fix_ns617t();
    fix_ns151de();
    fix_ns151ht();
    fix_h984_h8204_cb984_pt984_ht984_ch204();
    fix_h254_h8211_ht254_pt254_ch211();

    // Fix SONG_META_DATA_RELEVANT
    fix_nt377();
    fix_ns98();
    fix_nt575_ns34_h711_ns73_ns53();
    fix_h631_h481_h635();
    fix_ns111_ns59();
    fix_ns2();
    fix_ns4();
    fix_ns10_ns142();
    fix_h1033();
    fix_h1162_h1163();
    fix_ns1();
    fix_ns8();
    fix_ns12();
    fix_ns22();
    fix_c31();
    fix_c113();
    fix_h396_ns313();
    fix_h383();
    fix_h393();
    fix_nt477b();
    fix_c162();
    fix_h163();
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
                .setSource(PipelineError.Source.HYMNAL_NET)
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
              .setSource(PipelineError.Source.HYMNAL_NET)
              .setSeverity(Severity.WARNING)
              .setErrorType(ErrorType.PATCHER_OBSOLETE_BLOCK_LIST_ITEM)
              .addMessages(unseen.toString())
              .build());
    }
  }

  /**
   * h/1351 should also map to ht/1351,so we need to update all songs in that graph.
   */
  void fix_h1351() {
    addLanguages("h/1351", "ht/1351");
    addLanguages("ht/1351", "h/1351");
  }

  /**
   * ch/1090 and chx/1090 should map to the english and tagalog 1090, not 1089
   */
  void fix_ch1090() {
    removeLanguages("ch/1090", "h/1089", "ht/1089", "cb/1089", "pt/1089");

    addLanguages("ch/1090", "h/1090");
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
    addLanguages("ht/1359", "h/1359", "ch/339", "S/192");
    addLanguages("S/192", "h/1359", "ch/339", "ht/1359");
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

    // Add all correct mappings
    addLanguages("h/79", "cb/79", "S/44");
    addLanguages("cb/79", "h/79", "S/44");
    addLanguages("S/44", "h/79", "cb/79");
    addLanguages("pt/79", "h/79", "cb/79", "S/44");

    addLanguages("h/8079", "ch/68", "ht/79");
    addLanguages("ch/68", "h/8079", "ht/79");
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

  /**
   * Both ns/154 and h/8330 are translations of ch/330. However, there is only a mapping of ch/330
   * to h/8330. So we need to add that mapping to both ns/154 and ch/330.
   */
  void fix_ns154() {
    addLanguages("ns/154", "ch/330");
    addLanguages("ch/330", "ns/154");
  }

  /**
   * ch/9166 incorrectly maps to h/157, ht/157, and pt/157 when it should map to h/166 and its
   * related songs. h/166 and its related songs all already have the correct mappings.
   */
  void fix_ch9166() {
    removeLanguages("ch/9166", "h/157", "ht/157", "pt/157");
  }

  /**
   * ht/62 incorrectly maps to h/31, hd/31, pt/31, and ch/29 when it should map to h/62 and its
   * related songs. h/62 and its related songs all already have the correct mappings.
   */
  void fix_ht62() {
    removeLanguages("ht/62", "h/31", "pt/31", "ch/29");
    addLanguages("ht/62", "h/62");
  }

  /**
   * ht/1182 incorrectly maps to h/248, cb/248, pt/248, S/115, and ch/202 when it should map to h/1182 and its
   * related songs. h/1182 and its related songs all already have the correct mappings.
   */
  void fix_ht1182() {
    removeLanguages("ht/1182", "h/248", "cb/248", "pt/248", "S/115", "ch/202");
    addLanguages("ht/1182", "h/1182");
  }

  /**
   * hf/881 incorrectly maps to ns/180, hd/6011, ns/180de, and ts/1004 when it should map to h/881 and its
   * related songs. h/881 and its related songs all already have the correct mappings.
   */
  void fix_hf881() {
    removeLanguages("hf/881", "ns/180", "hd/6011", "ns/180de", "ts/1004");
    addLanguages("hf/881", "h/881");
  }

  /**
   * ch/632 is the Chinese song of h/870, but it references h/870b instead, which is a new tune.
   */
  void fix_ch632() {
    removeLanguages("ch/632", "h/870b");

    addLanguages("ch/632", "h/870");
  }

  /**
   * ts/248 is the Chinese song of h/300, but it references h/300b instead, which is a new tune.
   */
  void fix_ts248() {
    removeLanguages("ts/248", "h/300b");

    addLanguages("ts/248", "h/300");
  }

  /**
   * pt/1001 references h/100 when it should be referencing h/1001.
   */
  void fix_pt1001() {
    removeLanguages("pt/1001", "h/100");
    addLanguages("pt/1001", "h/1001");
  }

  /**
   * pt/131 references h/116 when it should be referencing h/131.
   */
  void fix_pt131() {
    removeLanguages("pt/131", "h/116");
    addLanguages("pt/131", "h/131");
  }

  /**
   * pt/598 references ch/540 when it should be referencing ch/440.
   */
  void fix_pt598() {
    removeLanguages("pt/598", "ch/540");
    addLanguages("pt/598", "ch/440");
  }

  /**
   * pt/634 has an incorrect Spanish song mapping. The Spanish song it's linked to (hs/470) is
   * actually the Spanish version of by h/1007.
   */
  void fix_pt1007() {
    removeLanguages("pt/634", "S/470");
  }

  /**
   * pt/1248 references ts/602 when it should be referencing ts/603.
   */
  void fix_pt1248() {
    removeLanguages("pt/1248", "ts/602");
    addLanguages("pt/1248", "ts/603");
  }

  /**
   * pt/737 references hs/308 and ch/532 when it should be referencing hs/309 and ch/533.
   */
  void fix_pt737() {
    removeLanguages("pt/737", "S/308", "ch/532");
    addLanguages("pt/737", "S/309", "ch/533");
  }

  /**
   * pt/601 has incorrect Spanish and Chinese song mappings. The songs it's linked to
   * (hs/291, ch/451) are actually translations of h/612.
   */
  void fix_pt601() {
    removeLanguages("pt/601", "S/291", "ch/451");
  }

  /**
   * pt/1232 references ts/617 when it should be referencing ts/616.
   */
  void fix_pt1232() {
    removeLanguages("pt/1232", "ts/617");
    addLanguages("pt/1232", "ts/616");
  }

  /**
   * pt/1258 references ts/511 when it should be referencing ts/536.
   */
  void fix_pt1258() {
    removeLanguages("pt/1258", "ts/511");
    addLanguages("pt/1258", "ts/536");
  }

  /**
   * pt/1222 references ts/16 when it should be referencing ts/37.
   */
  void fix_pt1222() {
    removeLanguages("pt/1222", "ts/16");
    addLanguages("pt/1222", "ts/37");
  }

  /**
   * pt/1243 references ts/613 when it should be referencing ts/626.
   */
  void fix_pt1243() {
    removeLanguages("pt/1243", "ts/613");
    addLanguages("pt/1243", "ts/626");
  }

  /**
   * pt/921 references ch/664 when it should be referencing ts/853.
   */
  void fix_pt921() {
    removeLanguages("pt/921", "ch/664");
    addLanguages("pt/921", "ts/853");
  }

  /**
   * pt/340 references ch/260 when it shouldn't be referencing any Chinese song.
   */
  void fix_pt340() {
    removeLanguages("pt/340", "ch/260");
  }

  /**
   * pt/1297 references ts/808 when it should be referencing ts/145.
   */
  void fix_pt1297() {
    removeLanguages("pt/1297", "ts/808");
    addLanguages("pt/1297", "ts/145");
  }

  /**
   * ht/1358 references cb/921 and hs/414 when it shouldn't be referencing any of those songs.
   */
  void fix_ht1358() {
    removeLanguages("ht/1358", "cb/921", "S/414");
  }

  /**
   * h/6864 references ts/848, when it shouldn't. Weirdly though, h/6864 also isn't the English
   * translation of ts/864, so it should just be by itself.
   */
  void fix_h6864() {
    removeLanguages("h/6864", "ts/848");
  }

  /**
   * ns/54de references itself when it should be referencing ns/54.
   */
  void fix_ns54de() {
    removeLanguages("ns/54de", "ns/54de");
    addLanguages("ns/54de", "ns/54");
  }

  /**
   * ns/381 has a bunch of exotic language translations (Arabic-ns/381ar, Estonian-ns/381es, etc.).
   * However, a lot of them have self-references. Apart from the self-references, the language
   * mappings should be correct.
   */
  void fix_ns381p_ns381tc_ns381cb() {
    removeLanguages("ns/381p", "ns/381p");
    removeLanguages("ns/381tc", "ns/381tc");
    removeLanguages("ns/381cb", "ns/381cb");
  }

  /**
   * ns/506 has a bunch of exotic language translations (Korean-ns/506k, Russian-ns/506r, etc.).
   * However, a lot of them have self-references. Apart from the self-references, the language
   * mappings should be correct.
   */
  void fix_ns506p_ns506r() {
    removeLanguages("ns/506p", "ns/506p");
    removeLanguages("ns/506r", "ns/506r");
  }

  /**
   * ns/180de has an unnecessary self-reference.
   */
  void fix_ns180de() {
    removeLanguages("ns/180de", "ns/180de");
  }

  /**
   * ns/617t has an unnecessary self-reference.
   */
  void fix_ns617t() {
    removeLanguages("ns/617t", "ns/617t");
  }

  /**
   * ns/151de has an unnecessary self-reference.
   */
  void fix_ns151de() {
    removeLanguages("ns/151de", "ns/151de");
  }

  /**
   * ns/151ht has an unnecessary self-reference.
   */
  void fix_ns151ht() {
    removeLanguages("ns/151ht", "ns/151ht");
  }

  /**
   * h/984 and h/8204 are both the same song (with the same tune), but h/8204 has 5 verses while h/984 only has 4.
   * However, h/984 is the official LSM-sanctioned version. In terms of languages though, we separate the translations
   * with 5 verses and assign them to h/8204 and the translations with 4 verses we assign to h/984.
   * <p/>
   *  Here is the correct mapping:
   *    h/984->cb/984,pt/984,de/984;
   *    cb/984->h/984,pt/984,de/984;
   *    pt/984->cb/984,h/984,de/984;
   *    de/984->cb/984,h/984,pt/984;
   * </p>
   *    h/8204->ht/984,ch/204;
   *    ht/984->h/8204,ch/204;
   *    ch/204->ht/984,h/8204;
   */
  void fix_h984_h8204_cb984_pt984_ht984_ch204() {
    removeLanguages("h/984", "ht/984", "ch/204");
    removeLanguages("cb/984", "ht/984", "ch/204");
    removeLanguages("pt/984", "ch/204");
    removeLanguages("de/984", "ht/984", "ch/204");

    removeLanguages("h/8204", "cb/984");
    removeLanguages("ht/984", "h/984", "cb/984", "pt/984");
    addLanguages("ht/984", "h/8204");
    removeLanguages("ch/204", "h/984", "cb/984", "pt/984");
    addLanguages("ch/204", "h/8204");
  }

  /**
   * h/254 and h/8211 are very similar songs but not exactly the same. It looks like h/254 was the original song but
   * h/8211 is a closer translation of the (probably original) ch/211. So we perform a remap, according to how closely
   * the lyrics match to each english song.
   * <p/>
   *  Here is the correct mapping:
   *    h/254->ht/254,pt/254;
   *    ht/254->h/254,pt/254;
   *    pt/254->h/254,ht/254;
   * </p>
   *    h/8211->ch/211;
   *    ch/211->h/8211;
   */
  void fix_h254_h8211_ht254_pt254_ch211() {
    removeLanguages("h/254", "ch/211");
    removeLanguages("ht/254", "ch/211");
    removeLanguages("pt/254", "ch/211");

    removeLanguages("h/8211", "ht/254");
    removeLanguages("ch/211", "h/254", "ht/254", "pt/254");
    addLanguages("ch/211", "h/8211");
  }

  /*--------------------------RELEVANTS--------------------------*/

  /**
   * nt/377 has a relevant song of nt/1079, but they are not really related. So remove it from the
   * relevant list.
   */
  void fix_nt377() {
    removeRelevants("nt/377", "nt/1079");
  }

  /**
   * ns/98 has a relevant song of ns/80, but they are not really related. So remove it from the
   * relevants list.
   */
  void fix_ns98() {
    removeRelevants("ns/98", "ns/80");
  }

  /**
   * ns/575, ns/34, h/711, ns/73, and ns/53 are in a large web of interconnected songs that need to
   * be unraveled. They all reference each other in some way, but aren't really related to each
   * other.
   */
  void fix_nt575_ns34_h711_ns73_ns53() {
    fix_nt575();
    fix_ns34();
    fix_h711();
    fix_ns73();
    fix_ns53();
  }

  /**
   * nt/575 is the new tune for h/575, but it also has a bunch of relevant songs (ns/34, nt/711, nt/1079)
   * that it is not related to, so we should remove them.
   */
  void fix_nt575() {
    removeRelevants("nt/575", "ns/34", "nt/711", "nt/1079");
  }

  /**
   * ns/34 doesn't have any new/alternate tunes, but just references a bunch of songs that it isn't
   * related to. So we can just clear the relevants list there.
   */
  void fix_ns34() {
    removeRelevants("ns/34", "nt/575", "nt/711", "nt/1079", "ns/73", "ns/53");
  }

  /**
   * h/711 has two new tunes: nt/711 and nt/711b, but also references ns/34, even though it's not
   * related.
   */
  void fix_h711() {
    removeRelevants("h/711", "ns/34");
  }

  /**
   * ns/73 references ns/34 and nt/711 but is not really related to those
   */
  void fix_ns73() {
    removeRelevants("ns/73", "ns/34", "nt/711");
  }

  /**
   * ns/53 references ns/34, but they're not really related
   */
  void fix_ns53() {
    removeRelevants("ns/53", "ns/34");
  }

  /**
   * h/635, h/481, h/631 reference each other, but they're not really related.
   */
  void fix_h631_h481_h635() {
    removeRelevants("h/631", "h/481", "h/635");
    removeRelevants("h/481", "h/631");
    removeRelevants("h/635", "h/631");
  }

  /**
   * ns/111 references ns/59 and ns/59 references ns/110. But in reality, none of them are really
   * related.
   */
  void fix_ns111_ns59() {
    removeRelevants("ns/111", "ns/59");
    removeRelevants("ns/59", "ns/110");
  }

  /**
   * ns/2 references ns/3, but they're not really related
   */
  void fix_ns2() {
    removeRelevants("ns/2", "ns/3");
  }

  /**
   * ns/4 references ns/5 and h/36, but they're not really related
   */
  void fix_ns4() {
    removeRelevants("ns/4", "ns/5", "h/36");
  }

  /**
   * ns/10, ns/142 reference each other, but they're not really related.
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
   * h/1033 references h/1007, but they're not really related.
   */
  void fix_h1033() {
    removeRelevants("h/1033", "h/1007");
  }

  /**
   * h/1162, h/1163 reference each other, but they're not really related.
   */
  void fix_h1162_h1163() {
    removeRelevants("h/1162", "h/1163");
    removeRelevants("h/1163", "h/1162");
  }

  /**
   * ns/1 references h/278, but they're not really related, apart from having the same tune.
   */
  void fix_ns1() {
    removeRelevants("ns/1", "h/278");
  }

  /**
   * ns/8 references h/1282, but they're not really related, apart from having the same tune.
   */
  void fix_ns8() {
    removeRelevants("ns/8", "h/1282");
  }

  /**
   * ns/12 references h/661, but they're not really related, apart from having the same tune.
   */
  void fix_ns12() {
    removeRelevants("ns/12", "h/661");
  }

  /**
   * ns/22 references h/313, but they're not really related, apart from having the same tune.
   */
  void fix_ns22() {
    removeRelevants("ns/22", "h/313");
  }

  /**
   * c/31 references h/1014, but they're not really related, apart from having the same tune in the
   * chorus.
   */
  void fix_c31() {
    removeRelevants("c/31", "h/1014");
  }

  /**
   * c/113 references h/556, but they're not really related, apart from having the same tune.
   */
  void fix_c113() {
    removeRelevants("c/113", "h/556");
  }

  /**
   * h/396 and ns/313 references each other, but they're not really related, apart from having the
   * same tune.
   */
  void fix_h396_ns313() {
    removeRelevants("h/396", "ns/313");
    removeRelevants("ns/313", "h/396");
  }

  /**
   * h/383 references itself when it should be referencing nt/383.
   */
  void fix_h383() {
    removeRelevants("h/383", "h/383");
    addRelevants("h/383", "nt/383");
  }

  /**
   * h/393 has an unnecessary self-reference.
   */
  void fix_h393() {
    removeRelevants("h/393", "h/393");
  }

  /**
   * nt/477b should link to nt/477 as a relevant song, but instead contains a self-link, so we
   * remove the self-link and add nt/477.
   */
  void fix_nt477b() {
    removeRelevants("nt/477b", "nt/477b");
    addRelevants("nt/477b", "nt/477");
  }

  /**
   * c/162 has a relevant song of h/993, but they are not really related. So remove it from the
   * relevant list.
   */
  void fix_c162() {
    removeRelevants("c/162", "h/993");
  }

  /**
   * h/163 references itself when it should be referencing nothing
   */
  void fix_h163() {
    removeRelevants("h/163", "h/163");
  }
}
