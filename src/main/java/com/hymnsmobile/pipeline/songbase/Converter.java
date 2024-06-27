package com.hymnsmobile.pipeline.songbase;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import com.hymnsmobile.pipeline.songbase.models.SongResponse;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.songbase.models.SongbaseResponse;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;

@SongbasePipelineScope
public class Converter {

  @Inject
  public Converter() {
  }

  public ImmutableList<SongbaseHymn> convert(String responseBody)
      throws InvalidProtocolBufferException {
    SongbaseResponse.Builder responseBuilder = SongbaseResponse.newBuilder();
    JsonFormat.parser().merge(responseBody, responseBuilder);
    SongbaseResponse songbaseResponse = responseBuilder.build();

    Map<Integer, SongResponse> songResponseById = songbaseResponse.getSongsList().stream()
        .collect(Collectors.toMap(SongResponse::getId, song -> song));

    Map<Integer, SongbaseHymn.Builder> songbaseHymnById = songbaseResponse.getBooksList().stream()
        .flatMap(bookResponse -> {
          String hymnType = bookResponse.getSlug();
          return bookResponse.getSongsMap().entrySet().stream().map(
              bookSongMap -> {
                int songId = Integer.parseInt(bookSongMap.getKey());
                String songIndex = bookSongMap.getValue();
                SongResponse song = songResponseById.get(songId);
                if (song == null) {
                  throw new IllegalStateException("song was null");
                }
                return Pair.of(songId, SongbaseHymn.newBuilder()
                    .addKey(SongbaseKey.newBuilder().setHymnType(hymnType).setHymnNumber(songIndex))
                    .setTitle(song.getTitle())
                    .setLanguage(song.getLang())
                    .setLyrics(song.getLyrics()));
              });
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Find and add songs that are not in any particular book.
    Map<Integer, SongResponse> copy = new HashMap<>(songResponseById);
    songbaseHymnById.keySet().forEach(copy::remove);
    copy.forEach((songId, song) ->
        songbaseHymnById.put(
            songId,
            SongbaseHymn.newBuilder()
                .addKey(
                    SongbaseKey.newBuilder().setHymnType(HymnType.SONGBASE_OTHER.codeName)
                        .setHymnNumber(String.valueOf(songId)))
                .setTitle(song.getTitle())
                .setLanguage(song.getLang())
                .setLyrics(song.getLyrics())));

    // Populate the language links
    songbaseHymnById.forEach((key, songbaseHymn) -> {
      int songId = key;
      SongResponse song = songResponseById.get(songId);
      for (int languageLink : song.getLanguageLinksList()) {
        songbaseHymn.addAllRelated(songbaseHymnById.get(languageLink).getKeyList());
      }
    });
    return songbaseHymnById.values().stream().map(SongbaseHymn.Builder::build).collect(toImmutableList());
  }
}
