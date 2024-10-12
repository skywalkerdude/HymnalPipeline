package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Module
interface H4aPipelineModule {

  @Provides
  @H4aPipelineScope
  static Set<H4aHymn> h4aHymns() {
    return new HashSet<>();
  }

  @H4a
  @Provides
  @H4aPipelineScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }

  /**
   * Songs that are blocked for miscellaneous reasons.
   */
  @MiscBlockList
  @Provides
  @H4aPipelineScope
  static List<String> miscBlockList() {
    return new ArrayList<>() {{
      // Song "exists" in the H4a db (as well as on Hymnal.net) but is a gibberish song.
      add("NS582");

      // Song "exists" in the H4a db (as well as on Hymnal.net) but is a gibberish song.
      add("NS881");

      // G10001 is a duplicate of "God's eternal economy" (NS180) and thus can just be ignored.
      add("G10001");

      // G10002 is a different translation of "What miracle! What mystery!" (NS151), but the
      // translation we want is already covered by G420, so we can skip this song.
      add("G10002");

      // Mistyped Spanish songs that don't exist.
      add("ES140");
      add("ES163");
      add("ES164");
      add("ES261");
      add("ES221");
      add("ES300");
      add("ES421");
      add("ES422");
      add("ES437");
      add("ES500");
    }};
  }

  /**
   * Songs that show up in "related" column but don't actually exist in the h4a db. These should be ignored since they
   * map to nothing.
   */
  @NonExistentRelatedSongs
  @Provides
  @H4aPipelineScope
  static List<String> nonExistentRelatedSongs() {
    return new ArrayList<>() {{
      add("C825");
      add("C914");
      add("C912");
      add("C834");
      add("T898");
      add("C815");
      add("C806");
      add("C905");
      add("BF1040");
      add("C856");
      add("C812");
      add("C810");
      add("C850");
      add("C901");
      add("C517c");
      add("C510c");
      add("C513c");
      add("CB57");
      add("C925");
      add("C917");
      add("C840");
      add("CS352"); // exists multiple times
      add("CS352"); // exists multiple times
      add("CS158");
      add("CB1360");
      add("C506c");
      add("CB381");
      add("C481c");
      add("CS9117");
      add("CS46");
      add("CS400");
    }};
  }

  /**
   * Songs that require special one-off attention.
   */
  @OneOff
  @Provides
  @H4aPipelineScope
  static List<String> oneOffCases() {
    return new ArrayList<>() {{
      // Super special case where BF243 needs to remove CS134 as its parent hymn but keep it as a related song. This is
      // because, since CS134 exists already, BF243 ends up being merged into it, even though it is the English
      // translation of CS134 (hence, keeping it as a related song).
      add("BF243");
    }};
  }
}
