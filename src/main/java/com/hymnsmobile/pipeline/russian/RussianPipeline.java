package com.hymnsmobile.pipeline.russian;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.dagger.DaggerPipelineComponent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * Russian songs from
 * <a href="https://play.google.com/store/apps/details?id=kbk.hymnals&gl=US">Гимны (1-800)</a>
 * </p>
 * <a href="https://screenshot.googleplex.com/Adq7Spz9SmhguJK">Screenshot of Play Store
 * listing</a>.
 * </p>
 * Description of app:
 * </p>
 * The application is an electronic version of the official hymnal, published "Collector biblical
 * books" in 2014. It contains text and melodies 800 Christian hymns with the thematic content,
 * index, and search function. Search by number in English Hymnal included in the settings.
 * </p>
 * A fragment of the preface:
 * </p>
 * "The Church exists on earth for nearly twenty centuries. For a long period of time in all the
 * centuries there have been many good hymns and songs that were written by many saints, has a
 * certain spiritual experiences and knowledge. Following in their footsteps, we received a blessing
 * - to enjoy all their writings as a rich heritage. Therefore, in this collection we have tried as
 * far as possible, collect all their works to the church today use and enjoy all the richness of
 * God's house. We are grateful for all the work as their authors, and our Father - Tom who gives
 * all gifts ... "
 * </p>
 * Read more about the application, visit <a href="http://kbk.ru/site/hymns/">Russian LSM</a>.
 */
public class RussianPipeline {

  private static final Logger LOGGER = Logger.getGlobal();
  private static final String FILE_PATH = "storage/russian/russian_protos.textproto";

  private final Set<RussianHymn> hymns;

  @Inject
  public RussianPipeline(Set<RussianHymn> hymns) {
    this.hymns = hymns;
  }

  public void run() throws IOException {
    LOGGER.fine("Russian pipeline starting");
    this.hymns.addAll(RussianHymns.parseFrom(new FileInputStream(FILE_PATH)).getRussianHymnsList());
    LOGGER.fine("Russian pipeline finished");
  }

  public ImmutableList<RussianHymn> getRussianHymns() {
    return ImmutableList.copyOf(hymns);
  }

  public static void main(String[] args)
      throws IOException {
    DaggerPipelineComponent.create().russianPipeline().build().pipeline().run();
  }
}
