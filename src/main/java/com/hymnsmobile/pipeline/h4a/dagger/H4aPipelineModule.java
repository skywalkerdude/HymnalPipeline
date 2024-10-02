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

      // Unparseable songs that show up in the "related" column. They technically are not "non-existent" because they
      // are completely unparseable, so there's no way to check if they exist or not.
      add("IloveYou");
      add("OJesusLord");
      add("I\'malwayscallingonYou.");
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
      add("CS352"); // exists multiple times
      add("CS158"); // exists multiple times
      add("CS158"); // exists multiple times
      add("CB1360");
      add("C506c");
      add("CB381"); // exists multiple times
      add("CB381"); // exists multiple times
      add("C481c");
      add("CS9117");
      add("CS46");
      add("CS400");
    }};
  }
}
