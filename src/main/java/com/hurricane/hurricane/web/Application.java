package com.hurricane.hurricane.web;

import java.util.List;
import java.util.Map;


/**
 * @author larrytaowang
 */
public class Application {
  public static final String DEFAULT_HOST = "";

  private List<UrlSpec> handlers;

  private Map<String, UrlSpec> namedHandlers;

  public Application(List<UrlSpec> handlers) {
    addHandlers(handlers);
  }

  private void addHandlers(List<UrlSpec> handlers) {

  }
}
