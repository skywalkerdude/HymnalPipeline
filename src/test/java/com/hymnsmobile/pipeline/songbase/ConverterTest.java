package com.hymnsmobile.pipeline.songbase;

import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterTest {

  private Converter target;

  @BeforeEach
  void setUp() {
    this.target = new Converter();
  }

  @Test
  void convert__singleSong_inBlueSongbook__convertsSong() throws IOException {
    String singleSong = TestUtils.readText("src/test/resources/songbase/input/single_song.json");

    assertThat(target.convert(singleSong)).containsExactly(
        SongbaseHymn.newBuilder()
            .addKey(SongbaseKey.newBuilder().setHymnType(HymnType.BLUE_SONGBOOK.codeName).setHymnNumber("1").build())
            .setTitle("Father Abraham")
            .setLanguage("english")
            .setLyrics("Father [D]Abraham\nHas many [A7]sons\n....")
            .build());
  }

  @Test
  void convert__singleSong_noBook__convertsSongToOtherType() throws IOException {
    String singleSong = TestUtils.readText("src/test/resources/songbase/input/single_song_no_book.json");

    assertThat(target.convert(singleSong)).containsExactly(
        SongbaseHymn.newBuilder()
            .addKey(SongbaseKey.newBuilder().setHymnType(HymnType.SONGBASE_OTHER.codeName).setHymnNumber("1").build())
            .setTitle("Father Abraham")
            .setLanguage("english")
            .setLyrics("Father [D]Abraham\nHas many [A7]sons\n....")
            .build());
  }

  @Test
  void convert__containsLanguageLinks__convertsLanguageLinks() throws IOException {
    String singleSong = TestUtils.readText("src/test/resources/songbase/input/two_songs_with_language_links.json");

    assertThat(target.convert(singleSong)).containsExactlyInAnyOrder(
        SongbaseHymn.newBuilder()
            .addKey(SongbaseKey.newBuilder().setHymnType(HymnType.BLUE_SONGBOOK.codeName).setHymnNumber("1").build())
            .setTitle("Father Abraham")
            .setLanguage("english")
            .setLyrics("Father [D]Abraham\nHas many [A7]sons\n....")
            .addRelated(SongbaseKey.newBuilder().setHymnType(HymnType.SONGBASE_OTHER.codeName).setHymnNumber("2").build())
            .build(),
        SongbaseHymn.newBuilder()
            .addKey(SongbaseKey.newBuilder().setHymnType(HymnType.SONGBASE_OTHER.codeName).setHymnNumber("2").build())
            .setTitle("Son Isaac")
            .setLanguage("english")
            .setLyrics("Son [D]Isaac\nHad just one [A7]dad\n....")
            .build());
  }
}
