package com.hymnsmobile.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.inject.Inject;

public class HttpNetworkClient {

  private final HttpClient client;

  @Inject
  public HttpNetworkClient(HttpClient client) {
    this.client = client;
  }



}
