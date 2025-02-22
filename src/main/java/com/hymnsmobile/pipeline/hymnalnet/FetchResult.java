package com.hymnsmobile.pipeline.hymnalnet;

import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetJson;

public abstract class FetchResult {

  /**
   * Already seen during current execution of the pipeline.
   */
  public static class AlreadySeen extends FetchResult {

    AlreadySeen() {
    }
  }

  /**
   * Result has been stored from a previous execution of the pipeline.
   */
  public static class AlreadyStored extends FetchResult {

    AlreadyStored() {
    }
  }

  public static class FetchSuccess extends FetchResult {

    public final HymnalNetJson response;

    FetchSuccess(HymnalNetJson response) {
      this.response = response;
    }
  }

  public static class FetchException extends FetchResult {

    public final Exception exception;

    FetchException(Exception exception) {
      this.exception = exception;
    }
  }

  public static class FetchFailure extends FetchResult {

    public final int responseCode;
    public final String responseBody;

    FetchFailure(int responseCode, String responseBody) {
      this.responseCode = responseCode;
      this.responseBody = responseBody;
    }
  }

  public static class FetchNotFound extends FetchResult {

    FetchNotFound() {
    }
  }
}
