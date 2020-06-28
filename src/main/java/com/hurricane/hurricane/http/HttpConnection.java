package com.hurricane.hurricane.http;

import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.tcp.callback.TcpFlushHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadBytesHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadDelimiterHandler;
import com.hurricane.hurricane.tcp.connection.TcpConnection;
import com.hurricane.hurricane.web.RequestHandler;
import java.nio.charset.StandardCharsets;
import org.apache.log4j.Logger;

import static com.hurricane.hurricane.common.Constant.*;


/**
 * @author larrytaowang This class handles a connection to an Http client, executing Http requests. We parse HTTP
 * headers and bodies, and execute the request callback until the HTTP connection is closed.
 */
public class HttpConnection {
  private final static Logger logger = Logger.getLogger(HttpConnection.class);

  /**
   * The TCP connection under this HTTP connection
   */
  private final TcpConnection tcpConnection;

  /**
   * If the connection should keep alive after finishing serving a HTTP request
   */
  private final boolean isNoKeepAlive;

  /**
   * Current HTTP request
   */
  private HttpRequest httpRequest;

  /**
   * If the request should be finished
   */
  private boolean requestFinished;

  /**
   * This callback is called when the whole request has been parsed. Note that it should be only called once. If there
   * are both Http header and body, we should only call it after the body is parsed.
   */
  private final RequestHandler requestHandler;

  public HttpConnection(TcpConnection tcpConnection, RequestHandler requestHandler) {
    this.tcpConnection = tcpConnection;
    EventLoop.getInstance().registerTcpConnection(tcpConnection.getKey(), tcpConnection);

    this.isNoKeepAlive = false;
    this.requestHandler = requestHandler;
  }

  /**
   * After this function, the server starts to receives data from client. The server will parse the Http header when
   * enough data has been received to construct it.
   */
  public void activate() {
    tcpConnection.setReadHandler(new TcpReadDelimiterHandler(HTTP_HEADER_DELIMITER, this::onHttpHeaderReceived));
  }

  /**
   * Write the data to client asynchronously. When finished, execute the complete callback.
   *
   * @param data data write to client
   */
  public void write(byte[] data) {
    tcpConnection.setWriteHandlerWithData(new TcpFlushHandler(this::onWriteCompleteCallback), data);
  }

  /**
   * Callback that will be run when write finishes. Finish the request if needed.
   *
   * @param connection TCP connection that is used for this HTTP connection
   */
  private void onWriteCompleteCallback(TcpConnection connection) {
    if (requestFinished) {
      finishRequest();
    }
  }

  /**
   * This function is called to parse the HTTP header when server receives complete data. A callback for parsing the
   * HTTP body should be registered if the server finds out that HTTP request body will be available after parsing the
   * CONTENT-LENGTH filed.A request callback will be called if
   * <ul>
   *   <li>finish parsing Http header and no more data for Http body</li>
   *   <li>finish parsing Http header and Http body</li>
   * </ul>
   *
   * @param connection      tcp connection of this Http connection
   * @param httpHeaderBytes bytes received to construct a Http header
   */
  protected void onHttpHeaderReceived(TcpConnection connection, byte[] httpHeaderBytes) {
    String httpRequestLines = new String(httpHeaderBytes, StandardCharsets.UTF_8);
    httpRequest = HttpRequest.parseHttpRequestHeaders(httpRequestLines, this);

    var isBodyParsed = parseHttpBodyIfNeeded(httpRequest);
    logger.info("Finish parse Http Request, request = " + httpRequest.toString());

    // When we finish parsing request, call the callback. However, we want to call the callback exactly once. Therefore,
    // If the callback is called when parsing the Http body, we should not call it again.
    if (!isBodyParsed && requestHandler != null) {
      requestHandler.run(this, httpRequest);
    }
  }

  /**
   * Check the CONTENT-LENGTH filed to see if parsing HTTP request body is necessary.
   *
   * @param request HTTP request
   * @return if the server need to parse the HTTP request body
   */
  private boolean parseHttpBodyIfNeeded(HttpRequest request) {
    // Parse CONTENT-LENGTH filed in the header
    var headers = request.getHttpHeaders();
    var contentLengthString = headers.getValues(HTTP_HEADER_KEY_CONTENT_LENGTH);

    // Don't need to parse HTTP body since CONTENT-LENGTH is empty
    if (contentLengthString.isEmpty()) {
      return false;
    }

    var contentLength = 0;
    try {
      contentLength = Integer.parseInt(contentLengthString);
    } catch (NumberFormatException e) {
      logger.warn("Failed to parse Http body. Malformed Content Length = " + contentLengthString);
      throw e;
    }

    if (contentLength > tcpConnection.getReadManager().getMaxCacheSize()) {
      throw new IllegalArgumentException("Content-Length is larger than Tcp connection max cache size");
    }

    // HTTP 100 Continue: To have a server check if the request could be accepted based on the request’s headers
    // alone, a client must send Expect: 100-continue as a header in its initial request and check if a 100 Continue
    // status code is received in response before continuing (or receive 417 Expectation Failed and not continue).
    if (headers.getValues(HTTP_HEADER_KEY_EXPECT).equals(HTTP_HEADER_EXPECT_VALUE_100_CONTINUE)) {
      var response = HTTP_100_CONTINUE_RESPONSE.getBytes(StandardCharsets.UTF_8);
      write(response);
    }

    // This server need to parse the HTTP body with desired content length
    tcpConnection.setReadHandler(new TcpReadBytesHandler(contentLength, this::onHttpBodyReceived));
    return true;
  }

  /**
   * This function is called to parse the HTTP request body when server receives complete data.
   *
   * @param connection    tcp connection of that HTTP connection
   * @param httpBodyBytes bytes for complete HTTP request body
   */
  private void onHttpBodyReceived(TcpConnection connection, byte[] httpBodyBytes) {
    httpRequest.parseBody(httpBodyBytes);
    if (requestHandler != null) {
      requestHandler.run(this, httpRequest);
    }
  }

  /**
   * Finish the HTTP request if there is no pending data for write
   */
  public void finish() {
    this.requestFinished = true;
    if (!tcpConnection.isWriting()) {
      finishRequest();
    }
  }

  /**
   * Disconnect if there is no need to keep alive. Otherwise start to serve the next HTTP request.
   */
  private void finishRequest() {
    var disconnect = false;

    if (isNoKeepAlive) {
      // If not keep alive, a HttpConnect is closed after the request is finished
      disconnect = true;
    } else {
      // Check the request headers to see if we need to close after finished
      disconnect = httpRequest.disconnectWhenFinish();
    }

    httpRequest = null;
    requestFinished = false;

    if (disconnect) {
      tcpConnection.closeConnection();
    } else {
      // If we should not disconnect after serving the request, continue to read the headers of next request
      tcpConnection.setReadHandler(new TcpReadDelimiterHandler(HTTP_HEADER_DELIMITER, this::onHttpHeaderReceived));
    }
  }

  public HttpRequest getHttpRequest() {
    return httpRequest;
  }
}
