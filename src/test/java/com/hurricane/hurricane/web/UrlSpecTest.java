package com.hurricane.hurricane.web;

import org.junit.Assert;
import org.junit.Test;


public class UrlSpecTest {

  @Test
  public void reverseBasic() {
    var urlSpec = new UrlSpec("/([0-9]{4})/([a-z-]+)/", null);
    Assert.assertEquals("/1234/abc/", urlSpec.getUrlPath("1234", "abc"));
  }

  @Test
  public void reverseWithNoArgs() {
    var urlSpec = new UrlSpec("/123/456/", null);
    Assert.assertEquals("/123/456/", urlSpec.getUrlPath());
  }


  @Test
  public void reverseWithNestedParenthesis() {
    var urlSpec = new UrlSpec("/([0-9]{4}([a-z-]+))/", null);
    Assert.assertNull(urlSpec.getFormattedPath());
  }

  @Test(expected = IllegalArgumentException.class)
  public void reverseWithWrongArgsNumber() {
    var urlSpec = new UrlSpec("/([0-9]{4})/([a-z-]+)/", null);
    Assert.assertEquals("/1234/abc/", urlSpec.getUrlPath("1234"));
  }

}