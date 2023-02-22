package com.hymnsmobile.hymnalnet;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.hymnsmobile.hymnalnet.models.Hymn;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import javax.inject.Inject;

public class HymnalNetFetcher {

  private static final String HYMNAL_NET_API_FORMAT = "https://hymnalnetapi.herokuapp.com/v2/hymn/%s/%s?check_exists=true";

  private final HttpClient client;

  @Inject
  public HymnalNetFetcher(HttpClient client) {
    this.client = client;
  }

  public Hymn fetchHymn(HymnalDbKey key) throws IOException, InterruptedException {
    HymnType hymnType = key.hymnType;
    String hymnNumber = key.hymnNumber;

    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
            .uri(URI.create(String.format(HYMNAL_NET_API_FORMAT, hymnType, hymnNumber)))
            .build(),
        BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException(
          String.format("Hymn %s/%s returned with status code: %d", hymnType, hymnNumber,
              response.statusCode()));
    }

    Hymn.Builder builder = Hymn.newBuilder();
    try {
      JsonFormat.parser().merge(response.body(), builder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(
          String.format("Unable to parse %s/%s: %s", hymnType, hymnNumber, response.body()), e);
    }
    return builder.build();
  }
}
