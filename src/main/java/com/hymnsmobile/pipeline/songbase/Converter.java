package com.hymnsmobile.pipeline.songbase;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.dagger.Songbase;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import com.hymnsmobile.pipeline.songbase.models.SongResponse;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.songbase.models.SongbaseResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

@SongbasePipelineScope
public class Converter {

  private final Set<PipelineError> errors;

  @Inject
  public Converter(@Songbase Set<PipelineError> errors) {
    this.errors = errors;
  }

  public ImmutableList<SongbaseHymn> convert(String responseBody)
      throws InvalidProtocolBufferException {
    SongbaseResponse.Builder responseBuilder = SongbaseResponse.newBuilder();
    JsonFormat.parser().merge(responseBody, responseBuilder);
    SongbaseResponse songbaseResponse = responseBuilder.build();

    Map<Integer, SongResponse> songsById = songbaseResponse.getSongsList().stream()
        .collect(Collectors.toMap(SongResponse::getId, song -> song));

    // Keep track of songs that we've seen, so we can go back through and add all the leftover songs
    // as well.
    Set<Integer> seen = new HashSet<>();

    List<SongbaseHymn> allSongs = songbaseResponse.getBooksList().stream()
        .flatMap(bookResponse -> {
          String hymnType = bookResponse.getSlug();
          return bookResponse.getSongsMap().entrySet().stream().map(
              bookSongMap -> {
                int songId = Integer.parseInt(bookSongMap.getKey());
                String songIndex = bookSongMap.getValue();
                seen.add(songId);
                SongResponse song = songsById.get(songId);
                assert song != null;
                return SongbaseHymn.newBuilder()
                    .addKey(
                        SongbaseKey.newBuilder().setHymnType(hymnType).setHymnNumber(songIndex))
                    .setTitle(song.getTitle())
                    .setLanguage(song.getLang())
                    .setLyrics(song.getLyrics())
                    .build();
              });
        }).collect(Collectors.toList());

    // Find and add songs that are not in any particular book.
    seen.forEach(songsById::remove);
    songsById.forEach((songId, song) -> allSongs.add(SongbaseHymn.newBuilder()
        .addKey(
            SongbaseKey.newBuilder().setHymnType(HymnType.SONGBASE_OTHER.codeName)
                .setHymnNumber(String.valueOf(songId)))
        .setTitle(song.getTitle())
        .setLanguage(song.getLang())
        .setLyrics(song.getLyrics())
        .build()));
    return ImmutableList.copyOf(allSongs);
  }
}
