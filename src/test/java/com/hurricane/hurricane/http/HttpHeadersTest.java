package com.hurricane.hurricane.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;


public class HttpHeadersTest {

  @Test
  public void normalizeName() {
    Assert.assertEquals("Content-Type", HttpHeaders.normalizeName("coNtent-TYPE"));
  }

  @Test
  public void capitalize() {
    Assert.assertNull(HttpHeaders.capitalize(null));
    Assert.assertEquals("C", HttpHeaders.capitalize("c"));
    Assert.assertEquals("Ca", HttpHeaders.capitalize("cA"));
  }

  @Test
  public void addAndGetValues() {
    var headers = HttpHeaders.parse("Content-Type: text/html\r\n");
    headers.add("Set-Cookie", "A=B");
    headers.add("Set-Cookie", "C=D");
    Assert.assertEquals("A=B, C=D", headers.getValues("Set-Cookie"));
  }

  @Test
  public void parseLine() {
    var headers = new HttpHeaders();
    headers.parseLine("Content-Type: text/html");
    Assert.assertEquals("text/html", headers.getValues("Content-Type"));
  }

  @Test
  public void parse() {
    var headers = HttpHeaders.parse("Content-Type: text/html\r\nContent-Length: 42\r\n");
    Assert.assertEquals("text/html", headers.getValues("Content-Type"));
    Assert.assertEquals("42", headers.getValues("Content-Length"));
  }

  @Test
  public void iterator() {
    var headers = HttpHeaders.parse("Content-Type: text/html\r\nContent-Length: 42\r\n");
    var keySet = new HashSet<>(Arrays.asList("Content-Type", "Content-Length"));
    for (var header : headers) {
      keySet.remove(header.getKey());
    }

    Assert.assertTrue(keySet.isEmpty());
  }
}