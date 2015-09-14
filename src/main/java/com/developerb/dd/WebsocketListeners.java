package com.developerb.dd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Keeps track of listening browsers
 *
 * @author Kim A. Betti
 */
public class WebsocketListeners {

    private static final Logger log = LoggerFactory.getLogger(WebsocketListeners.class);

    private final Set<ServerWebSocket> sockets = new CopyOnWriteArraySet<>();
    private final ObjectMapper jackson = new ObjectMapper();



    public void push(String topic, JsonNode json) {
        ObjectNode response = jackson.createObjectNode();
        response.put("namespace", topic);
        response.set("payload", json);

        distribute(response.toString());
    }

    public void distribute(final String message) {
        for (ServerWebSocket socket : sockets) {
            try {
                socket.writeFinalTextFrame(message);
            }
            catch (Exception ex) {
                log.warn("Failed to send message to " + socket, ex);
            }
        }
    }

    public void addListener(ServerWebSocket socket) {
        log.info("Accepted websocket connection: " + socket);
        sockets.add(socket);
    }

    public void removeListener(ServerWebSocket socket) {
        log.info("Lost websocket connection: " + socket);
        sockets.remove(socket);
    }

    public boolean hasAtLeastOneListener() {
        return !sockets.isEmpty();
    }

}
