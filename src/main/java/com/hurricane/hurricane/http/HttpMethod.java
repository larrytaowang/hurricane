package com.hurricane.hurricane.http;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;


/**
 * @author larrytaowang
 */

public enum HttpMethod {

  /**
   * HTTP request HEAD method
   */
  HEAD,

  /**
   * HTTP request GET method
   */
  GET,
  /**
   * HTTP request POST method
   */
  POST,
  /**
   * HTTP request DELETE method
   */
  DELETE,
  /**
   * HTTP request PUT method
   */
  PUT;

  private static final Map<String, HttpMethod> stringToEnum =
      Stream.of(values()).collect(toMap(Object::toString, e -> e));

  public static Optional<HttpMethod> fromString(String method) {
    return Optional.ofNullable(stringToEnum.get(method));
  }
}
