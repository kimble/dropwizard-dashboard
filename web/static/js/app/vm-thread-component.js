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
                states : update.gauges,
                count : update.gauges["jvm.threads.count"].value,
                daemons : update.gauges["jvm.threads.daemon.count"].value,
                time: new Date()
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
					if (state.match(/jvm\.threads\..*\.count/g)) {
                        var value = threads.states[state].value;
                        state = prettyPrintString(state);
                        data.push([ state, Math.round(value) ]);
                    }
                }

                var plot = google.visualization.arrayToDataTable(data);
                pieChart.draw(plot, options);
            });
        }
    });

    function prettyPrintString(string) {
        string = string.slice(12);
		string = string.substring(0,string.length-5);
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

})();