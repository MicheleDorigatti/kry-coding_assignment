package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class BackgroundPoller {

  public Future<List<String>> pollServices(Vertx vertx, Map<String, String> services) {
    WebClient client = WebClient.create(vertx);
    for (String service: services.keySet()) {
      URL url = null;
      try {
        url = new URL(service);
      } catch (MalformedURLException e) {
        e.printStackTrace();
        continue;
      }
      String host = url.getHost();
      int port = url.getDefaultPort();
      client
              .get(port, host, "/")
              .ssl(true)
              .send(asyncResult -> {
                if (asyncResult.succeeded()) {
                  String status = asyncResult.result().statusMessage();
                  services.put(service, status);
                }
                else if (asyncResult.failed()) {
                  System.out.println(asyncResult.cause());
                  services.put(service, "FAIL");
                }
              });
    }
    return Future.failedFuture("TODO");
  }
}
