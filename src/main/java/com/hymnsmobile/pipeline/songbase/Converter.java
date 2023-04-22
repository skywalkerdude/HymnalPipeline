package com.hymnsmobile.pipeline.songbase;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.dagger.Songbase;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineScope;
import com.hymnsmobile.pipeline.songbase.models.BookResponse;
import com.hymnsmobile.pipeline.songbase.models.ReferenceResponse;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import com.hymnsmobile.pipeline.songbase.models.SongbaseKey;
import com.hymnsmobile.pipeline.songbase.models.SongbaseResponse;
import java.util.Set;
import javax.inject.Inject;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

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

    ImmutableMap<Integer, BookResponse> booksById = songbaseResponse.getBooksList().stream()
        .collect(toImmutableMap(BookResponse::getId, book -> book));

    ArrayListValuedHashMap<Integer, SongbaseKey> songIdToKeys = new ArrayListValuedHashMap<>();
    for (ReferenceResponse reference : songbaseResponse.getReferencesList()) {
      BookResponse book = booksById.get(reference.getBookId());
      assert book != null;
      SongbaseKey key = SongbaseKey.newBuilder().setHymnType(book.getSlug())
          .setHymnNumber(reference.getIndex()).build();
      songIdToKeys.put(reference.getSongId(), key);
    }

    return songbaseResponse.getSongsList().stream().map(song -> {
      SongbaseHymn.Builder builder = SongbaseHymn.newBuilder();
      if (songIdToKeys.containsKey(song.getId())) {
        builder.addAllKey(songIdToKeys.get(song.getId()));
      } else {
        builder.addKey(SongbaseKey.newBuilder()
            .setHymnType(HymnType.SONGBASE.codeName)
            .setHymnNumber(String.valueOf(song.getId()))
            .build());
      }
      return builder
          .setTitle(song.getTitle())
          .setLanguage(song.getLang())
          .setLyrics(song.getLyrics())
          .build();
    }).collect(toImmutableList());
  }
}
