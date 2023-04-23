package com.hymnsmobile.pipeline.liederbuch;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.hymnsmobile.pipeline.liederbuch.BlockList.BLOCK_LIST;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.liederbuch.dagger.Liederbuch;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchHymn;
import com.hymnsmobile.pipeline.liederbuch.models.LiederbuchKey;
import com.hymnsmobile.pipeline.liederbuch.models.Verse;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.utils.TextUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.viewer.Viewer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class LiederbuchPipeline {

  private static final Logger LOGGER = Logger.getGlobal();
  private static final String EPUB_PATH = "storage/liederbuch/liederbuch_v0.1.1.epub";

  private final Converter converter;
  private final Set<LiederbuchHymn> songs;
  private final Set<PipelineError> errors;
  private final EpubReader reader;

  @Inject
  public LiederbuchPipeline(
      Converter converter,
      EpubReader reader,
      Set<LiederbuchHymn> songs,
      @Liederbuch Set<PipelineError> errors) {
    this.converter = converter;
    this.errors = errors;
    this.songs = songs;
    this.reader = reader;
  }

  public ImmutableList<LiederbuchHymn> getLiederbuchSong() {
    return ImmutableList.copyOf(songs);
  }

  public ImmutableList<PipelineError> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void run() throws IOException {
    LOGGER.info("Liederbuch pipeline starting");
    Book book = reader.readEpub(new FileInputStream(EPUB_PATH));
    ImmutableList<Resource> allResources = book.getResources().getAll().stream()
        .sorted(Comparator.comparing(Resource::getHref))
        .collect(toImmutableList());

    for (int i = 0; i < allResources.size(); i++) {
      Resource resource = allResources.get(i);

      String data = new String(resource.getData()); // Returns content as html.
      Document document = Jsoup.parse(data);

      ImmutableList<String> allSongs = document.getElementsByClass("calibre4").stream()
          .flatMap(element -> element.children().stream().filter(e -> e.hasAttr("href")))
          .map(Element::text)
          .collect(toImmutableList());

      for (String songId : allSongs) {
        LiederbuchKey key = converter.toKey(songId).orElseThrow();
        if (BLOCK_LIST.contains(key)) {
          LOGGER.fine(String.format("%s contained in block list. Skipping...", key));
          continue;
        }

        Element headerElement = document.getElementById(songId);
        if (headerElement == null) {
          // End of document. Go to the next one.
          resource = allResources.get(++i);
          data = new String(resource.getData()); // Returns content as html.
          document = Jsoup.parse(data);
          headerElement = document.getElementById(songId);
        }

        LiederbuchHymn.Builder liederbuchSong = LiederbuchHymn.newBuilder().setKey(key);
        assert headerElement != null;
        // Remove the "#CS1" link that is part of the title, so we can get the actual title out.
        headerElement.select("a[href]").remove();

        liederbuchSong.setTitle(headerElement.text());

        // Info about the song, such as related songs and meter
        Element infoElement = headerElement.nextElementSibling();
        assert infoElement != null && infoElement.hasClass("calibre 7");
        liederbuchSong.addAllRelated(infoElement.select("a[href]").stream()
            .peek(Node::remove)
            .map(Element::text)
            .map(s -> converter.toKey(s).orElseThrow())
            .collect(toImmutableList()));
        liederbuchSong.setMeter(infoElement.text().replace(",", "").trim());

        Element nextElement = infoElement.nextElementSibling();
        // if nextElement is null, then that indicates the end of the file
        // if the nextElement's class is calibre5, indicates that it's onto the next song
        while (nextElement != null && !nextElement.hasClass("calibre5")) {
          Verse.Builder verse = Verse.newBuilder();
          nextElement.textNodes().stream()
              .map(TextNode::text)
              // Filter out the verse numbers, because the verse ordering inherently already
              // contains the verse numbers.
              .filter(text -> !TextUtil.isNumeric(text))
              .forEach(verse::addVerseContent);
          liederbuchSong.addVerses(verse);

          nextElement = nextElement.nextElementSibling();
        }
        songs.add(liederbuchSong.build());
      }
    }
    LOGGER.info("Liederbuch pipeline finished");
  }

  public static void main(String[] args)
      throws IOException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    try (FileInputStream file = new FileInputStream(EPUB_PATH)) {
      // Schedule a job for the event dispatch thread:
      // creating and showing this application's GUI.
      javax.swing.SwingUtilities.invokeLater(() -> new Viewer(file));
    }
  }
}
