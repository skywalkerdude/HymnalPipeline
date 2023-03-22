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
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("68"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("79"));
  }

  /**
   * The parent of i/330 in H4a database is bf/157, when it should be ch/330, since it is the
   * Indonesian translation of ch/330.
   */
  private void fix_i330() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("330"),
        SongReference.newBuilder().setType(BE_FILLED).setNumber("157"));
    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("330"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setType(CHINESE).setNumber("330")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("330")));
  }

  /**
   * The parent of i/773 in H4a database is bf/69, when it should be ch/773, since it is the
   * Indonesian translation of ch/773.
   */
  private void fix_i773() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("773"),
        SongReference.newBuilder().setType(BE_FILLED).setNumber("69"));
    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("773"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setType(CHINESE).setNumber("773")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("773")));
  }

  /**
   * bf/69 is in hymnal.net as h/8773. Since it already exists in hymnal.net, we can remove it from
   * H4a.
   */
  private void fix_bf69() {
    removeReference(SongReference.newBuilder().setType(BE_FILLED).setNumber("69"));
  }

  /**
   * i/269's parent hymn should be ch/289, not ch/269. Similarly, h/367's Indonesian translation is
   * i/289, but it also includes i/269.
   */
  private void fix_i269_h367() {
    clearLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("269"));
    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("269"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(SongReference.newBuilder().setType(CHINESE).setNumber("269")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("269")));

    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("367"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("269"));
  }

  /**
   * Based on {@link HymnalNetPatcher#fix_h720_h8526()}, h/720 should not have ch/526 and chx/526 as
   * Chinese references. i/526 is the Indonesian version of ch/526, so it also should not have h/720
   * as a reference.
   */
  private void fix_i526() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("526"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("720"));
  }

  /**
   * i/485 has h/666 as a mapping, but that is a mistake. i/485 has no English translation yet.
   */
  private void fix_i485_h666() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("485"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("666"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("666"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("485"));
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
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("664"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("921"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("921"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("664"));
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
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("643"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1017"));
    removeLanguages(SongReference.newBuilder().setType(JAPANESE).setNumber("643"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1017"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1017"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("643"),
        SongReference.newBuilder().setType(JAPANESE).setNumber("643"));

    addLanguages(SongReference.newBuilder().setType(JAPANESE).setNumber("643"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("643")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("643")));
  }

  /**
   * i/1401 references ts/401 and h/1191. That's not right, because h/1191's Chinese song is ts/410,
   * not ts/401. So we need to remove h/1191 as a mapping of i/1401.
   */
  private void fix_i1401() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("1401"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1191"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1191"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("1401"));
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
    addLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("31"),
        SongLink.newBuilder().setName("Tagalog")
            .setReference(SongReference.newBuilder().setType(TAGALOG).setNumber("31")));
  }

  /**
   * k/1014 matches to h/1248, but it should match to h/1295 instead
   */
  private void fix_k1014_h1248() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("1014"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1248"));
    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("1014"),
        SongLink.newBuilder().setName("English")
            .setReference(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1295")));

    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1248"),
        SongReference.newBuilder().setType(KOREAN).setNumber("1014"));
  }

  /**
   * i/1832 is mapped to ts/823, but it should be mapped to ts/832 instead. Most likely a typo.
   */
  private void fix_i1832() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("1832"),
        SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("823"),
        SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL_SIMPLIFIED).setNumber("823"));
    removeLanguages(SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("823"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("1832"));

    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("1832"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("832")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL_SIMPLIFIED)
                .setNumber("832")));
  }

  /**
   * i/42's parent should be ch/42, not ch/43. Mostly likely a typo in H4a.
   */
  private void fix_i43() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("42"),
        SongReference.newBuilder().setType(CHINESE).setNumber("43"));
    removeLanguages(SongReference.newBuilder().setType(CHINESE).setNumber("43"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("42"));

    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("42"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("42")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("42")));
  }

  /**
   * i/1531's should be linked ts/531, not ts/513 and h/1348. Mostly likely a typo in H4a.
   */
  private void fix_i1531() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("1531"),
        SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("513"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1348"));
    removeLanguages(SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("513"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("1531"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1348"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("1531"));

    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("1531"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL).setNumber("531")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SUPPLEMENTAL_SIMPLIFIED)
                .setNumber("531")));
  }

  /**
   * k/460 shouldn't map to h/605, but should instead map to ch/460, which is the Chinese version of
   * h/623.
   */
  private void fix_k460() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("460"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("605"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("605"),
        SongReference.newBuilder().setType(KOREAN).setNumber("460"));

    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("460"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("460")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("460")));
  }

  /**
   * j/539 shouldn't map to h/734, but should instead map to ch/539, which is the Chinese version of
   * h/743.
   */
  private void fix_j539() {
    removeLanguages(SongReference.newBuilder().setType(JAPANESE).setNumber("539"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("734"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("734"),
        SongReference.newBuilder().setType(JAPANESE).setNumber("539"));

    addLanguages(SongReference.newBuilder().setType(JAPANESE).setNumber("539"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("539")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("539")));
  }

  /**
   * k/57 shouldn't map to h/51, but should instead map to ch/57, which is the Chinese version of
   * h/61.
   */
  private void fix_k57() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("57"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("51"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("51"),
        SongReference.newBuilder().setType(KOREAN).setNumber("57"));

    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("57"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("57")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("57")));
  }

  /**
   * k/319 shouldn't map to h/419, but should instead map to ch/319.
   */
  private void fix_k319() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("319"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("419"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("419"),
        SongReference.newBuilder().setType(KOREAN).setNumber("319"));

    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("319"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("319")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("319")));
  } // should have 6 errors left

  /**
   * i/709's should be linked ch/709, not ch/708 and h/1028. Mostly likely a typo in H4a.
   */
  private void fix_i709() {
    removeLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("709"),
        SongReference.newBuilder().setType(CHINESE).setNumber("708"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1028"));
    removeLanguages(SongReference.newBuilder().setType(CHINESE).setNumber("708"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("709"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("1028"),
        SongReference.newBuilder().setType(INDONESIAN).setNumber("709"));

    addLanguages(SongReference.newBuilder().setType(INDONESIAN).setNumber("709"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("709")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("709")));
  }

  /**
   * k/667 shouldn't map to h/894, but should instead map to ch/667, which is the Chinese version of
   * h/1349.
   */
  private void fix_k667() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("667"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("894"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("894"),
        SongReference.newBuilder().setType(KOREAN).setNumber("667"));

    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("667"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("667")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("667")));
  }

  /**
   * k/372 shouldn't map to h/494, but should instead map to ch/372, which is the Chinese version of
   * h/495.
   */
  private void fix_k372() {
    removeLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("372"),
        SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("494"));
    removeLanguages(SongReference.newBuilder().setType(CLASSIC_HYMN).setNumber("494"),
        SongReference.newBuilder().setType(KOREAN).setNumber("372"));

    addLanguages(SongReference.newBuilder().setType(KOREAN).setNumber("372"),
        SongLink.newBuilder().setName("詩歌(繁)")
            .setReference(
                SongReference.newBuilder().setType(CHINESE).setNumber("372")),
        SongLink.newBuilder().setName("诗歌(简)")
            .setReference(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED)
                .setNumber("372")));
  }

  /**
   * ch/276 is translated in Hymnal.net by h/8276. bf/231 is an alternate translation that is very
   * similar to h/8276, but slightly different. So here, we can add a language to ch/276 as a 'Be
   * Filled' translation as an exception to the regular rule that each song only has one English
   * translation, since here there are clearly two.
   */
  private void fix_bf231() {
    addLanguages(SongReference.newBuilder().setType(CHINESE).setNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setType(BE_FILLED).setNumber("231")));
    addLanguages(SongReference.newBuilder().setType(CHINESE_SIMPLIFIED).setNumber("276"),
        SongLink.newBuilder().setName("Be Filled")
            .setReference(
                SongReference.newBuilder().setType(BE_FILLED).setNumber("231")));
  }
}
