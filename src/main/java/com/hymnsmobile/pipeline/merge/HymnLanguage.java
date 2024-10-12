package com.hymnsmobile.pipeline.merge;

import java.util.Locale;

/**
 * Represents the language of a hymn according to
 * <a href="https://www.loc.gov/standards/iso639-2/php/code_list.php">ISO codes</a>.
 */
public enum HymnLanguage {
  ENGLISH("en"),
  DUTCH("nl"),
  GERMAN("de"),
  CHINESE_TRADITIONAL("zh_HANT"),
  CHINESE_SIMPLIFIED("zh_HANS"),
  CEBUANO("ceb"),
  TAGALOG("tl"),
  FRENCH("fr"),
  SPANISH("es"),
  KOREAN("ko"),
  JAPANESE("ja"),
  INDONESIAN("id"),
  FARSI("fa"),
  RUSSIAN("ru"),
  PORTUGUESE("pt"),
  HEBREW("he"),
  SLOVAK("sk"),
  ESTONIAN("et"),
  ARABIC("ar"),
  UNKNOWN("");

  /**
   * abbreviated value (eg: en, de, zh_HANT, etc)
   */
  public final String iso;

  HymnLanguage(String iso) {
    this.iso = iso;
  }
}
