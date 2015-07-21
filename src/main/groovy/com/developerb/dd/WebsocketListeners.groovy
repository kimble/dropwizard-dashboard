package com.developerb.dd

import groovy.json.JsonBuilder
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Keeps track of listening browsers
 *
 * @author Kim A. Betti
 */
class WebsocketListeners {

    private static final Logger log = LoggerFactory.getLogger(WebsocketListeners.class);

    private final Set<ServerWebSocket> sockets = new CopyOnWriteArraySet<ServerWebSocket>()

    void push(String topic, Object json) {
        JsonBuilder builder = new JsonBuilder()

        builder {
            namespace   topic
            payload     json
        }

        distribute builder.toString()
    }

    void distribute(String message) {
        sockets.each { socket ->
            try {
                socket.writeFinalTextFrame(message)
            }
            catch (Exception ex) {
                log.warn("Failed to send message to " + socket, ex)
            }
        }
    }

    void addListener(ServerWebSocket socket) {
        log.info("Accepted websocket connection: " + socket)
        sockets << socket
    }

    void removeListener(ServerWebSocket socket) {
        log.info("Lost websocket connection: " + socket)
        sockets.remove(socket)
    }

    boolean hasAtLeastOneListener() {
        return !sockets.isEmpty()
    }

}
