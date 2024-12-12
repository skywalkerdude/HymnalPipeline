package com.hymnsmobile.pipeline.utils;

import java.util.List;

public class TextUtil {

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
 }
