

window.pubsub = (function() {
    "use strict";

    var bus = new Bacon.Bus();

    var stream = function(requestedNamespace) {
        var correctNamespace = function(event) {
            return requestedNamespace === "*" || event.namespace === requestedNamespace;
        };

        return bus.filter(correctNamespace).map(".data");
    };

    // Dead simple pubsub event-bus with reactive
    // capabilities provided by Bacon.js
    return {
        broadcast: function(event) {
            bus.push(event);
        },

        takeOne: function(type) {
            return stream(type).take(1);
        },

        stream: stream
    }
})();


google.load('visualization', '1', { packages:['corechart', 'gauge'] });
google.setOnLoadCallback(googleChartsLoaded);

// All components should have registered them self by now
Dropwizard.applyBindings();

function googleChartsLoaded() {
    Dropwizard.bindings.googleChartsLoaded(true);
    initializeWebsocketConnection();
}


function initializeWebsocketConnection() {

    if (window.WebSocket) {
        Dropwizard.bindings.beforeSocketConnect(true);

        var initialized = false;
        var socket = new WebSocket("ws://localhost:9000");
        socket.onmessage = function (event) {
            var json = JSON.parse(event.data);

            pubsub.broadcast({
                namespace: json.namespace,
                data: json.payload
            });

            if (json.namespace === "metrics") {
                Dropwizard.onMetrics(json.payload);

                if (initialized === false) {
                    $(".hiddenFromStart").css("visibility", "visible");
                    initialized = true;
                }
            }


            if (json.namespace === "healthy") {
                Dropwizard.bindings.healthCheckFailed(false);
            }
            if (json.namespace === "unhealthy") {
                Dropwizard.bindings.healthCheckFailed(json.payload);
            }
        };

        socket.onerror = function(event) {
            Dropwizard.bindings.connectionError(event);
        };

        socket.onopen = function (event) {
            Dropwizard.bindings.connectionToProxyEstablished(true)
        };

        socket.onclose = function (event) {
            Dropwizard.bindings.connectionToProxyLost(true)
        };
    }
    else {
        alert("Your browser does not support Websockets");
    }



}

pubsub.stream("*").log("Bus activity");


/**
 * Trigger heartbeat when any message is received
 */
(function() {
    var $heart = $("#heart");

    pubsub.stream("*").onValue(function() {
            $heart.fadeTo(100, 0.4, function () {
                $heart.fadeTo(300, 0.2);
            });
        });
})();


/**
 * Fade screen when the connection to Dropwizard goes away
 */
(function() {
    var body = $("body");

    pubsub.stream("connectionLost").onValue(function() {
        body.fadeTo(500, 0.5);
    });

    pubsub.stream("connectionRestored").onValue(function() {
        body.fadeTo(500, 1.0);
    });
})();