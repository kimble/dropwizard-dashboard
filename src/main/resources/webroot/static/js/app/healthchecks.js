(function() {
    "use strict";

    var prop = function(name) {
        return function(obj) {
            return obj[name];
        };
    };

    var isTrue = function(name) {
        return function(obj) {
            return obj[name] === true;
        };
    };

    var not = function(fn) {
        return function(arg) {
            return !fn(arg);
        };
    };

    var notNull = function(input) {
        return input != null;
    };

    var healthcheckList = pubsub.stream("unhealthy")
        .merge(pubsub.stream("healthy"))
        .map(function(health) {
            return Object.keys(health).map(function(name) {
                return {
                    name: name,
                    healthy: health[name].healthy,
                    message: health[name].message
                };
            });
        });


    var container = d3.select("#healthCheckContainer");


    healthcheckList.onValue(function(list) {
        var healthCheck = container.selectAll(".health-check")
            .data(list, prop("name"));


        var enterHealthCheck = healthCheck.enter()
            .append("div")
            .attr("class", "health-check")
            .classed("healthy", isTrue("healthy"))
            .classed("unhealthy", not(isTrue("healthy")));

        enterHealthCheck.append("span")
            .attr("class", "glyphicon glyphicon-ok-sign healthy");

        enterHealthCheck.append("span")
            .attr("class", "glyphicon glyphicon-remove-sign unhealthy");

        enterHealthCheck.append("span")
            .attr("class", "name")
            .text(prop("name"));

        enterHealthCheck.append("blockquote")
            .attr("class", "message")
            .classed("hidden", function(hc) {
                return hc.message == null;
            })
            .text(prop("message"));



        healthCheck.exit().remove();
    });
})();