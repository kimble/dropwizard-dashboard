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
        broadcast: function(namespace, data) {
            bus.push({
                namespace: namespace,
                data: data
            });
        },

        takeOne: function(type) {
            return stream(type).take(1);
        },

        stream: stream
    }
})();


// For debugging..
// pubsub.stream("*").log("Bus activity");