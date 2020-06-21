package com.hurricane.hurricane.web;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 * Specifies mappings between URLs and handlers.
 */
public class UrlSpec {
  private final static Logger logger = Logger.getLogger(UrlSpec.class);

  /**
   * Regular expression to be matched. Any groups in the regex will be passed in to the handler's get/post/etc methods
   * as arguments
   */
  private Pattern pattern;

  /**
   * Handler of this URL spec
   */
  private RequestHandler handler;

  /**
   * A dictionary of additional arguments to be passed to the handler's constructor
   */
  private Map<String, String> kwargs;

  /**
   * Name of this handler.
   */
  private String name;

  /**
   * URL path with placeholder
   */
  private String formattedPath;

  /**
   * Count of groups in the URL pattern
   */
  private int groupCount;

  public UrlSpec(String pattern, RequestHandler handler, Map<String, String> kwargs, String name) {
    this.pattern = Pattern.compile(pattern);
    this.handler = handler;
    this.kwargs = kwargs;
    this.name = name;
    this.groupCount = -1;
    createFormattedUrlPath();
  }

  public UrlSpec(String pattern, RequestHandler handler) {
    this(pattern, handler, new HashMap<>(), "");
  }

  /**
   * Given the pattern of URL path, create the formatted path of URL, that is, replace the "()" with placeholder "%s".
   * For example, given the pattern "/([0-9]{4})/([a-z-]+)/", which contains patterns for two groups, we can generate
   * the formatted string for URL path: "/%s/%s/". The getUrlPath() function can provide the real arguments to get
   * the real URL path, e.g., "/1234/abc".
   *
   * Note that nested parenthesis are not supported.
   */
  private void createFormattedUrlPath() {
    var placeHolder = "%s";
    var stringBuilder = new StringBuilder();
    var warnLog = "Found nested parenthesis. This pattern is too complicated for our simplistic matching, "
        + "so we can't support reversing it";

    var parenthesisStack = new ArrayDeque<Character>();
    var groupCount = 0;
    for (var character : pattern.toString().toCharArray()) {
      if (character == '(') {
        if (!parenthesisStack.isEmpty() && parenthesisStack.peek() == '(') {
          logger.warn(warnLog);
          return;
        } else {
          parenthesisStack.push(character);
        }
      } else if (character == ')') {
        if (parenthesisStack.isEmpty() || parenthesisStack.peek() != '(') {
          logger.warn(warnLog);
          return;
        } else {
          parenthesisStack.pop();
          stringBuilder.append(placeHolder);
          groupCount += 1;
        }
      } else {
        if (parenthesisStack.isEmpty()) {
          stringBuilder.append(character);
        }
      }
    }

    this.formattedPath = stringBuilder.toString();
    this.groupCount = groupCount;
  }

  /**
   * Get the real URL path of this spec
   * @param args argus to construct the URL path, if regex pattern is used
   * @return real URL path
   */
  public String getUrlPath(Object... args) {
    if (formattedPath == null || groupCount == -1) {
      throw new IllegalArgumentException("Cannot resolve URL regex = " + pattern.toString());
    }

    if (args.length != groupCount) {
      throw new IllegalArgumentException(
          "Required number of arguments = " + groupCount + ", actually get count = " + args.length);
    }

    if (args.length == 0) {
      return formattedPath;
    } else {
      return String.format(formattedPath, args);
    }
  }

  public String getFormattedPath() {
    return formattedPath;
  }
}
