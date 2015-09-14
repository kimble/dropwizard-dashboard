/**
 * Trigger heartbeat when any message is received
 */
(function() {
    var $heart = $("#heart");

    var metricStream = pubsub.stream("metrics");
    var healthStream = pubsub.stream("healthcheck");

    metricStream.merge(healthStream).onValue(function() {
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


/**
 * Display warning about websocket connection
 */
(function() {
    var $warning = $("#websocketConnectionWarning");
    wsBacon.isWebsocketOpen.assign($warning, "toggleClass", "hidden");
})();


/**
 * Virtual machine / host information
 */
(function() {
    var gauges = pubsub.stream("metrics").map(".gauges");

    var streamGaugeValues = function(name) {
        var correctGauge = function (all) {
            return all[name];
        };

        return gauges.map(correctGauge)
            .map(".value")
            .skipDuplicates();
    };


    streamGaugeValues("jvm.attribute.vendor")
        .onValue(function(vendor) {
            $("#jvmVendorName").html(vendor);
        });

    streamGaugeValues("jvm.attribute.name")
        .onValue(function(attribute) {
            $("#jvmAttribute").html(attribute);
        });

    streamGaugeValues("jvm.attribute.uptime")
        .onValue(function(uptime) {
            var formatted = moment.humanizeDuration(uptime);
            $("#serverTime").html(formatted);
        });
})();