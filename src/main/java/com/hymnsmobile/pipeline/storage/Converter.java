package com.hymnsmobile.pipeline.storage;

import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.storage.dagger.StorageScope;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

@StorageScope
public class Converter {

  private static final Map<HymnType, String> HYMN_TYPE_TO_STRING_VALUE_MAP = new HashMap<>() {{
    put(HymnType.CLASSIC_HYMN, "h");
    put(HymnType.NEW_TUNE, "nt");
    put(HymnType.NEW_SONG, "ns");
    put(HymnType.CHILDREN_SONG, "c");
    put(HymnType.BE_FILLED, "bf");
    put(HymnType.HOWARD_HIGASHI, "lb");
    put(HymnType.DUTCH, "hd");
    put(HymnType.GERMAN, "de");
    put(HymnType.CHINESE, "ch");
    put(HymnType.CHINESE_SIMPLIFIED, "chx");
    put(HymnType.CHINESE_SUPPLEMENTAL, "ts");
    put(HymnType.CHINESE_SUPPLEMENTAL_SIMPLIFIED, "tsx");
    put(HymnType.CEBUANO, "cb");
    put(HymnType.TAGALOG, "ht");
    put(HymnType.FRENCH, "hf");
    put(HymnType.SPANISH, "hs");
    put(HymnType.KOREAN, "k");
    put(HymnType.INDONESIAN, "i");
    put(HymnType.JAPANESE, "j");
    put(HymnType.FARSI, "f");
    put(HymnType.LIEDERBUCH, "lde");
  }};

  @Inject
  public Converter() {
  }

  public String serialize(HymnType hymnType) {
    return Optional.ofNullable(HYMN_TYPE_TO_STRING_VALUE_MAP.get(hymnType)).orElseThrow();
  }
}
