package com.hurricane.hurricane.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;


/**
 * @author larrytaowang
 * This class hosts data of the HTTP body and the parsed arguments.
 */
public class HttpBody {

  /**
   * Body of the Http request
   */
  private byte[] data;

  /**
   * Key-Value tuples parsed from "application/x-www-form-urlencoded" request body
   */
  private Map<String, String> arguments;

  public HttpBody(@NotNull byte[] data) {
    this.data = data;
    this.arguments = new HashMap<>();
  }

  /**
   * Parse HTTP request body of type "application/x-www-form-urlencoded". The keys and values are encoded in key-value
   * tuples separated by '&', with a '=' between the key and the value. Non-alphanumeric characters in both keys and
   * values are percent encoded. Refer https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST for detail.
   */
  public void parseFormUrlEncodedBody() {
    var requestBody = new String(data, StandardCharsets.UTF_8);
    var argumentsLine = URLDecoder.decode(requestBody, StandardCharsets.UTF_8);

    var keyValueTupleDelimiter = "&";
    var keyValueDelimiter = "=";

    for (var argument : argumentsLine.split(keyValueTupleDelimiter)) {
      var keyValuePair = argument.split(keyValueDelimiter);
      if (keyValuePair.length != 2) {
        throw new IllegalArgumentException(
            "Malformed HTTP request body of type 'application/x-www-form-urlencoded'. Decoded body = " + argumentsLine);
      }

      var key = keyValuePair[0].strip();
      var value = keyValuePair[1].strip();
      arguments.put(key, value);
    }
  }

  public Map<String, String> getArguments() {
    return Collections.unmodifiableMap(arguments);
  }

  @Override
  public String toString() {
    return "HttpBody{" + "data=" + Arrays.toString(data);
  }
}
