package com.developerb.dd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.ConnectException;
import java.util.function.BiConsumer;

/**
 * @author Kim A. Betti
 */
public class DropwizardClient {

    private static final Logger log = LoggerFactory.getLogger(DropwizardClient.class);

    private final HttpClient client;
    private final ServerState state = new ServerState();
    private final ObjectMapper jackson = new ObjectMapper();


    public DropwizardClient(String hostname, Integer port, Vertx vertx) {
        if (hostname == null) {
            throw new IllegalArgumentException("Hostname can't be null");
        }
        if (port == null) {
            throw new IllegalArgumentException("Port can't be null");
        }

        HttpClientOptions httpOptions = new HttpClientOptions()
                .setConnectTimeout(10 * 10000)
                .setIdleTimeout(10 * 1000)
                .setDefaultHost(hostname)
                .setDefaultPort(port);

        client = vertx.createHttpClient(httpOptions);
    }


    void fetchMetrics(WebsocketListeners listeners) {
        client.getNow("/metrics", new MyResponseHandler(listeners, (response, jsonNode) -> {
            listeners.push("metrics", jsonNode);
        }));
    }

    void runHealthChecks(WebsocketListeners listeners) {
        client.getNow("/healthcheck", new MyResponseHandler(listeners, (response, jsonNode) -> {
            boolean healthy = response.statusCode() == 200;
            String topic = healthy ? "healthy" : "unhealthy";

            listeners.push(topic, jsonNode);
        }));
    }


    private class MyResponseHandler implements Handler<HttpClientResponse> {

        private final BiConsumer<HttpClientResponse, JsonNode> consumer;
        private final WebsocketListeners listeners;

        private MyResponseHandler(WebsocketListeners listeners, BiConsumer<HttpClientResponse, JsonNode> consumer) {
            this.consumer = consumer;
            this.listeners = listeners;
        }

        @Override
        public void handle(HttpClientResponse response) {
            response.exceptionHandler(ex -> {
                if (ex instanceof VertxException) {
                    if (ex.getMessage().equals("Connection was closed")) {
                        // Wtf!? Why is this an exception?!
                    }
                }
                else if (ex instanceof ConnectException) {
                    state.onFailedConnection(ex, listeners);
                }
                else {
                    log.warn("Unable to fetch metrics", ex);
                }
            });

            response.bodyHandler(body -> {
                JsonNode responseJson = deserializeJson(body);

                state.onSuccessfulConnection(listeners);
                consumer.accept(response, responseJson);
            });
        }

        private JsonNode deserializeJson(Buffer body) {
            try {
                String responseText = body.toString("utf-8");
                return jackson.readTree(responseText);
            }
            catch (Exception ex) {
                log.warn("Failed to de-serialize json response", ex);
                return null;
            }
        }

    }


    private class ServerState {

        volatile boolean accessible = true;

        synchronized void onSuccessfulConnection(WebsocketListeners listeners) {
            if (!accessible) {
                log.info("Restored connection to remote Dropwizard application!");

                JsonNode state = BooleanNode.TRUE;
                listeners.push("connectionRestored", state);
                accessible = true;
            }
        }

        synchronized void onFailedConnection(Throwable trouble, WebsocketListeners listeners) {
            if (accessible) {
                log.warn("Lost connection to remote Dropwizard application: " + trouble.getMessage());

                JsonNode state = BooleanNode.FALSE;
                listeners.push("connectionLost", state);
                accessible = false;
            }
        }

    }

}