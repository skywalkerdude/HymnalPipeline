package com.hymnsmobile.pipeline.merge.patchers;

import com.hymnsmobile.pipeline.merge.dagger.Merge;
import com.hymnsmobile.pipeline.merge.dagger.MergeScope;
import com.hymnsmobile.pipeline.models.PipelineError;
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
    fix_h79_I68_J68_K68();
    fix_I773();
    fix_I269_h367();
    fix_I485_h666();
    fix_I664_h921();
    fix_I643_J643_h1017();
    fix_I1401_h1191();
    fix_K1014_h1248();
    fix_I1832_ts823();
    fix_I42_ch42();
    fix_I1531_h1348();
    fix_K460_h605();
    fix_J539_h734();
    fix_K57_h51();
    fix_K319_h419();
    fix_I709_h1028();
    fix_K667_h894();
    fix_K372_h494();
  }

  /**
   * This is mostly fixed by {@link HymnalNetPatcher#fix_h79_h8079}. However, H4a adds I/68, K/68,
   * and J/68, so we need to go through and fix those as well,
   * </p>
   * I/68, K/68, and J/68 are the Indonesian, Korean, and Japanese translations of ch/68
   * respectively. ch/68, however, is not actually related (language-wise) to h/79, meaning that
   * all the links to h/79 are wrong. Therefore, we need to remove the wrong links to h/79 and add
   * the correct links to ch/68 where needed.
   * </p>
   *  Here is the current mapping:
   *    h/79->cb/79,hs/44,pt/79,I/68,K/68,J68;
   *    I/68->h/79,ch/68
   *    K/68->h/79
   *    J/68->h/79
   * </p>
   *  Here is the correct mapping:
   *    h/79->cb/79,hs/44,pt/79;
   *    I/68->ch/68
   *    K/68->ch/68
   *    J/68->ch/68
   */
  private void fix_h79_I68_J68_K68() {
    removeLanguages("h/79", "I/68", "J/68", "K/68");
    removeLanguages("I/68", "h/79");
    removeLanguages("K/68", "h/79");
    removeLanguages("J/68", "h/79");

    addLanguages("K/68", "ch/68");
    addLanguages("J/68", "ch/68");
  }

  /**
   * The parent of I/773 in H4a database is bf/69, but it's a duplicate of h/8773, so we need to
   * change it to h/8773.
   */
  private void fix_I773() {
    removeLanguages("I/773", "bf/69");
    addLanguages("I/773", "h/8773");
  }

  /**
   * I/269's parent hymn should be ch/269, not ch/289 and h/367.
   * <p/>
   * Similarly, h/367's Indonesian translation is I/289, but it also includes I/269.
   */
  private void fix_I269_h367() {
    removeLanguages("I/269", false, "ch/289", "h/367");
    addLanguages("I/269", "ch/269");

    removeLanguages("h/367", "I/269");
  }

  /**
   * I/485 has h/666 as a mapping, but that is a mistake. i/485 has no English translation yet.
   */
  private void fix_I485_h666() {
    removeLanguages("I/485", "h/666");
    removeLanguages("h/666", false, "I/485");
  }

  /**
   * h/1358 is a 1-verse song called "Rise! Preach the Gospel now!". h/921 is a 4-verse song called
   * "Rescue the perishing".
   * </p>
   * These are two different songs with the same tune. Therefore, they should be separate in terms
   * of language translations. However, they are confounded by i/664, which references both h/921
   * and ch/664. We need to remove h/921.
   */
  private void fix_I664_h921() {
    removeLanguages("I/664", "h/921");
    removeLanguages("h/921", "I/664");
  }

  /**
   * Based on {@link HymnalNetPatcher#fix_ch643()}, ch/643 should have no references to non-Chinese
   * songs. H4a adds I/643 as a legitimate Indonesian translation of ch/643. However, H4a also
   * re-adds h/1017 back into the mix, which needs to be taken out again.
   * <p/>
   * A similar thing applies to J/643. It should only map to ch/643, and not h/1017 for the same
   * reason. However, it only maps to h/1017, so we need to remove it and add ch/643 to its
   * mapping.
   */
  private void fix_I643_J643_h1017() {
    removeLanguages("I/643", "h/1017");

    removeLanguages("J/643", "h/1017");
    addLanguages("J/643", "ch/643");

    removeLanguages("h/1017", "I/643", "J/643");
  }

  /**
   * I/1401 references ts/401 and h/1191. That's not right, because h/1191's Chinese song is ts/410,
   * not ts/401. So we need to remove h/1191 as a mapping of I/1401.
   */
  private void fix_I1401_h1191() {
    removeLanguages("I/1401", "h/1191");
    removeLanguages("h/1191", "I/1401");
  }

  /**
   * K/1014 matches to h/1248, but it should match to h/1295 instead
   */
  private void fix_K1014_h1248() {
    removeLanguages("K/1014", "h/1248");
    removeLanguages("h/1248", "K/1014");
    addLanguages("K/1014", "h/1295");
  }

  /**
   * I/1832 is mapped to ts/823, but it should be mapped to ts/832 instead. Most likely a typo.
   */
  private void fix_I1832_ts823() {
    removeLanguages("I/1832", false,"ts/823");
    removeLanguages("ts/823", "I/1832");

    addLanguages("I/1832", "ts/832");
  }

  /**
   * I/42's parent should be ch/42, not ch/43. Mostly likely a typo in H4a.
   */
  private void fix_I42_ch42() {
    removeLanguages("I/42", false, "ch/43");
    removeLanguages("ch/43", "I/42");

    addLanguages("I/42", "ch/42");
  }

  /**
   * I/1531's should be linked ts/531, not ts/513 and h/1348. Mostly likely a typo in H4a.
   */
  private void fix_I1531_h1348() {
    removeLanguages("I/1531", false, "ts/513", "h/1348");
    removeLanguages("h/1348", "I/1531");

    addLanguages("I/1531", "ts/531");
  }

  /**
   * K/460 shouldn't map to h/605, but should instead map to ch/460, which is the Chinese version of
   * h/623.
   */
  private void fix_K460_h605() {
    removeLanguages("K/460", "h/605");
    removeLanguages("h/605", "K/460");

    addLanguages("K/460", "ch/460");
  }

  /**
   * J/539 shouldn't map to h/734, but should instead map to ch/539, which is the Chinese version of
   * h/743.
   */
  private void fix_J539_h734() {
    removeLanguages("J/539", "h/734");
    removeLanguages("h/734", "J/539");

    addLanguages("J/539", "ch/539");
  }

  /**
   * K/57 shouldn't map to h/51, but should instead map to ch/57, which is the Chinese version of
   * h/61.
   */
  private void fix_K57_h51() {
    removeLanguages("K/57", "h/51");
    removeLanguages("h/51", "K/57");

    addLanguages("K/57", "ch/57");
  }

  /**
   * K/319 shouldn't map to h/419, but should instead map to ch/319.
   */
  private void fix_K319_h419() {
    removeLanguages("K/319", "h/419");
    removeLanguages("h/419", "K/319");

    addLanguages("K/319", "ch/319");
  }

  /**
   * I/709's should be linked ch/709, not ch/708 and h/1028. Mostly likely a typo in H4a.
   */
  private void fix_I709_h1028() {
    removeLanguages("I/709", false, "h/1028", "ch/708");
    removeLanguages("h/1028", "I/709");

    addLanguages("I/709", "ch/709");
  }

  /**
   * K/667 shouldn't map to h/894, but should instead map to ch/667, which is the Chinese version of
   * h/1349.
   */
  private void fix_K667_h894() {
    removeLanguages("K/667", "h/894");
    removeLanguages("h/894", "K/667");

    addLanguages("K/667", "ch/667");
  }

  /**
   * K/372 shouldn't map to h/494, but should instead map to ch/372, which is the Chinese version of
   * h/495.
   */
  private void fix_K372_h494() {
    removeLanguages("K/372", "h/494");
    removeLanguages("h/494", "K/372");

    addLanguages("K/372", "ch/372");
  }
}
