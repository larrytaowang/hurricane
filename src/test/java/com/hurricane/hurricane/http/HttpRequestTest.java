package com.hurricane.hurricane.http;

import org.junit.Assert;
import org.junit.Test;


public class HttpRequestTest {

  @Test
  public void getQueryArgsAndPath() {
    var uri = "/tutorials/other/top-20-mysql-best-practices?k1=v1&k2=v2";
    var httpRequest = new HttpRequest(null, HttpMethod.GET, uri, "HTTP/1.1", null);

    Assert.assertEquals("/tutorials/other/top-20-mysql-best-practices", httpRequest.getPath());
    Assert.assertEquals("v1", httpRequest.getQueryArgs().get("k1"));
    Assert.assertEquals("v2", httpRequest.getQueryArgs().get("k2"));

    uri = "/tutorials/other/top-20-mysql-best-practices";
    httpRequest = new HttpRequest(null, HttpMethod.GET, uri, "HTTP/1.1", null);
    Assert.assertEquals("/tutorials/other/top-20-mysql-best-practices", httpRequest.getPath());
  }
}