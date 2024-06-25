package com.hymnsmobile.pipeline.testutil;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;

public class TestUtils {

  public static <M extends Message> M readTextProto(String filePath, M.Builder builder) throws IOException {
    TextFormat.getParser().merge(readText(filePath), builder);
    //noinspection unchecked
    return (M) builder.build();
  }

  public static String readText(String filePath) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(new FileInputStream(filePath), writer);
    return writer.toString();
  }
}
