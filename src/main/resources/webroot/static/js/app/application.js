

(function() {
    "use strict";

    if (window.WebSocket) {
        var createSocket = function(uri) {
            console.info("Connecting to.. " + uri);
            var socket;

            try {
                pubsub.broadcast("websocket-connecting", uri);
                socket = new WebSocket(uri);
            }
            catch (err) {
                pubsub.broadcast("websocket-connection-failed", uri);
            }

            socket.onmessage = function (event) {
                var json = JSON.parse(event.data);
                pubsub.broadcast(json.namespace, json.payload);
            };

            socket.onerror = function(event) {
                pubsub.broadcast("websocket-error", event);
            };

            socket.onopen = function (event) {
                pubsub.broadcast("websocket-opened", event);
            };

            socket.onclose = function (event) {
                pubsub.broadcast("websocket-closed", event);
            };
        };


        Bacon.once()
            .merge(Bacon.interval(5000))
            .filter(wsBacon.isWebsocketOpen.not())
            .filter(wsBacon.isWebsocketConnecting.not())
            .assign(createSocket, "ws://localhost:9000");
    }
    else {
        alert("Your browser does not support Websockets");
    }
})();

