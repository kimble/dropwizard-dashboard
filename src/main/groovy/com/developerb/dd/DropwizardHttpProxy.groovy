package com.developerb.dd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import io.vertx.core.Vertx
import io.vertx.core.VertxException
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

/**
 * Responsible for speaking with the remote Dropwizard application.
 *
 * @author Kim A. Betti
 */
class DropwizardHttpProxy {

    private static final Logger log = LoggerFactory.getLogger(DropwizardHttpProxy.class);

    private final HttpClient client;
    private final ServerState state = new ServerState()


    public DropwizardHttpProxy(String hostname, Integer port, Vertx vertx) {
        if (hostname == null) {
            throw new IllegalArgumentException("Hostname can't be null")
        }
        if (port == null) {
            throw new IllegalArgumentException("Port can't be null")
        }

        HttpClientOptions httpOptions = new HttpClientOptions()
                .setConnectTimeout(10 * 10000)
                .setIdleTimeout(10 * 1000)
                .setDefaultHost(hostname)
                .setDefaultPort(port)

        client = vertx.createHttpClient(httpOptions)
    }


    void fetchMetrics(WebsocketListeners listeners) {
        client.getNow("/metrics") { response ->
            response.exceptionHandler { ex ->
                if (ex instanceof VertxException) {
                    if (ex.message == "Connection was closed") {
                        // Wtf!? Why is this an exception?!
                    }
                }
                else if (ex instanceof ConnectException) {
                    state.onFailedConnection(ex, listeners)
                }
                else {
                    log.warn("Unable to fetch metrics", ex)
                }
            }

            response.bodyHandler { body ->
                state.onSuccessfulConnection(listeners)

                String responseText = body.toString("utf-8")
                ObjectMapper jackson = new ObjectMapper()
                JsonNode json = jackson.readTree(responseText)

                listeners.push("metrics", json)
            }
        }
    }

    void runHealthChecks(WebsocketListeners listeners) {
        client.getNow("/healthcheck") { response ->
            boolean healthy = response.statusCode() == 200

            response.exceptionHandler { ex ->
                if (ex instanceof VertxException) {
                    if (ex.message == "Connection was closed") {
                        // Wtf!? Why is this an exception?!
                    }
                }
                else if (ex instanceof ConnectException) {
                    state.onFailedConnection(ex, listeners)
                }
                else {
                    log.warn("Unable to fetch metrics", ex)
                }
            }

            response.bodyHandler { body ->
                state.onSuccessfulConnection(listeners)

                String responseText = body.toString("utf-8")
                String topic = healthy ? "healthy" : "unhealthy"
                JsonNode payload = new TextNode(responseText)

                listeners.push(topic, payload);
            }
        }
    }


    private class ServerState {

        volatile boolean accessible = true

        synchronized void onSuccessfulConnection(WebsocketListeners listeners) {
            if (!accessible) {
                log.info("Restored connection to remote Dropwizard application!")

                listeners.push("connectionRestored", true)
                accessible = true
            }
        }

        synchronized void onFailedConnection(Throwable trouble, WebsocketListeners listeners) {
            if (accessible) {
                log.warn("Lost connection to remote Dropwizard application: " + trouble.getMessage())

                listeners.push("connectionLost", true)
                accessible = false
            }
        }

    }

}
