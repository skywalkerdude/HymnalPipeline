package com.hymnsmobile.pipeline.common;

import java.net.http.HttpClient;
import javax.inject.Inject;

public class HttpNetworkClient {

  private final HttpClient client;

  @Inject
  public HttpNetworkClient(HttpClient client) {
    this.client = client;
  }



}
