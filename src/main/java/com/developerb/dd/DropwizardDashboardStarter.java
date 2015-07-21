package com.developerb.dd;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Fragile boot script making it possible to try out Dropwizard Dashboard
 * without installing or really knowing anything about Vertx.
 *
 * There has to be a more convenient way to do this..?
 *
 * @author Kim A. Betti
 */
public class DropwizardDashboardStarter {

    private static final Logger log = LoggerFactory.getLogger(DropwizardDashboardStarter.class);

    public static void main(String... args) {

        Vertx vertx = Vertx.vertx();

        WebsocketListeners listeners = new WebsocketListeners();
        DropwizardHttpProxy proxy = new DropwizardHttpProxy("localhost", 8081, vertx);

        Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create());

        HttpServer httpServer = vertx.createHttpServer()
                .requestHandler(router::accept);

        httpServer.websocketHandler(websocket -> {
            listeners.addListener(websocket);
            websocket.endHandler(event -> listeners.removeListener(websocket));
        });

        vertx.setPeriodic(3000, ping -> {
            if (listeners.hasAtLeastOneListener()) {
                proxy.fetchMetrics(listeners);
            }
        });

        vertx.setPeriodic(10000, ping -> {
            if (listeners.hasAtLeastOneListener()) {
                proxy.runHealthChecks(listeners);
            }
        });


        int serverPort = 9000;
        httpServer.listen(serverPort);
        log.info("Initialized, listening on port " + serverPort);
    }

}
