# Tornado in Java

Hurricane ports parts of [Tornadao](https://github.com/tornadoweb/tornado/tree/stable) version 1.0. My goal of this side project is to

- Have a better understanding of epoll, Java NIO and asynchronous programming
- How a minimal web server and web framework works

## Current Status

As of right now, the following features have been implemented

- An I/O loop that handles time events, callback and network IO events
- A non-blocking TCP server that with user-defined handlers for ACCEPT, READ, WRITE events
- A non-blocking HTTP server with very limited HTTP protocol support
- A web framework that supports request routing
- A sample Hello Wold web application

## Hello, World

Here is a simple "Hello, World!" example web app for Hurricane:

```java
public class HelloWorld {
  public static void main(String[] args) throws IOException {
    RequestHandler requestHandler = new RequestHandler() {
      @Override
      protected void handleGetMethod(HttpRequest request) throws HttpException {
        write("Hello World!");
      }
    };

    var application = new Application(Collections.singletonList(new UrlSpec(".*", requestHandler)));

    HttpServer httpServer = HttpServer.getInstance();
    httpServer.setApplication(application);
    httpServer.listen(8888);

    EventLoop.getInstance().start();
  }
}
```
