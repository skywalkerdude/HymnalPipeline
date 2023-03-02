package com.hymnsmobile.pipeline.testutil;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineTestModule;
import com.hymnsmobile.pipeline.hymnalnet.models.HymnalNetKey;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension to determine which hymns to fetch.
 */
public class FetchHymnsExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    String[] keysToFetch = getKeysToFetch(context);
    if (keysToFetch == null) {
      return;
    }

    HymnalNetPipelineTestModule.SONGS_TO_FETCH.addAll(
        ImmutableList.copyOf(keysToFetch).stream().map(keyToFetch -> {
          String[] parts = keyToFetch.split("/");
          if (parts.length < 2 || parts.length > 3) {
            throw new RuntimeException(String.format("%s is an invalid key to fetch", keyToFetch));
          }

          HymnalNetKey.Builder key = HymnalNetKey.newBuilder().setHymnType(parts[0])
              .setHymnNumber(parts[1]);
          if (parts.length == 3) {
            key.setQueryParams(parts[2]);
          }
          return key.build();
        }).collect(toImmutableList()));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    HymnalNetPipelineTestModule.SONGS_TO_FETCH.clear();
  }

  /**
   * Retrieves the names of fixture files for test
   */
  private String[] getKeysToFetch(ExtensionContext context) {
    // Method-level fixture annotations have precedence over class-level fixture annotations
    FetchHymns methodAnnotation = context.getRequiredTestMethod().getAnnotation(FetchHymns.class);
    if (methodAnnotation != null) {
      return methodAnnotation.keysToFetch();
    }

    // if a test has no annotation, then return null
    return null;
  }


}
