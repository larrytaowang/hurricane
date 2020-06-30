package com.hurricane.hurricane.web;

import com.hurricane.hurricane.http.HttpConnection;
import com.hurricane.hurricane.http.HttpException;
import com.hurricane.hurricane.http.HttpMethod;
import com.hurricane.hurricane.http.HttpRequest;
import com.hurricane.hurricane.http.HttpResponse;
import com.hurricane.hurricane.http.HttpStatus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

import static com.hurricane.hurricane.common.Constant.*;


/**
 * @author larrytaowang
 */
public class RequestHandler {
  private final static Logger logger = Logger.getLogger(RequestHandler.class);

  /**
   * Http response of current Http request
   */
  private HttpResponse httpResponse;

  public RequestHandler() {
    this.httpResponse = new HttpResponse();
  }

  /**
   * Override this method to define workflow of HEAD method.
   *
   * @param request Http request this handler this going to process
   * @throws HttpException Some Http exceptions during processing
   */
  protected void handleHeadMethod(HttpRequest request) throws HttpException {
    throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Override this method to define workflow of HEAD method.
   *
   * @param request Http request this handler this going to process
   * @throws HttpException Some Http exceptions during processing
   */
  protected void handleGetMethod(HttpRequest request) throws HttpException {
    throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Override this method to define workflow of HEAD method.
   *
   * @param request Http request this handler this going to process
   * @throws HttpException Some Http exceptions during processing
   */
  protected void handlePostMethod(HttpRequest request) throws HttpException {
    throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Override this method to define workflow of HEAD method.
   *
   * @param request Http request this handler this going to process
   * @throws HttpException Some Http exceptions during processing
   */
  protected void handleDeleteMethod(HttpRequest request) throws HttpException {
    throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Override this method to define workflow of HEAD method.
   *
   * @param request Http request this handler this going to process
   * @throws HttpException Some Http exceptions during processing
   */
  protected void handlePutMethod(HttpRequest request) throws HttpException {
    throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Override this method to do setup work before handling Http request
   *
   * @param request http request to handle
   */
  protected void prepare(HttpRequest request) {
  }

  /**
   * Check if a method is supported in this handler
   *
   * @param method a HTTP handler
   * @return if a method is supported in this handler
   */
  public boolean isMethodSupported(HttpMethod method) {
    return getSupportedMethods().contains(method);
  }

  /**
   * Override this method if a subclass wants to support more methods than the standard GET/HEAD/POST.
   *
   * @return All the HTTP methods that are supported in this handler
   */
  protected Set<HttpMethod> getSupportedMethods() {
    var methods = Arrays.asList(HttpMethod.values());
    return new HashSet<>(methods);
  }

  /**
   * Run the callback after HTTP server finishes parsing HTTP request.
   *
   * @param connection TCP connection that this callback hosts
   * @param request    HTTP request of this callback
   */
  public void run(HttpConnection connection, HttpRequest request) {
    logger.info("Start to process the Http Request with handler");
    try {
      var requestMethod = request.getMethod();
      if (!isMethodSupported(requestMethod)) {
        throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
      }

      prepare(request);
      execute(request);
      finish(request);

    } catch (HttpException e) {
      logger.error("Http exception " + request.summary(), e);
//      sendError(connection, e.getStatus(), Optional.of(request));
    } catch (Exception e) {
      logger.error("Uncaught exception" + request.summary() + " when handling Http request = " + request.toString(), e);
//      sendError(connection, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(request));
    }
  }

  /**
   * Handle the Http request regarding proper method.
   *
   * @param request Http request to handle
   * @throws HttpException Some Http Exceptions during handling Http request
   */
  private void execute(HttpRequest request) throws HttpException {
    var method = request.getMethod();

    if (method.equals(HttpMethod.GET)) {
      logger.info("Handle request with GET method");
      handleGetMethod(request);
    } else if (method.equals(HttpMethod.HEAD)) {
      logger.info("Handle request with HEAD method");
      handleHeadMethod(request);
    } else if (method.equals(HttpMethod.POST)) {
      logger.info("Handle request with POST method");
      handlePostMethod(request);
    } else if (method.equals(HttpMethod.DELETE)) {
      logger.info("Handle request with DELETE method");
      handleDeleteMethod(request);
    } else {
      logger.info("Handle request with PUT method");
      handlePutMethod(request);
    }
  }

  /**
   * Write the given chunk to the response buffer. Note that flush() must be called to write the output to the network.
   *
   * @param chunk chunk that will be write to the output buffer
   */
  protected void write(String chunk) {
    httpResponse.append(chunk);
  }

  /**
   * Finish the response and end the Http request
   *
   * @param request Http request to finish
   * @param message messages that we want to append to the response
   */
  private void finish(HttpRequest request, String message) {
    httpResponse.append(message);
    finish(request);
  }

  /**
   * Finish the response and end the Http request
   *
   * @param request Http request to finish
   */
  private void finish(HttpRequest request) {
    var response = httpResponse.getBodyBytes();

    // Set 'CONTENT-LENGTH' header in the response
    if (httpResponse.headerNotWritten() && !httpResponse.getHeaders().contains(HTTP_HEADER_KEY_CONTENT_LENGTH)) {
      var contentLength = response.length;
      httpResponse.getHeaders().add(HTTP_HEADER_KEY_CONTENT_LENGTH, Integer.toString(contentLength));
    }

    flush(request);
    request.finish();
    logWhenFinish(request);
    httpResponse.reset();
  }

  /**
   * Log the information when finish handling Http request
   *
   * @param request Http request that we finished handling
   */
  private void logWhenFinish(HttpRequest request) {
    var infoThreshold = 400;
    var warnThreshold = 500;

    var message = "STATUS = " + httpResponse.getStatus() + ", REQUEST SUMMARY = " + request.summary();
    var statusCode = httpResponse.getStatus().getCode();
    if (statusCode < infoThreshold) {
      logger.info(message);
    } else if (statusCode < warnThreshold) {
      logger.warn(message);
    } else {
      logger.error(message);
    }
  }

  private void flush(HttpRequest request) {
    logger.info("Start to flush the response of Http request");
    var responseBodyBytes = httpResponse.getBodyBytes();

    // If the header has not been written to the browser, we should generate and send it.
    var headerBytes = new byte[0];
    if (httpResponse.headerNotWritten()) {
      headerBytes = httpResponse.getHeadersBytes(request.getVersion());
    }

    // Ignore the chunk and write only headers for HEAD method
    if (request.getMethod().equals(HttpMethod.HEAD)) {
      if (headerBytes.length != 0) {
        request.write(headerBytes);
        return;
      }
    }

    // Write headers and body of the HTTP response
    if (headerBytes.length != 0) {
      logger.info("Write HTTP response Header of [" + headerBytes.length + "] bytes");
      request.write(headerBytes);
    }

    if (responseBodyBytes.length != 0) {
      logger.info("Write HTTP response Body of [" + responseBodyBytes.length + "] bytes");
      request.write(responseBodyBytes);
    }
  }

//
//  /**
//   * Override this method if a subclass wants to get customized error pages.
//   *
//   * @return html content of error page
//   */
//  protected String getErrorHtml(HttpStatus status) {
//    return "<html><title>" + status.toString() + "</title><body>" + status.toString() + "</body></html>";
//  }
//
//  /**
//   * Send the given HTTP error code to the browser. We also send the error HTML for the given error code as returned by
//   * getErrorHtml(). Override that method if you want custom error pages for your application.
//   *
//   * @param connection  Http connection
//   * @param status      Http status code
//   * @param httpRequest Http request to handle. This may be null if the request is malformed and we failed the construct
//   *                    a Http request.
//   */
//  private void sendError(HttpConnection connection, HttpStatus status, Optional<HttpRequest> httpRequest) {
//    httpRequest.ifPresent(r -> {
//      if (headersWritten) {
//        logger.error("Cannot send error response after headers written");
//        if (!finished) {
//          finish(r);
//        }
//        return;
//      }
//
//      resetResponse(r);
//      resetResponse(r);
//    });
//
//    connection.clearWriteCache();
//  }
//
//  /**
//   *
//   */
//  private void resetResponse(HttpRequest httpRequest) {
//
//  }
}
