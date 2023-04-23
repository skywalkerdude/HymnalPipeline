package com.hymnsmobile.pipeline.sanitization.patchers
    ;

import static com.hymnsmobile.pipeline.models.HymnType.BE_FILLED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL;
import static com.hymnsmobile.pipeline.models.HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED;
import static com.hymnsmobile.pipeline.models.HymnType.CLASSIC_HYMN;
import static com.hymnsmobile.pipeline.models.HymnType.INDONESIAN;
import static com.hymnsmobile.pipeline.models.HymnType.JAPANESE;
import static com.hymnsmobile.pipeline.models.HymnType.KOREAN;
import static com.hymnsmobile.pipeline.models.HymnType.TAGALOG;

import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import com.hymnsmobile.pipeline.sanitization.dagger.Sanitization;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationScope;
import java.util.Set;
import javax.inject.Inject;

/**
 * Performs one-off patches to the set of hymns from Hymns For Android that are unfixable with a
 * general algorithm.
 */
@SanitizationScope
public class H4aPatcher extends Patcher {

  @Inject
  public H4aPatcher(@Sanitization Set<PipelineError> errors) {
    super(errors);
  }

  @Override
  protected void performPatch() {
    fix_i68();
    fix_i330();
    fix_i773();
    fix_bf69();
    fix_i269_h367();
    fix_i526();
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
   * Based on {@link HymnalNetPatcher#fix_h79_h8079()}, h/79 should not have ch/68 and chx/68 as
   * Chinese references. i/68 is the Indonesian version of ch/68, so it also should not have h/79 as
   * a reference.
   */
  private void fix_i68() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("68"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("79"));
  }

  /**
   * The parent of i/330 in H4a database is bf/157, when it should be ch/330, since it is the
   * Indonesian translation of ch/330.
   */
  private void fix_i330() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("330"),
        SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("157"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("330"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("330")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("330")));
  }

  /**
   * The parent of i/773 in H4a database is bf/69, when it should be ch/773, since it is the
   * Indonesian translation of ch/773.
   */
  private void fix_i773() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("773"),
        SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("69"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("773"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("773")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("773")));
  }

  /**
   * bf/69 is in hymnal.net as h/8773. Since it already exists in hymnal.net, we can remove it from
   * H4a.
   */
  private void fix_bf69() {
    removeReference(SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("69"));
    getHymn(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("8773"))
        .addReferences(SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("69"));
  }

  /**
   * i/269's parent hymn should be ch/289, not ch/269. Similarly, h/367's Indonesian translation is
   * i/289, but it also includes i/269.
   */
  private void fix_i269_h367() {
    clearLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("269"));
    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("269"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("269")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("269")));

    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("367"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("269"));
  }

  /**
   * Based on {@link HymnalNetPatcher#fix_h720_h8526()}, h/720 should not have ch/526 and chx/526 as
   * Chinese references. i/526 is the Indonesian version of ch/526, so it also should not have h/720
   * as a reference.
   */
  private void fix_i526() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("526"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("720"));
  }

  /**
   * i/485 has h/666 as a mapping, but that is a mistake. i/485 has no English translation yet.
   */
  private void fix_i485_h666() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("485"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("666"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("666"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("485"));
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
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("664"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("921"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("921"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("664"));
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
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1017"));
    removeLanguages(SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1017"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1017"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("643"),
        SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("643"));

    addLanguages(SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("643"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("643")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("643")));
  }

  /**
   * i/1401 references ts/401 and h/1191. That's not right, because h/1191's Chinese song is ts/410,
   * not ts/401. So we need to remove h/1191 as a mapping of i/1401.
   */
  private void fix_i1401() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1401"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1191"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1191"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1401"));
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
    addLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("31"),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setHymnType(TAGALOG).setHymnNumber("31")));
  }

  /**
   * k/1014 matches to h/1248, but it should match to h/1295 instead
   */
  private void fix_k1014_h1248() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("1014"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1248"));
    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("1014"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1295")));

    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1248"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("1014"));
  }

  /**
   * i/1832 is mapped to ts/823, but it should be mapped to ts/832 instead. Most likely a typo.
   */
  private void fix_i1832() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1832"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("823"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setHymnNumber("823"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("823"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1832"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1832"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("832")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED)
                .setHymnNumber("832")));
  }

  /**
   * i/42's parent should be ch/42, not ch/43. Mostly likely a typo in H4a.
   */
  private void fix_i43() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("42"),
        SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("43"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("43"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("42"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("42"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("42")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("42")));
  }

  /**
   * i/1531's should be linked ts/531, not ts/513 and h/1348. Mostly likely a typo in H4a.
   */
  private void fix_i1531() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1531"),
        SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("513"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1348"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("513"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1531"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1348"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1531"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("1531"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL).setHymnNumber("531")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SUPPLEMENTAL_SIMPLIFIED)
                .setHymnNumber("531")));
  }

  /**
   * k/460 shouldn't map to h/605, but should instead map to ch/460, which is the Chinese version of
   * h/623.
   */
  private void fix_k460() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("460"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("605"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("605"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("460"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("460"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("460")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("460")));
  }

  /**
   * j/539 shouldn't map to h/734, but should instead map to ch/539, which is the Chinese version of
   * h/743.
   */
  private void fix_j539() {
    removeLanguages(SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("539"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("734"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("734"),
        SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("539"));

    addLanguages(SongReference.newBuilder().setHymnType(JAPANESE).setHymnNumber("539"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("539")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("539")));
  }

  /**
   * k/57 shouldn't map to h/51, but should instead map to ch/57, which is the Chinese version of
   * h/61.
   */
  private void fix_k57() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("57"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("51"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("51"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("57"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("57"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("57")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("57")));
  }

  /**
   * k/319 shouldn't map to h/419, but should instead map to ch/319.
   */
  private void fix_k319() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("319"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("419"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("419"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("319"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("319"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("319")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("319")));
  } // should have 6 errors left

  /**
   * i/709's should be linked ch/709, not ch/708 and h/1028. Mostly likely a typo in H4a.
   */
  private void fix_i709() {
    removeLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("709"),
        SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("708"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1028"));
    removeLanguages(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("708"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("709"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("1028"),
        SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("709"));

    addLanguages(SongReference.newBuilder().setHymnType(INDONESIAN).setHymnNumber("709"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("709")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("709")));
  }

  /**
   * k/667 shouldn't map to h/894, but should instead map to ch/667, which is the Chinese version of
   * h/1349.
   */
  private void fix_k667() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("667"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("894"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("894"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("667"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("667"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("667")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("667")));
  }

  /**
   * k/372 shouldn't map to h/494, but should instead map to ch/372, which is the Chinese version of
   * h/495.
   */
  private void fix_k372() {
    removeLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("372"),
        SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("494"));
    removeLanguages(SongReference.newBuilder().setHymnType(CLASSIC_HYMN).setHymnNumber("494"),
        SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("372"));

    addLanguages(SongReference.newBuilder().setHymnType(KOREAN).setHymnNumber("372"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("372")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED)
                .setHymnNumber("372")));
  }

  /**
   * ch/276 is translated in Hymnal.net by h/8276. bf/231 is an alternate translation that is very
   * similar to h/8276, but slightly different. So here, we can add a language to ch/276 as a 'Be
   * Filled' translation as an exception to the regular rule that each song only has one English
   * translation, since here there are clearly two.
   */
  private void fix_bf231() {
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE).setHymnNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("231")));
    addLanguages(SongReference.newBuilder().setHymnType(CHINESE_SIMPLIFIED).setHymnNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setHymnType(BE_FILLED).setHymnNumber("231")));
  }
}
