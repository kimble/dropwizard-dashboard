(function() {

    var options = {
        animation: {
            duration: 1000
        },
        width: 100, height: 100,
        greenFrom: 0, greenTo: 25,
        yellowFrom:25, yellowTo: 50,
        redFrom: 50, redTo: 100,
        minorTicks: 10
    };

    var component = {
        name                : "HTTP error responses",
        shortDescription    : "Live error response gauges",
        dom_id              : "http_errors_container"
    };

    function HttpErrorView() {

        var container = document.getElementById(component.dom_id);
        var currentRow;

        this.createGaugeContainer = function(gaugeId, name) {
            if (currentRow === undefined || currentRow.find("div.gaugeContainer").size() == 2) {
                currentRow = $("<div class='row' style='margin-top: 2em'></div>");
                $(container).append(currentRow);
            }

            $(currentRow).append("<div class='span1 gaugeContainer' id='" + gaugeId + "'></div>");
            $(currentRow).append("<div class='span3'><h2>" + prettyPrintString(name) + "</h2></div>");

            return document.getElementById(gaugeId);
        }

    }

    var view;
    var formatter;

    var gauges = {};

    var bindings = {
        httpErrors : ko.observable()
    };

    bindings.httpErrors.subscribe(function(errors) {
        if (view === undefined) {
            view = new HttpErrorView;
            formatter = new google.visualization.NumberFormat({
                fractionDigits: 0,
                suffix: '%'
            });

            for (var name in errors) {
                var value = errors[name];
                var gaugeId = createDomId(name);
                var container = view.createGaugeContainer(gaugeId, name);

                gauges[gaugeId] = {
                    chart     : new google.visualization.Gauge(container),
                    dataTable : google.visualization.arrayToDataTable([
                                    ['Label', 'Value'],
                                    ['', 0]
                                ])
                };
            }
        }

        for (var name in errors) {
            var gaugeId = createDomId(name);
            var value = errors[name];
            var gauge = gauges[gaugeId];

            gauge.dataTable.setCell(0, 1, Math.round(value * 100));
            formatter.format(gauges[gaugeId].dataTable, 1);
            gauge.chart.draw(gauge.dataTable, options);
        }
    });

    Dropwizard.registerComponent({
        bindings : bindings,
        pageComponent : component,

        onMetrics : function(metrics) {
            var servletInfo = metrics["org.eclipse.jetty.servlet.ServletContextHandler"];

            var errors = {
                "percent 4xx 1m": servletInfo["percent-4xx-1m"].value,
                "percent 4xx 5m": servletInfo["percent-4xx-5m"].value,
                "percent 5xx 1m": servletInfo["percent-5xx-1m"].value,
                "percent 5xx 5m": servletInfo["percent-5xx-5m"].value
            };

            bindings.httpErrors(errors);
        }
    });

    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    function createDomId(name) {
        return "http_errors_" + name.replace(/[^a-zA-Z0-9]/g, "_");
    }

})();