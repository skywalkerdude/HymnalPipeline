package com.hymnsmobile.pipeline.liederbuch;

import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import nl.siegmann.epublib.viewer.Viewer;

public class LiederbuchLauncher {

  public static final String EPUB_PATH = "storage/liederbuch/liederbuch_v0.1.1.epub";

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
