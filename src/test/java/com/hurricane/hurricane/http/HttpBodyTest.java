package com.hurricane.hurricane.http;

import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;


public class HttpBodyTest {

  @Test
  public void parseFormUrlEncodedBodyBasic() {
    var body = new HttpBody("field1=value1&field2=value2".getBytes(StandardCharsets.UTF_8));
    body.parseFormUrlEncodedBody();
    var arguments = body.getArguments();

    Assert.assertEquals(2, arguments.size());
    Assert.assertEquals("value1", arguments.get("field1"));
    Assert.assertEquals("value2", arguments.get("field2"));
  }

  @Test
  public void parseFormUrlEncodedBodyEncoding() {
    var body = new HttpBody("Name=John+Smith&Grade=19".getBytes(StandardCharsets.UTF_8));
    body.parseFormUrlEncodedBody();
    var arguments = body.getArguments();

    Assert.assertEquals("John Smith", arguments.get("Name"));
  }
}