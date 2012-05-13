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
        var socket = new WebSocket("ws://localhost:9000");
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

    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

}