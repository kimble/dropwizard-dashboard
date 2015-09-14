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
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.function.BiConsumer;

/**
 * @author Kim A. Betti
 */
public class DropwizardClient {

    private static final Logger log = LoggerFactory.getLogger(DropwizardClient.class);

    private final HttpClient client;
    private final ServerState state = new ServerState();
    private final ObjectMapper jackson = new ObjectMapper();
    private final String baseUri;


    public DropwizardClient(String hostname, Integer port, String baseUri, Boolean useSsl, Boolean sslTrustAll, Vertx vertx) {
        HttpClientOptions httpOptions = new HttpClientOptions()
                .setConnectTimeout(10 * 10000)
                .setIdleTimeout(10 * 1000)
                .setSsl(useSsl)
                .setTrustAll(sslTrustAll)
                .setDefaultHost(hostname)
                .setDefaultPort(port);

        this.baseUri = baseUri;
        this.client = vertx.createHttpClient(httpOptions);
    }


    void fetchMetrics(WebsocketListeners listeners) {
        HttpClientRequest request = client.get(baseUri + "metrics", new MyResponseHandler(listeners, (response, jsonNode) -> {
            listeners.push("metrics", jsonNode);
        }));

        request.exceptionHandler(trouble -> {
            if (trouble instanceof SocketException) {
                state.onFailedConnection(trouble, listeners);
            }
        });

        request.end();
    }

    void runHealthChecks(WebsocketListeners listeners) {
        HttpClientRequest request = client.get(baseUri + "healthcheck", new MyResponseHandler(listeners, (response, jsonNode) -> {
            boolean healthy = response.statusCode() == 200;
            String topic = healthy ? "healthy" : "unhealthy";

            listeners.push(topic, jsonNode);
        }));

        request.exceptionHandler(trouble -> {
            if (trouble instanceof SocketException) {
                state.onFailedConnection(trouble, listeners);
            }
        });

        request.end();
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
            String responseText = body.toString("utf-8");

            try {
                return jackson.readTree(responseText);
            }
            catch (Exception ex) {
                log.warn("Failed to de-serialize json response: " + responseText, ex);
                return null;
            }
        }

    }

    boolean hasBackendConnection() {
        return state.accessible;
    }


    private class ServerState {

        volatile boolean accessible = false;

        synchronized void onSuccessfulConnection(WebsocketListeners listeners) {
            if (!accessible) {
                log.info("Restored connection to remote Dropwizard application!");

                JsonNode state = BooleanNode.TRUE;
                listeners.push("backend-connected", state);
                accessible = true;
            }
        }

        synchronized void onFailedConnection(Throwable trouble, WebsocketListeners listeners) {
            if (accessible) {
                log.warn("Lost connection to remote Dropwizard application: " + trouble.getMessage());

                JsonNode state = BooleanNode.FALSE;
                listeners.push("backend-disconnected", state);
                accessible = false;
            }
        }

    }

}
