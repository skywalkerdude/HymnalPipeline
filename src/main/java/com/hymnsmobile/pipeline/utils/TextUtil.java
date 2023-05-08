package com.hymnsmobile.pipeline.utils;

import static com.google.gson.stream.JsonToken.END_DOCUMENT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class TextUtil {

  public static String strMapToJson(Map<String, String> stringMap) {
    if (stringMap.isEmpty()) {
      return null;
    }
    return new Gson().toJson(stringMap);
  }

  public static <M extends Message> String toJson(List<M> messages) throws IOException {
    if (messages.isEmpty()) {
      return null;
    }

    JsonArray array = new JsonArray();

    for (M message : messages) {
      array.add(toJson(message));
    }
    return array.toString();
  }

  public static <M extends Message> JsonElement toJson(M message)
      throws InvalidProtocolBufferException {
    return JsonParser.parseString(JsonFormat.printer().preservingProtoFieldNames().printingEnumsAsInts().print(message));
  }

  public static String join(List<String> strings) {
    if (strings.isEmpty()) {
      return null;
    }
    return String.join(";", strings);
  }

  public static boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * Returns true if a and b are equal, including if they are both null.
   * <p>Note: In platform versions 1.1 and earlier, this method only worked well if
   * both the arguments were instances of String.</i>
   *
   * @param a first CharSequence to check
   * @param b second CharSequence to check
   * @return true if a and b are equal
   */
  public static boolean equals(CharSequence a, CharSequence b) {
    if (a == b) {
      return true;
    }
    int length;
    if (a != null && b != null && (length = a.length()) == b.length()) {
      if (a instanceof String && b instanceof String) {
        return a.equals(b);
      } else {
        for (int i = 0; i < length; i++) {
          if (a.charAt(i) != b.charAt(i)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  public static boolean isNumeric(String str) {
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  public static String escapeSingleQuotes(String str) {
    if (TextUtil.isEmpty(str)) {
      return str;
    }
    StringBuilder sb = new StringBuilder();
    for (char c : str.toCharArray()) {
      if (c == '\'') {
        sb.append("'");
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * @return true if str2 is longer than str1.
   */
  public static boolean isLonger(String str1, String str2) {
    if (TextUtil.isEmpty(str1) && TextUtil.isEmpty(str2)) {
      return false;
    }

    if (!TextUtil.isEmpty(str1) && TextUtil.isEmpty(str2)) {
      return false;
    }

    if (TextUtil.isEmpty(str1) && !TextUtil.isEmpty(str2)) {
      return true;
    }

    return str2.trim().length() > str1.trim().length();
  }

  public static String getLongerString(String str1, String str2) {
    if (TextUtil.isEmpty(str1) && TextUtil.isEmpty(str2)) {
      return null;
    }

    if (!TextUtil.isEmpty(str1) && TextUtil.isEmpty(str2)) {
      return str1.trim();
    }

    if (TextUtil.isEmpty(str1) && !TextUtil.isEmpty(str2)) {
      return str2.trim();
    }

    return str1.trim().length() >= str2.trim().length() ? str1.trim() : str2.trim();
  }

  public static boolean isJsonValid(final String json) throws IOException {
    if (isEmpty(json)) {
      return true;
    }
    return isJsonValid(new StringReader(json));
  }

  private static boolean isJsonValid(final Reader reader)
      throws IOException {
    return isJsonValid(new JsonReader(reader));
  }

  private static boolean isJsonValid(final JsonReader jsonReader)
      throws IOException {
    try {
      JsonToken token;
      loop:
      while ((token = jsonReader.peek()) != END_DOCUMENT && token != null) {
        switch (token) {
          case BEGIN_ARRAY:
            jsonReader.beginArray();
            break;
          case END_ARRAY:
            jsonReader.endArray();
            break;
          case BEGIN_OBJECT:
            jsonReader.beginObject();
            break;
          case END_OBJECT:
            jsonReader.endObject();
            break;
          case NAME:
            jsonReader.nextName();
            break;
          case STRING:
          case NUMBER:
          case BOOLEAN:
          case NULL:
            jsonReader.skipValue();
            break;
          case END_DOCUMENT:
            break loop;
          default:
            throw new AssertionError(token);
        }
      }
      return true;
    } catch (final MalformedJsonException ignored) {
      return false;
    }
  }
}