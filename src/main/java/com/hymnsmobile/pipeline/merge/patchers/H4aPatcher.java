package com.hymnsmobile.pipeline.merge.patchers;

import static com.hymnsmobile.pipeline.merge.HymnType.BE_FILLED;
import static com.hymnsmobile.pipeline.merge.HymnType.CEBUANO;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.merge.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.merge.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.merge.HymnType.GERMAN;
import static com.hymnsmobile.pipeline.merge.HymnType.INDONESIAN;
import static com.hymnsmobile.pipeline.merge.HymnType.JAPANESE;
import static com.hymnsmobile.pipeline.merge.HymnType.KOREAN;
import static com.hymnsmobile.pipeline.merge.HymnType.SPANISH;
import static com.hymnsmobile.pipeline.merge.HymnType.TAGALOG;

import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Set;
import javax.inject.Inject;

/**
 * Performs one-off patches to the set of hymns from Hymns For Android that are unfixable with a
 * general algorithm.
 */
@MergeScope
public class H4aPatcher extends Patcher {

  @Inject
  public H4aPatcher(@Merge Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performPatch() {
    fix_h79_h8079();
    fix_i330();
    fix_i773();
    fix_bf69();
    fix_i269_h367();
    fix_h720_h8526();
    fix_i485_h666();
    fix_i664();
    fix_i643_j643();
    fix_i1401();
    fix_h31();
    fix_k1014_h1248();
    fix_i1832();
    fix_i43();
    fix_i1531();
    fix_k460();
    fix_j539();
    fix_k57();
    fix_k319();
    fix_i709();
    fix_k667();
    fix_k372();
    fix_bf231();
  }

  /**
   * We basically need to redo {@link HymnalNetPatcher#  void fix_h720_h8526() {()} here, because
   * H4A added the incorrect links back in. However, we should also take i/68 into account. So the
   * new correct mapping is:
   *    h/79->cb/79,hs/44;
   *    cb/79->h/79,hs/44;
   *    hs/44->h/79,cb/79;
   *
   *    h/8079->ch/68,ht/79,i/68;
   *    ch/68->h/8079,ht/79,i/68;
   *    ht/79->h/8079,ch/68,i/68;
   *    i/68->h/8079,ch/68,ht/79;
   */
  private void fix_h79_h8079() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("79"),
        SongLink.newBuilder().setName("Cebuano").setReference(SongReference.newBuilder().setHymnType(
            CEBUANO.abbreviatedValue).setHymnNumber("79")),
        SongLink.newBuilder().setName("Spanish").setReference(SongReference.newBuilder().setHymnType(
            SPANISH.abbreviatedValue).setHymnNumber("44")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8079"),
        SongLink.newBuilder().setName("詩歌(繁)").setReference(SongReference.newBuilder().setHymnType(
            CHINESE.abbreviatedValue).setHymnNumber("68")),
        SongLink.newBuilder().setName("诗歌(简)").setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("68")),
        SongLink.newBuilder().setName("Tagalog").setReference(SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("79")),
        SongLink.newBuilder().setName("Indonedian").setReference(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("68")));
  }

  /**
   * The parent of i/330 in H4a database is bf/157, when it should be ch/330, since it is the
   * Indonesian translation of ch/330.
   */
  private void fix_i330() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("330"),
        SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("157"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("330"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("330")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("330")));
  }

  /**
   * The parent of i/773 in H4a database is bf/69, when it should be ch/773, since it is the
   * Indonesian translation of ch/773.
   */
  private void fix_i773() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("773"),
        SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("69"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("773"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("773")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("773")));
  }

  /**
   * bf/69 is in hymnal.net as h/8773. Since it already exists in hymnal.net, we can remove it from
   * H4a.
   */
  private void fix_bf69() {
    removeReference(SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("69"));
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8773"))
        .addReferences(SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("69"));
  }

  /**
   * i/269's parent hymn should be ch/289, not ch/269. Similarly, h/367's Indonesian translation is
   * i/289, but it also includes i/269.
   */
  private void fix_i269_h367() {
    clearLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("269"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("269"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("269")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("269")));

    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("367"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("269"));
  }

  /**
   * We basically need to redo {@link HymnalNetPatcher#fix_h720_h8526} here, because
   * H4A added the incorrect links back in. However, we should also take i/526 into account. So the
   * new correct mapping is:
   *    h/720->cb/720,ht/720,de/720;
   *    cb/720->h/720,ht/720,de/720;
   *    ht/720->h/720,cb/720,de/720;
   *    de/720->h/720,cb/720,ht/720;
   *
   *    h/8526-> ch/526,chx/526,i/526;
   *    ch/526-> h/8526,chx/526,i/526;
   *    chx/526-> h/8526,ch/526,i/526;
   *    i/526-> h/8526,ch/526,chx/526;
   */
  private void fix_h720_h8526() {
    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("720"),
        SongLink.newBuilder().setName("Cebuano")
            .setReference(SongReference.newBuilder().setHymnType(CEBUANO.abbreviatedValue).setHymnNumber("720")),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("720")),
        SongLink.newBuilder().setName("German")
            .setReference(SongReference.newBuilder().setHymnType(GERMAN.abbreviatedValue).setHymnNumber("720")));

    resetLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("8526"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("526")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("526")),
        SongLink.newBuilder().setName("Indonesian")
            .setReference(
                SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("526")));
  }

  /**
   * i/485 has h/666 as a mapping, but that is a mistake. i/485 has no English translation yet.
   */
  private void fix_i485_h666() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("485"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("666"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("666"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("485"));
  }

  /**
   * h/1358 is a 1-verse song called "Rise! Preach the Gospel now!". h/921 is a 4-verse song called
   * "Rescue the perishing".
   *
   * These are two different songs with the same tune. Therefore, they should be separate in terms
   * of language translations. However, they are confounded by i/664, which references both h/921
   * and ch/664. We need to remove h/921.
   */
  private void fix_i664() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("664"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("921"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("921"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("664"));
  }

  /**
   * Based on {@link HymnalNetPatcher#fix_ch643()}, ch/643 should have no references to non-Chinese
   * songs. However, H4a adds i/643, which is the Indonesian translation of ch/643. However, the
   * link to h/1017 is not correct.
   *
   * A similar thing applies to j/643. It should only map to ch/643, and not h/1017 for the same
   * reason. However, it only maps to h/1017, so we need to remove it and add ch/643 to its
   * mapping.
   */
  private void fix_i643_j643() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1017"));
    removeLanguages(SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1017"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1017"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("643"));

    addLanguages(SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("643"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("643")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("643")));
  }

  /**
   * i/1401 references ts/401 and h/1191. That's not right, because h/1191's Chinese song is ts/410,
   * not ts/401. So we need to remove h/1191 as a mapping of i/1401.
   */
  private void fix_i1401() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1401"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1191"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1191"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1401"));
  }

  /**
   * ht/31 is a legitimate translation of h/31 that is missing from Hymnal.net. However, h/31 does
   * not reference it back, so we need to fix it here.
   *
   * Note: This can also be fixed by inferring the {@link SongLink#name_} of TAGALOG hymns, but that
   * may suppress legitimate errors related to Tagalog songs, so we opted for one-off patches for
   * the few tagalog songs that are legitimate translations but are missing from Hymnal.net.
   */
  private void fix_h31() {
    addLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("31"),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(TAGALOG.abbreviatedValue).setHymnNumber("31")));
  }

  /**
   * k/1014 matches to h/1248, but it should match to h/1295 instead
   */
  private void fix_k1014_h1248() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("1014"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1248"));
    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("1014"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1295")));

    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1248"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("1014"));
  }

  /**
   * i/1832 is mapped to ts/823, but it should be mapped to ts/832 instead. Most likely a typo.
   */
  private void fix_i1832() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1832"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("823"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue).setHymnNumber("823"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("823"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1832"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1832"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("832")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue)
                .setHymnNumber("832")));
  }

  /**
   * i/42's parent should be ch/42, not ch/43. Mostly likely a typo in H4a.
   */
  private void fix_i43() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("42"),
        SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("43"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("43"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("42"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("42"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("42")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("42")));
  }

  /**
   * i/1531's should be linked ts/531, not ts/513 and h/1348. Mostly likely a typo in H4a.
   */
  private void fix_i1531() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1531"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("513"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1348"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("513"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1531"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1348"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1531"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("1531"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL.abbreviatedValue).setHymnNumber("531")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED.abbreviatedValue).setHymnNumber("531")));
  }

  /**
   * k/460 shouldn't map to h/605, but should instead map to ch/460, which is the Chinese version of
   * h/623.
   */
  private void fix_k460() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("460"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("605"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("605"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("460"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("460"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("460")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("460")));
  }

  /**
   * j/539 shouldn't map to h/734, but should instead map to ch/539, which is the Chinese version of
   * h/743.
   */
  private void fix_j539() {
    removeLanguages(SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("539"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("734"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("734"),
        SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("539"));

    addLanguages(SongReference.newBuilder().setHymnType(JAPANESE.abbreviatedValue).setHymnNumber("539"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("539")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("539")));
  }

  /**
   * k/57 shouldn't map to h/51, but should instead map to ch/57, which is the Chinese version of
   * h/61.
   */
  private void fix_k57() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("57"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("51"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("51"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("57"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("57"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("57")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("57")));
  }

  /**
   * k/319 shouldn't map to h/419, but should instead map to ch/319.
   */
  private void fix_k319() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("319"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("419"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("419"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("319"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("319"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("319")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("319")));
  } // should have 6 errors left

  /**
   * i/709's should be linked ch/709, not ch/708 and h/1028. Mostly likely a typo in H4a.
   */
  private void fix_i709() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("709"),
        SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("708"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1028"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("708"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("709"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("1028"),
        SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("709"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN.abbreviatedValue).setHymnNumber("709"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("709")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("709")));
  }

  /**
   * k/667 shouldn't map to h/894, but should instead map to ch/667, which is the Chinese version of
   * h/1349.
   */
  private void fix_k667() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("667"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("894"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("894"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("667"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("667"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("667")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("667")));
  }

  /**
   * k/372 shouldn't map to h/494, but should instead map to ch/372, which is the Chinese version of
   * h/495.
   */
  private void fix_k372() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("372"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("494"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN.abbreviatedValue).setHymnNumber("494"),
        SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("372"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN.abbreviatedValue).setHymnNumber("372"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("372")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("372")));
  }

  /**
   * ch/276 is translated in Hymnal.net by h/8276. bf/231 is an alternate translation that is very
   * similar to h/8276, but slightly different. So here, we can add a language to ch/276 as a 'Be
   * Filled' translation as an exception to the regular rule that each song only has one English
   * translation, since here there are clearly two.
   */
  private void fix_bf231() {
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE.abbreviatedValue).setHymnNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("231")));
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED.abbreviatedValue).setHymnNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setHymnType(BE_FILLED.abbreviatedValue).setHymnNumber("231")));
  }
}
