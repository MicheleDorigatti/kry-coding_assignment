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
  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    DBConnector connector = setupDB();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    populateServicesFromDB(connector, services);
    vertx.setPeriodic(1000 * 1, timerId -> poller.pollServices(vertx, services));
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

  public void deleteService(String url)  {
    if (! services.containsKey(url)) {
      System.out.println("key not found");
      return;
    }

    services.remove(url);
    connector.query("DELETE FROM TABLE Services WHERE Name = '" +  url + "';");
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
      services.put(jsonBody.getString("url"), "UNKNOWN");
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });
  }

  private DBConnector setupDB() {
    connector = new DBConnector(vertx);
    //connector.query("DROP TABLE Services;");
    connector.query("CREATE TABLE IF NOT EXISTS Services (Name VARCHAR(255), Status CHAR(10));");
    //connector.query("INSERT INTO Services VALUES ('https://www.kry.se', 'UNKNOWN');");
    //connector.query("INSERT INTO Services VALUES ('http://a.non.existing.url', 'UNKNOWN');");

    return connector;
  }

  private void populateServicesFromDB(DBConnector connector, HashMap<String, String> services) {
    Future<ResultSet> result = connector.query("SELECT * FROM Services;");

    result.setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        for (JsonObject row : asyncResult.result().getRows()) {
          System.out.println(row.getString("Name"));
          System.out.println(row.getString("Status"));
          services.put(row.getString("Name"), row.getString("Status"));
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



