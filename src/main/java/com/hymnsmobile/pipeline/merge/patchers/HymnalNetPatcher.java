package com.hymnsmobile.pipeline.merge.patchers;

import static com.hymnsmobile.pipeline.models.HymnType.CEBUANO;
import static com.hymnsmobile.pipeline.models.HymnType.CHILDREN_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.DUTCH;
import static com.hymnsmobile.pipeline.models.HymnType.FRENCH;
import static com.hymnsmobile.pipeline.models.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.models.HymnType.HOWARD_HIGASHI;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_SONG;
import static com.hymnsmobile.pipeline.models.HymnType.NEW_TUNE;
import static com.hymnsmobile.pipeline.models.HymnType.SPANISH;
import static com.hymnsmobile.pipeline.models.HymnType.TAGALOG;

import com.hymnsmobile.pipeline.merge.Exceptions;
import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Performs one-off patches to the set of hymns from Hymnal.net that are unfixable with a general
 * algorithm.
 */
@MergeScope
public class HymnalNetPatcher extends Patcher {

  @Inject
  public HymnalNetPatcher(@Merge Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performPatch() {
    // It's a new or alternate tune that references the translation of the original tune. For
    // those, we will just label them as "OTHER" as a catch-all.

    // Some songs are alternate/new tunes but aren't in the NEW TUNE category. Instead, a "b" is
    // appended to the hymn number (e.g. h/81b, h/12b, etc). In these cases, we should clear their
    // languages because it shouldn't be part of the languages map, but should be part of the
    // relevants map.
    this.builders.stream()
        .filter(builder ->
            builder.getReferencesList().stream()
                .anyMatch(reference -> reference.getHymnNumber().matches("\\d+b")))
        .forEach(Hymn.Builder::clearLanguages);

    fix_danglingReferences();
    // Fix Languages
    fix_h1351();
    fix_ch1090();
    fix_h445_h1359();
    fix_hf15();
    fix_h79_h8079();
    fix_h267_h1360();
    fix_ts253();
    fix_ts142();
    fix_h720_h8526();
    fix_h379();
    fix_ch643();
    fix_h528();
    fix_h480();
    fix_ns154();
    fix_ts438();
    fix_nt723();
    fix_nt1307();
    fix_de10_h10b();
    fix_de786b_h786b();
    fix_ch9166();
    fix_ns54de();
    fix_hd31();
    fix_ch632();
    fix_ts248();

    // Fix SONG_META_DATA_RELEVANT
    fix_nt_477b();
    fix_nt377();
    fix_ns98();
    fix_nt575_ns34_h711();
    fix_ch9575_chnt575c();
    fix_h635_h481_h631();
    fix_ns59_ns110_ns111();
    fix_ns2();
    fix_ns4();
    fix_ns10_ns142();
    fix_h1033();
    fix_h1162_h1163();
    fix_ns73();
    fix_ns53();
    fix_ns1();
    fix_ns8();
    fix_ns12();
    fix_ns22();
    fix_c31();
    fix_c113();
    fix_h396_ns313();
    fix_h383();
    fix_c162();
    fix_h163();
  }

  /**
   * h/1351 should also map to ht/1351,so we need to update all songs in that graph.
   */
  void fix_h1351() {
    addLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1351"),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("1351")));
    addLanguages(SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("1351"),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1351")));
  }

  /**
   * ch/1090 and chx/1090 should map to the english, and tagalog 1090, not 1089
   */
  void fix_ch1090() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("1090"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English").setReference(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1090")));
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("1090"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English").setReference(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1090")));
  }

  /**
   *  h/445 and h/1359 are related. However, the language mappings for each is all messed up.
   *  Here is the current mapping:
   *    h/445->cb/445,ch/339,ht/1359,hs/192;
   *    cb/445->h/445,ch/339,ht/1359,hf/79,hs/192;
   *    ht/445->cb/445,ch/339,h/445,hs/192;
   *    de/445->cb/445,ch/339,h/445,hf/79,ht/1359,hs/192;
   *    hf/79->h/445,cb/445,ch/339,de/445,ht/1359,hs/192;
   *    hs/192->cb/445,ch/339,h/1359,de/445,ht/1359;
   *
   *    h/1359->cb/445,ch/339,ht/1359,hs/192;
   *    ch/339->h/1359,cb/445,ht/1359,hs/192;
   *    ht/1359->h/1359,cb/445,ch/339,hs/192;
   *    hs/190->h/445,cb/445,ch/339,ht/1359;
   *
   *  Here is the correct mapping:
   *    h/445->cb/445,ht/445,hf/79,de/445,hs/190;
   *    cb/445->h/445,ht/445,hf/79,de/445,hs/190;
   *    ht/445->h/445,cb/445,hf/79,de/445,hs/190;
   *    hf/79->h/445,cb/445,ht/445,de/445,hs/190;
   *    de/445->h/445,cb/445,ht/445,hf/79,hs/190;
   *    hs/190->h/445,cb/445,ht/445,hf/79,de/445;
   *
   *    h/1359->ch/339,ht/1359,hs/192;
   *    ch/339->h/1359,ht/1359,hs/192;
   *    ht/1359->h/1359,ch/339,hs/192;
   *    hs/192->h/1359,ch/339,ht/1359;
   */
  void fix_h445_h1359() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("445"),
        SongLink.newBuilder().setName("Cebuano").setReference(SongReference.newBuilder().setHymnType(CEBUANO).setHymnNumber("445")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("445")),
        SongLink.newBuilder().setName("French").setReference(SongReference.newBuilder().setHymnType(FRENCH).setHymnNumber("79")),
        SongLink.newBuilder().setName("German").setReference(SongReference.newBuilder().setHymnType(GERMAN).setHymnNumber("445")),
        SongLink.newBuilder().setName("Spanish").setReference(SongReference.newBuilder().setHymnType(SPANISH).setHymnNumber("190")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1359"),
        SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("339")),
        SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("339")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("1359")),
        SongLink.newBuilder().setName("Spanish").setReference(SongReference.newBuilder().setHymnType(SPANISH).setHymnNumber("192")));
  }


  /**
   * hf/15 is mapped to h/473 for some reason, but it actually is the French version of h/1084. So
   * we need to update the languages json and copy over Author(null), Composer(null), Key(F Major),
   * Time(3/4), Meter(8.8.8.8), Hymn Code(51712165172321), Scriptures (Song of Songs) from h/1084.
   * Category and subcategory seem to be correct, though.
   */
  void fix_hf15() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.FRENCH).setHymnNumber("15"))
        .clearLanguages()
        .clearAuthor()
        .clearComposer()
        .clearKey()
        .clearTime()
        .clearMeter()
        .clearHymnCode()
        .clearScriptures()
        .addLanguages(SongLink.newBuilder().setName("English").setReference(
            SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1084")))
        .addKey("F Major").addTime("3/4").addMeter("8.8.8.8")
        .addHymnCode("51712165172321").addScriptures("Song of Songs");
  }

  /**
   * h/8079 and h/79 are related. However, the language mappings for each is all messed up.
   *
   *  Here is the current mapping:
   *    h/79->cb/79,ch/68,ht/79,hs/44;
   *    cb/79->h/79,ch/68,ht/79,hs/44;
   *    hs/44->h/79,ch/68,ht/79,cb/79;
   *
   *    h/8079->cb/79,ch/68,ht/79,hs/44;
   *    ht/79->h/79,ch/68,cb/79,hs/44;
   *    ch/68->h/8079,cb/79,ht/79,hs/44;
   *
   *  Here is the correct mapping:
   *    h/79->cb/79,hs/44;
   *    cb/79->h/79,hs/44;
   *    hs/44->h/79,cb/79;
   *
   *    h/8079->ch/68,ht/79;
   *    ch/68->h/8079,ht/79;
   *    ht/79->h/8079,ch/68;
   */
  void fix_h79_h8079() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("79"),
        SongLink.newBuilder().setName("Cebuano").setReference(SongReference.newBuilder().setHymnType(
            CEBUANO).setHymnNumber("79")),
        SongLink.newBuilder().setName("Spanish").setReference(SongReference.newBuilder().setHymnType(
            SPANISH).setHymnNumber("44")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8079"),
        SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("68")),
        SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("68")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(HymnType.TAGALOG).setHymnNumber("79")));
  }

  /**
   * h/267 and h/1360 are related. However, the language mappings for each is all messed up.
   *
   *  Here is the current mapping:
   *    h/267->cb/267,ch/217,hf/46,ht/267,hs/127;
   *    cb/267->h/267,ch/217,hf/46,ht/267,hs/127;
   *    hf/46->cb/267,ch/217,h/267,de/267,ht/267,hs/127;
   *    ch/217->h/1360,cb/267,ht/1360,hs/127;
   *    ht/267->h/267,cb/267,ch/217hf/46,hs/127;
   *    hs/127->cb/267,ch/217,h/267,hf/46,de/267,ht/267;
   *    de/267->cb/267,ch/217,h/267,hf/46,hs/127,ht/267
   *
   *    h/1360->cb/267,ch/217,hs/127,ht/1360;
   *    ht/1360->cb/267,ch/217,h/1360,hs/127;
   *
   *  Here is the correct mapping:
   *    h/267->cb/267,ht/267,de/267,hs/127;
   *    cb/267->h/267,ht/267,de/267,hs/127;
   *    de/267->cb/267,h/267,ht/267,hs/127;
   *    ht/267->h/267,cb/267,de/267,hs/127;
   *    hs/127->h/267,cb/267,ht/267,de/267;
   *
   *    h/1360->ch/217,ht/1360,hf/46;
   *    ch/217->h/1360,ht/1360,hf/46;
   *    ht/1360->ch/217,h/1360,hf/46;
   *    hf/46->ch/217,h/1360,ht/1360;
   */
  void fix_h267_h1360() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("267"),
        SongLink.newBuilder().setName("Cebuano").setReference(SongReference.newBuilder().setHymnType(
            CEBUANO).setHymnNumber("267")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(HymnType.TAGALOG).setHymnNumber("267")),
        SongLink.newBuilder().setName("Spanish").setReference(SongReference.newBuilder().setHymnType(
            SPANISH).setHymnNumber("127")),
        SongLink.newBuilder().setName("German").setReference(SongReference.newBuilder().setHymnType(HymnType.GERMAN).setHymnNumber("267")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1360"),
        SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("217")),
        SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("217")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(HymnType.TAGALOG).setHymnNumber("1360")),
        SongLink.newBuilder().setName("French").setReference(SongReference.newBuilder().setHymnType(HymnType.FRENCH).setHymnNumber("46")));
  }

  /**
   * ts/253 and ts/253?gb=1 -> mapped to h/754 for some reason, but it actually is the chinese
   * version of h/1164. So it should map to h/1164 and its related songs
   */
  void fix_ts253() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL).setHymnNumber("253"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English")
            .setReference(
                SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1164")));
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED)
        .setHymnNumber("253"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English")
            .setReference(
                SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1164")));
  }

  /**
   *  ts/142 and ts/142?gb=1 -> mapped to h/1193 for some reason, but it actually is the chinese
   *  version of h/1198. So it should map to h/1198 and its related songs
   */
  void fix_ts142() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL).setHymnNumber("142"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English")
            .setReference(
                SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1198")));
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("142"))
        .clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1198")));
  }

  /**
   *  h/8526 is an alternate tune of h/720. In Hymnal.net, ch/526 and ch/526?gb=1 play the same tune
   *  as h/8526, while cb/720, ht/720, and de/720 play the same tune as h/720.
   *  So here is the correct mapping:
   *    h/720->cb/720,ht/720,de/720;
   *    cb/720->h/720,ht/720,de/720;
   *    ht/720->h/720,cb/720,de/720;
   *    de/720->h/720,cb/720,ht/720;
   *
   *    h/8526-> ch/526,chx/526;
   *    ch/526-> h/8526,chx/526;
   *    chx/526-> h/8526,ch/526;
   *
   *  In {@link Exceptions}, there is an exception, but that is for the relevants list. But here
   *  we still need to clean up the languages list.
   *
   *  TODO figure out if this should really be changed, or if it should be just an exception, since
   *    everything is the same song with the same lyrics, all the translations technically are
   *    translations of each other
   */
  void fix_h720_h8526() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("720"),
        SongLink.newBuilder().setName("Cebuano")
            .setReference(SongReference.newBuilder().setHymnType(CEBUANO).setHymnNumber("720")),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.TAGALOG).setHymnNumber("720")),
        SongLink.newBuilder().setName("German")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.GERMAN).setHymnNumber("720")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8526"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("526")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(
                SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("526")));
  }

  /**
   * h/379 should be by itself. The Chinese song it's linked to (ch/385) is actually translated by h/8385.
   */
  void fix_h379() {
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("379").build()).clearLanguages();
  }

  /**
   * h/1017, ch/693, and ch/643 are all kind of related. h/1017 is the full English song, ch/693 is
   * the full Chinese song, while ch/643 is just the verse repeated a few times. I'm making a
   * judgement call here to say that just having the chorus does NOT constitute a translation of the
   * song, and therefore, I am going to set ch/643 and ch/643?gb=1 to just have each other as
   * languages.
   */
  void fix_ch643() {
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("643")).clearLanguages()
        .addLanguages(SongLink.newBuilder().setName("诗歌(简)").setReference(
            SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("643")
                .build()));
    getHymn(SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED)
        .setHymnNumber("643")).clearLanguages().addLanguages(SongLink.newBuilder().setName("诗歌(简)")
        .setReference(
            SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("643").build()));
  }

  /**
   * h/528 should be by itself. The Chinese song it's linked to (ch/444) is actually translated by h/8444.
   */
  void fix_h528() {
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("528").build()).clearLanguages();
  }

  /**
   * h/480 should be by itself. The Chinese song it's linked to (ch/357) is actually translated by h/8357.
   */
  void fix_h480() {
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("480").build()).clearLanguages();
  }

  /**
   * Both ns/154 and h/8330 are translations of ch/330. However, there is only a mapping of ch/330
   * to h/8330. So we need to add that mapping to both ns/154 and ch/330.
   */
  void fix_ns154() {
    addLanguages(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("154"),
        SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("330")),
        SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("330")));
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("330"),
        SongLink.newBuilder().setName("English").setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("154")));
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("330"),
        SongLink.newBuilder().setName("English").setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("154")));
  }

  /**
   * Both ns/19 and ns/474 are translations of ts/428. However, ts/428 only references ns/19. So we
   * need to add ns/474 as well.
   */
  void fix_ts438() {
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("428"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("474")));
    addLanguages(
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("428"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("474")));
  }

  /**
   * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/723, we are going to keep the language
   * mapping of the original song of h/723 and remove the language mapping for nt/723. Even though
   * this is technically incorrect, it will be easier navigate to the Chinese version from the
   * classic hymn than from the new tune.
   */
  void fix_nt723() {
    getHymn(SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("723").build()).clearLanguages();
  }

  /**
   * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/1307, we are going to keep the
   * language mapping of the original song of h/723 and remove the language mapping for nt/1307.
   * Even though this is technically incorrect, it will be easier navigate to the Chinese version
   * from the classic hymn than from the new tune.
   */
  void fix_nt1307() {
    getHymn(SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("1307").build()).clearLanguages();
  }

  /**
   * de/10 and de/10b are the German translations of h/10 and h/10b. However, the mapping is kind of
   * messed up. de/10 has the same tune is h/10b and de/10b has the tune of h/10. It makes it very
   * difficult to do the mapping here, so we are just going to keep it simple and map h/10 to de/10
   * and h/10b to de/10b, even though it's technically incorrect. The mitigation here is that it
   * will still be possible to navigate between then because of the Relevant relationship between
   * each of the two songs.
   *
   * The good news though, is that there exists only a one-way mapping from h/10b to the original
   * tunes, so we can just remove all languages from h/10b and set it only to de/10b and that should
   * solve any invalid reference checks.
   */
  void fix_de10_h10b() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("10b"),
        SongLink.newBuilder().setName("German").setReference(
            SongReference.newBuilder().setHymnType(GERMAN).setHymnNumber("10b").build()));
  }

  /**
   * de/786b is the German translation of h/786 and h/786b, but adheres to the tune of h/786b. So we should remove its
   * references to songs with the same tune as h/786 and only keep h/786b.
   */
  void fix_de786b_h786b() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("786b"),
        SongLink.newBuilder().setName("German")
            .setReference(SongReference.newBuilder().setHymnType(GERMAN).setHymnNumber("786b")));
  }

  /**
   * ch/9166 incorrectly maps to h/157 and ht/157 when it should map to h/166 and ht/166. Everything
   * else is correct. so just change ch/9166 to the correct mapping.
   */
  void fix_ch9166() {
    getHymn(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("9166")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("166")));
    getHymn(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("9166")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("166")));
  }

  /**
   * ns/54de is the German song of ns/54. ns/54 references ns/54de as German, but ns/54de just
   * references itself also as German. So we need to fix ns/54de to point back to ns/54.
   */
  void fix_ns54de() {
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("54de")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("54")));
  }

  /**
   * hd/31 is the Dutch song of ns/79 but is referenced by h/31. So we need to remove that mapping.
   */
  void fix_hd31() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("31"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("29")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("29")));
  }

  /**
   * ch/632 is the Chinese song of h/870, but it references h/870b instead, which is a new tune.
   */
  void fix_ch632() {
    getHymn(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("632")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("870")));
    getHymn(
        SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("632")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("870")));
  }

  /**
   * ts/248 is the Chinese song of h/300, but it references h/300b instead, which is a new tune.
   */
  void fix_ts248() {
    getHymn(
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("248")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("300")));
    getHymn(
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED)
            .setHymnNumber("248")).clearLanguages()
        .addLanguages(
            SongLink.newBuilder().setName("English")
                .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("300")));
  }

  /**
   * nt/477b should link to nt/477 as a relevant song, but instead contains a self-link.
   */
  void fix_nt_477b() {
    // Replace self link with nt/477
    SongReference nt477b = SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("477b").build();
    List<SongLink> newRelevants = getHymn(nt477b).getRelevantsList().stream()
        .map(
            songLink -> {
              if (songLink.getReference().equals(nt477b)) {
                return SongLink.newBuilder().setName(songLink.getName()).setReference(
                    SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("477").build()).build();
              }
              return songLink;
            }).collect(Collectors.toList());
    getHymn(nt477b).clearRelevants().addAllRelevants(newRelevants);
  }

  /**
   * nt/377 has a relevant song of nt/1079, but they are not really related. So remove it from the relevant list
   */
  void fix_nt377() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("377"),
        SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("1079"));
  }

  /**
   * ns/98 has a relevant song of ns/80, but they are not really related. So remove it from the relevant list
   */
  void fix_ns98() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("98"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("80"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("80"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("98"));
  }

  /**
   * ns/575, ns/34, and h/711 are in a large web of interconnected songs that need to be unraveled.
   * They all reference each other (and others) in some way, but aren't really related to each other
   */
  void fix_nt575_ns34_h711() {
    fix_nt575();
    fix_ns34();
    fix_h711();
  }

  /**
   * nt/575 is the new tune for h/575, but it also has a bunch of relevant songs (ns/34, nt/711, nt/1079)
   * that it is not related to, so we should remove them.
   */
  void fix_nt575() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("575"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("34"),
        SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("711"),
        SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("1079"));
  }

  /**
   * ch/9575 is the Chinese translation of h/575. The new tune of h/575 is nt/575, and that song in
   * Chinese is ch/nt575c
   */
  void fix_ch9575_chnt575c() {
    resetRelevants(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("9575"),
        SongLink.newBuilder().setName("New Tune")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("nt575c")));

    resetRelevants(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("9575"),
        SongLink.newBuilder().setName("New Tune")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("nt575c")));
  }

  /**
   * ns/34 doesn't have any new/alternaete tunes, but just references a bunch of songs that it isn't
   * related to. So we can just clear the relevants list there.
   */
  void fix_ns34() {
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("34").build()).clearRelevants();
  }

  /**
   * h/711 has two new tunes: nt/711 and nt/711b, but also references ns/34, even though it's not
   * related.
   */
  void fix_h711() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("711"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("34"));
  }

  /**
   * h/635, h/481, h/631 reference each other, but they're not really related
   */
  void fix_h635_h481_h631() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("635"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("481"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("631"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("481"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("635"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("631"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("631"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("481"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("635"));
  }

  /**
   * ns/59, ns/110, ns/111 reference each other, but they're not really related
   */
  void fix_ns59_ns110_ns111() {
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("59")).clearRelevants();
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("110")).clearRelevants();
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("111")).clearRelevants();
  }

  /**
   * ns/2 references ns/3, but they're not really related
   */
  void fix_ns2() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("2"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("3"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("3"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("2"));
  }

  /**
   * ns/4 references ns/5 and h/36, but they're not really related
   */
  void fix_ns4() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("4"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("36"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("4"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("5"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("5"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("4"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("5"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("36"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("36"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("4"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("36"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("5"));
  }

  /**
   * ns/10, ns/142 reference each other, but they're not really related
   */
  void fix_ns10_ns142() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("10"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("142"));
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("142"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("10"));
  }

  /**
   * h/1033 references h/1007, but they're not really related
   */
  void fix_h1033() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1033"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1007"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1007"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1033"));
  }

  /**
   * h/1162, h/1163 reference each other, but they're not really related
   */
  void fix_h1162_h1163() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1162"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1163"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1163"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1162"));
  }

  /**
   * ns/73 references ns/34 and nt/711 but is not really related to those
   */
  void fix_ns73() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("73"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("34"),
        SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("711"));
  }

  /**
   * ns/53 references ns/34, but they're not really related
   */
  void fix_ns53() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("53"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("34"));
  }

  /**
   * ns/1 references h/278, but they're not really related, apart from having the same tune
   */
  void fix_ns1() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("1"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("278"));
  }

  /**
   * ns/8 references h/1282, but they're not really related, apart from having the same tune
   */
  void fix_ns8() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("8"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1282"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1282"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("8"));
  }

  /**
   * ns/12 references h/661, but they're not really related, apart from having the same tune
   */
  void fix_ns12() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("12"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("661"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("661"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("12"));
  }

  /**
   * ns/22 references h/313, but they're not really related, apart from having the same tune
   */
  void fix_ns22() {
    removeRelevants(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("22"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("313"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("313"),
        SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("22"));
  }

  /**
   * c/31 references h/1014, but they're not really related, apart from having the same tune in the chorus
   */
  void fix_c31() {
    removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("31"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1014"));
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1014"),
        SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("31"));
  }

  /**
   * c/113 references h/556, but they're not really related, apart from having the same tune
   */
  void fix_c113() {
    removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("113"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("556"));
  }

  /**
   * h/396 and ns/313 references each other, but they're not really related, apart from having the same tune
   */
  void fix_h396_ns313() {
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("396")).clearRelevants();
    getHymn(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("313")).clearRelevants();
  }

  /**
   * h/383 references itself when it should really be referencing nt/383
   */
  void fix_h383() {
    resetRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("383"),
        SongLink.newBuilder().setName("New Tune")
            .setReference(SongReference.newBuilder().setHymnType(NEW_TUNE).setHymnNumber("383")));
  }

  /**
   * c/162 has a relevant song of h/993, but they are not really related. So remove it from the relevant list
   */
  void fix_c162() {
    removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("162"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("993"));
  }

  /**
   * h/163 references itself when it should really be referencing nothing
   */
  void fix_h163() {
    removeRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("163"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("163"));
  }

  /**
   * Fix references where something points to a hymn, but it doesn't point back (i.e. dangling).
   *
   * This is difficult to automate because we don't often know what the language of the dangling
   * reference is. For instance, ns/568 references ns/568c and ns/568?gb=1. However, those songs
   * don't reference it back. It is difficult in code to know that ns/568c is the Chinese version of
   * ns/568, so we won't know how to set the "value" field in the Datum. Thankfully, there are only
   * ~20 of these, so we can fix them manually.
   */
  void fix_danglingReferences() {
    // ns/568 references ch/ns568c and chx/ns568. However, those songs don't reference it back. Fix the mapping so they reference each other.
    addLanguages(SongReference.newBuilder().setHymnType(HymnType.CHINESE).setHymnNumber("ns568c"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.NEW_SONG).setHymnNumber("568")));
    addLanguages(
        SongReference.newBuilder().setHymnType(HymnType.CHINESE_SIMPLIFIED).setHymnNumber("ns568c"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.NEW_SONG).setHymnNumber("568")));

    // ns/195 references ts/228 and ts/228?gb=1 but those songs don't reference it back. Fix the
    // mapping so they reference each other.
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("228"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(HymnType.NEW_SONG).setHymnNumber("195")));
    addLanguages(
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("228"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("195")));

    // hd/4 references ns/257 but ns/257 doesn't reference it back. Fix the mapping so it does.
    addLanguages(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("257"),
        SongLink.newBuilder().setName("Dutch")
            .setReference(SongReference.newBuilder().setHymnType(DUTCH).setHymnNumber("4")));

    // ns/7 references h/18 because it is an adaptation of it. However, h/18 doesn't reference it
    // back. Fix h/18 to reference it back.
    addRelevants(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("18"),
        SongLink.newBuilder().setName("O Father God, how faithful You are")
            .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("7")));

    // ns/20 references lb/12 because it is an adaptation of it. However, lb/12 doesn't reference it back.
    addRelevants(SongReference.newBuilder().setHymnType(HOWARD_HIGASHI).setHymnNumber("12"),
        SongLink.newBuilder().setName("Lord, I still love You")
            .setReference(SongReference.newBuilder().setHymnType(NEW_SONG).setHymnNumber("20")));

    // c/21 references h/70 because it is a shortened version of it. Since they are essentially the same song, we
    // should change the name to "Related" instead of the song title, because otherwise it'll just be the same as
    // the current song title.
    removeRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("21"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("70"));
    removeRelevants(SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN).setHymnNumber("70"),
        SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("21"));
    addRelevants(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("21"),
        SongLink.newBuilder().setName("Related")
            .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("70")));
    addRelevants(SongReference.newBuilder().setHymnType(HymnType.CLASSIC_HYMN).setHymnNumber("70"),
        SongLink.newBuilder().setName("Related")
            .setReference(SongReference.newBuilder().setHymnType(CHILDREN_SONG).setHymnNumber("21")));
  }
}