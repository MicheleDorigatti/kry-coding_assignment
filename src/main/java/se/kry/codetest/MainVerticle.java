package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> services = new HashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    DBConnector connector = setupDB();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    populateServicesFromDB(connector, services);
    vertx.setPeriodic(1000 * 5, timerId -> poller.pollServices(vertx, services));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void deleteService(String url)  {
    services.remove(url);
    connector.query("DELETE FROM TABLE Services WHERE URL = '" +  url + "';");
  }

  private void addService(String name, String url) {
    services.put(url, "UNKNOWN");
    connector.query("INSERT INTO Services (Name, URL) VALUES (" + name + ", " + url +  ");");
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());

    router.get("/service").handler(req -> {
      List<JsonObject> jsonServices = services
          .entrySet()
          .stream()
          .map(service ->
              new JsonObject()
                  .put("name", service.getKey())
                  .put("status", service.getValue()))
          .collect(Collectors.toList());
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });

    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      addService(jsonBody.getString("name"), jsonBody.getString("url"));
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });

    // API for deleting a service
    router.delete("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String url = jsonBody.getString("url");
      if (services.containsKey(url)) {
        services.remove(url);
        req.response()
                .putHeader("content-type", "text/plain")
                .setStatusCode(204)
                .end("OK");
      }
      else {
        req.response()
                .setStatusCode(404)
                .end();
      }
    });
  }

  private DBConnector setupDB() {
    connector = new DBConnector(vertx);
    //connector.query("DROP TABLE Services;");
    Future<ResultSet> result = connector.query("CREATE TABLE IF NOT EXISTS Services (Name VARCHAR(255), URL VARCHAR(255), Inserted DATETIME DEFAULT CURRENT_TIMESTAMP, Status CHAR(10) DEFAULT 'UNKNOWN');");
    result.setHandler(asyncResult -> {
      if (asyncResult.failed()) {
        System.out.println(asyncResult.cause());
      }
    });
    //connector.query("INSERT INTO Services (Name, URL) VALUES ('kry', 'https://www.kry.se');");
    //connector.query("INSERT INTO Services (Name, URL) VALUES ('non existing', 'http://a.non.existing.url');");

    return connector;
  }

  private void populateServicesFromDB(DBConnector connector, HashMap<String, String> services) {
    Future<ResultSet> result = connector.query("SELECT * FROM Services;");

    result.setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        for (JsonObject row : asyncResult.result().getRows()) {
          System.out.println(row.getString(row.encodePrettily()));

          services.put(row.getString("URL"), row.getString("Status"));
        }

        System.out.println("DB succ");
        System.out.println(asyncResult.result());
      } else if (asyncResult.failed()) {
        System.out.println("DB fail");
        System.out.println(asyncResult.cause());
      }
    });
  }

}



