google.load('visualization', '1', { packages:['corechart', 'gauge'] });
google.setOnLoadCallback(googleChartsLoaded);


// All components should have registered them self by now
Dropwizard.applyBindings();

function googleChartsLoaded() {
    Dropwizard.bindings.googleChartsLoaded(true);
    initializeWebsocketConnection();
}


function initializeWebsocketConnection() {

    var heart = $("#heart");

    if (window.WebSocket) {
        Dropwizard.bindings.beforeSocketConnect(true);

        var initialized = false;
        var socket = new WebSocket("ws://ec2-54-245-134-66.us-west-2.compute.amazonaws.com:9000");
        socket.onmessage = function (event) {
            triggerHeartBeat();

            var json = JSON.parse(event.data);
            if (json.namespace === "metrics") {
                Dropwizard.onMetrics(json.payload);

                if (initialized === false) {
                    $(".hiddenFromStart").css("visibility", "visible");
                    initialized = true;
                }
            }
            if (json.namespace === "connectionLost") {
                Dropwizard.bindings.proxyConnectionToDropwizardLost(true);
            }
            if (json.namespace === "connectionRestored") {
                Dropwizard.bindings.proxyConnectionToDropwizardRestored(true);
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

    function triggerHeartBeat() {
        heart.fadeTo(100, 0.4, function () {
            heart.fadeTo(300, 0.2);
        });
    }

    var body = $("body");

    Dropwizard.bindings.proxyConnectionToDropwizardLost.subscribe(function () {
        body.fadeTo(500, 0.6);
    });

    Dropwizard.bindings.proxyConnectionToDropwizardRestored.subscribe(function () {
        body.fadeTo(500, 1.0);
    });

    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

}