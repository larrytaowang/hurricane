package com.hurricane.hurricane.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;


/**
 * @author larrytaowang
 * A Class that maintains Http-Header-Case for all keys. Supports multiple values per key.
 */
public class HttpHeaders implements Iterable<Map.Entry<String, List<String>>> {
  private Map<String, List<String>> headers;

  public HttpHeaders() {
    this.headers = new HashMap<>();
  }

  /**
   * Add a new value for the given key
   * @param name name of a key in Http header
   * @param value One value of the key
   */
  public void add(String name, String value) {
    var normalizedName = HttpHeaders.normalizeName(name);
    if (!headers.containsKey(normalizedName)) {
      headers.put(normalizedName, new ArrayList<>());
    }

    headers.get(normalizedName).add(value);
  }

  /**
   * Returns all values for the given header as a list
   * @param name name of the header
   * @return values of the given header as list. Return empty List if header does not exist.
   */
  public String getValues(String name) {
    var normalizedName = HttpHeaders.normalizeName(name);
    var valueList = headers.getOrDefault(normalizedName, new ArrayList<>());
    return String.join(", ", valueList);
  }

  /**
   * Update the dictionary with a single header line.
   * @param line A line contains Key-Value of header, separated by a colon
   */
  public void parseLine(String line) {
    var keyValue = line.split(":");
    if (keyValue.length != 2) {
      throw new IllegalArgumentException("Illegal header line, the format should be Key:Value, the input is = " + line);
    }

    add(normalizeName(keyValue[0]), keyValue[1].strip());
  }

  /**
   * Construct a HttpHeader from header text.
   * @param headersLine Http Header text
   * @return HttpHeader constructed from text
   */
  public static HttpHeaders parse(String headersLine) {
    HttpHeaders headers = new HttpHeaders();
    var headerLineList = Arrays.asList(headersLine.split("\\r\\n"));
    headerLineList = headerLineList.stream().filter((x) -> !x.strip().isEmpty()).collect(Collectors.toList());
    for (var headerLine : headerLineList) {
      headers.parseLine(headerLine);
    }

    return headers;
  }

  /**
   * Convert a name to Http-Header-Case
   * @param name name of http header
   * @return name of http header in Http-Header-Case
   */
  protected static String normalizeName(String name) {
    var splitWords = Arrays.asList(name.split("-"));
    return splitWords.stream().map(HttpHeaders::capitalize).collect(Collectors.joining("-"));
  }

  /**
   * Capitalize a string. The first character is Upper case, the rest of the string is lower case.
   * @param name the name we want to capitalize
   * @return Capitalized string
   */
  protected static String capitalize(String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }

    var result = name.substring(0, 1).toUpperCase();
    if (name.length() > 1) {
      result += name.substring(1).toLowerCase();
    }

    return result;
  }

  /**
   * Check if key is present in this header
   * @param key header key
   * @return if header key is present
   */
  public boolean contains(String key) {
    return headers.containsKey(key);
  }

  @NotNull
  @Override
  public Iterator<Map.Entry<String, List<String>>> iterator() {
    return headers.entrySet().iterator();
  }

  @Override
  public String toString() {
    return "HttpHeaders{" + "headers=" + headers + '}';
  }
}
