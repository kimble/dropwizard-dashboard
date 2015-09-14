
/**
 * Define some useful global streams and properties
 */
var wsBacon = (function() {
    "use strict";

    var websocketConnecting = pubsub.stream("websocket-connecting");
    var websocketConnectionFailed = pubsub.stream("websocket-connection-failed");
    var websocketOpened = pubsub.stream("websocket-opened");
    var websocketClosed = pubsub.stream("websocket-closed");

    return {
        isWebsocketOpen : websocketOpened.map(true)
            .merge(websocketClosed.map(false))
            .toProperty(false),

        isWebsocketConnecting : websocketConnecting.map(true)
            .merge(websocketConnectionFailed.map(false))
            .merge(websocketOpened.map(false))
            .merge(websocketClosed.map(false))
            .toProperty(false),

        backendConnectionRestored: pubsub.stream("connectionRestored"),
        backendConnectionLost: pubsub.stream("connectionLost")
    };
})();