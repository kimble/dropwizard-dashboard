package com.developerb.dd;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 *
 *
 * @author Kim A. Betti
 */
public class DropwizardDashboardStarter {

    private static final Logger log = LoggerFactory.getLogger(DropwizardDashboardStarter.class);

    public static void main(String... args) {

        Vertx vertx = Vertx.vertx();

        String baseUri = getSystemProperty("dropwizard.baseUri", "/");
        String dropwizardHostname = getSystemProperty("dropwizard.hostname", "localhost");
        Integer dropwizardAdminPort = getSystemInteger("dropwizard.port", 8081);
        Integer metricsPollInterval = getSystemInteger("dropwizard.metrics.seconds", 3) * 1000;
        Integer healthcheckPollInterval = getSystemInteger("dropwizard.healthcheck.seconds", 10) * 1000;

        Boolean useSsl = getSystemBoolean("dropwizard.ssl.enable", false);
        Boolean sslTrustAll = getSystemBoolean("dropwizard.ssl.trustAll", false);

        Integer dashboardServerPort = getSystemInteger("dashboard.port", 9000);

        DropwizardClient proxy = new DropwizardClient(dropwizardHostname, dropwizardAdminPort, baseUri, useSsl, sslTrustAll, vertx);
        WebsocketListeners listeners = new WebsocketListeners();

        Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create());

        HttpServer httpServer = vertx.createHttpServer()
                .requestHandler(router::accept);

        httpServer.websocketHandler(websocket -> {
            listeners.addListener(websocket);
            websocket.endHandler(event -> listeners.removeListener(websocket));
        });

        vertx.setPeriodic(metricsPollInterval, ping -> {
            if (listeners.hasAtLeastOneListener()) {
                proxy.fetchMetrics(listeners);
            }
        });

        vertx.setPeriodic(healthcheckPollInterval, ping -> {
            if (listeners.hasAtLeastOneListener()) {
                proxy.runHealthChecks(listeners);
            }
        });


        httpServer.listen(dashboardServerPort);
        log.info("Initialized, listening on port " + dashboardServerPort);
    }


    private static Integer getSystemInteger(String name, Integer fallback) {
        try {
            return Integer.parseInt(getSystemProperty(name, fallback.toString()));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Expected the value of " + name + " to be a valid integer", ex);
        }
    }

    private static Boolean getSystemBoolean(String name, Boolean fallback) {
        return getSystemProperty(name, fallback.toString()).equalsIgnoreCase("true");
    }

    private static String getSystemProperty(String name, String fallback) {
        String value = System.getProperty(name);

        if (value == null) {
            log.info(name + " not provided, falling back to '" + fallback + "'");
            return fallback;
        }
        else {
            return value;
        }
    }


}
