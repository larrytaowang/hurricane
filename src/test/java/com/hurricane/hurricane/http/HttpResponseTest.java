package com.hurricane.hurricane.http;

import com.hurricane.hurricane.common.Constant;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;


public class HttpResponseTest {

  @Test
  public void getHeaders() {
    var headersString = "Content-Length: 1234\r\n";
    var headers = HttpHeaders.parse(headersString);
    var httpResponse = new HttpResponse(headers);

    var headerBytes = httpResponse.getHeadersBytes(Constant.HTTP_VERSION_1_1);
    var expectedString = "HTTP/1.1 200 OK\r\n" + "Content-Length: 1234\r\n\r\n";
    Assert.assertArrayEquals(expectedString.getBytes(StandardCharsets.UTF_8), headerBytes);
  }
}