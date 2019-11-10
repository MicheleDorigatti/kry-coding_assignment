package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import java.util.List;
import java.util.Map;

public class BackgroundPoller {

  public Future<List<String>> pollServices(Vertx vertx, Map<String, String> services) {
    WebClient client = WebClient.create(vertx);
    for (String service: services.keySet()) {
      client
              .get(443, service, "/")
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
    //client.close();
    return Future.failedFuture("TODO");
  }
}
