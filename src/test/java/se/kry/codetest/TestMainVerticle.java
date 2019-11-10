package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
    private MainVerticle verticle;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
      verticle = new MainVerticle();
    vertx.deployVerticle(verticle, testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
      try {
          TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
          e.printStackTrace();
      }

      //verticle.deleteService("https://www.kry.se");

      WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          JsonArray body = response.result().bodyAsJsonArray();
          assertEquals(2, body.size());
          System.out.println(body.encodePrettily());
          testContext.completeNow();
        }));

  }

}
