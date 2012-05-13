(function() {

    var options = {
        "width": 400, "height": 200,
        "chartArea": {
            left: 10, top: 10,
            width: "80%", height: "90%"
        },
        "is3D": true,
        "legend": {
            position: "right",
            textStyle : {
                fontSize : 13
            }
        },
        backgroundColor : {
            fill : "transparent"
        }
    };

    var component = {
        name                : "Threads",
        shortDescription    : "Virtual machine threads",
        dom_id              : "jvm_threads_container"
    };

    var bindings = {
        virtualMachineThreads : ko.observable({})
    };

    var pieChart;

    Dropwizard.registerComponent({
        bindings : bindings,
        pageComponent : component,

        onMetrics : function(update) {
            bindings.virtualMachineThreads({
                states : update.jvm['thread-states'],
                count : update.jvm.thread_count,
                daemons : update.jvm.daemon_thread_count,
                time: update.jvm.current_time
            });
        },

        /**
         * Download and install the heap page component template and install it to
         * activate Knockout.js data binding.
         */
        beforeSocketConnect : function() {
            Dropwizard.installRemoteTemplate("thread-template", "/static/templates/threads.html");
            Dropwizard.appendTemplateTo("thread-template", document.getElementById(component.dom_id));

            var container = document.getElementById("jvm_thread_state_container");
            pieChart = new google.visualization.PieChart(container);

            bindings.virtualMachineThreads.subscribe(function(threads) {
                var data = [
                    ["Label", "Value"]
                ];

                for (var state in threads.states) {
                    if (threads.states.hasOwnProperty(state)) {
                        var value = threads.states[state];
                        state = prettyPrintString(state);
                        data.push([ state, Math.round(threads.count * value) ]);
                    }
                }

                var plot = google.visualization.arrayToDataTable(data);
                pieChart.draw(plot, options);
            });
        }
    });

    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    /*
    var container = document.getElementById("jvm_thread_state_container");
    var chart = undefined;

    var options = {
        "width": 300, "height": 250,
        "chartArea": {
            left: 10, top: 10,
            width: "80%", height: "90%"
        },
        "is3D": true,
        "legend": {
            position: "none"
        },
        backgroundColor : {
            fill : "transparent"
        }
    };

    Dropwizard.bindings.googleChartsLoaded.subscribe(function(value) {
        chart = new google.visualization.PieChart(container);
    });

    Dropwizard.bindings.virtualMachineThreads.subscribe(function(threads) {
        var data = [
            ["Label", "Value"]
        ];

        for (var state in threads.states) {
            var value = threads.states[state];
            state = prettyPrintString(state);
            data.push([ state, Math.round(threads.count * value) ]);
        }

        var plot = google.visualization.arrayToDataTable(data);
        chart.draw(plot, options);
    });

    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }*/

})();