(function() {

    var options = {
        animation: {
            duration: 1000
        },
        width: 100, height: 100,
        greenFrom: 0, greenTo: 60,
        redFrom: 80, redTo: 100,
        yellowFrom:60, yellowTo: 80,
        minorTicks: 10
    };

    var component = {
        name                : "Garbage collection",
        shortDescription    : "Compartment utilisation",
        dom_id              : "gc_compartments_container"
    };

    function GcCompartmentView() {

        var container = document.getElementById(component.dom_id);
        var currentRow;

        this.createGaugeContainer = function(gaugeId, name) {
            if (currentRow === undefined || currentRow.find("div.gaugeContainer").size() == 3) {
                currentRow = $("<div class='row' style='margin-top: 2em'></div>");
                $(container).append(currentRow);
            }

            $(currentRow).append("<div class='span1 gaugeContainer' id='" + gaugeId + "'></div>");
            $(currentRow).append("<div class='span3'><h2>" + prettyPrintString(name) + "</h2><p class='info'>" + getCompartmentDescription(name) + "</p></div>");

            return document.getElementById(gaugeId);
        }

    }

    var view;
    var formatter;

    var gauges = {};

    var bindings = {
        memoryPools : ko.observable()
    };



    bindings.memoryPools.subscribe(function(memoryPools) {
        if (view === undefined) {
            view = new GcCompartmentView;
            formatter = new google.visualization.NumberFormat({
                fractionDigits: 0,
                suffix: '%'
            });

            for (var name in memoryPools) {
                var value = memoryPools[name];
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

        for (var name in memoryPools) {
            var gaugeId = createDomId(name);
            var value = memoryPools[name];
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
            var memory_pool_usages = metrics.jvm.memory.memory_pool_usages;
            bindings.memoryPools(memory_pool_usages);
        }
    });




    function prettyPrintString(string) {
        string = string.replace("_", " ");
        return string.charAt(0).toUpperCase() + string.slice(1);
    }


    function createDomId(name) {
        return "jvm_gc_" + name.replace(/[^a-zA-Z]/g, "_");
    }

    function getCompartmentDescription(name) {
        if (name.match(/code.*cache/i)) {options
            return "The HotSpot Java VM also includes a code cache, containing memory that is used for compilation and storage of native code.";
        }
        else if (name.match(/eden/i)) {
            return "The pool from which memory is initially allocated for most objects.";
        }
        else if (name.match(/survivor/i)) {
            return "The pool containing objects that have survived the garbage collection of the Eden space.";
        }
        else if (name.match(/old|tenured/i)) {
            return "The pool containing objects that have existed for some time in the survivor space.";
        }
        else if (name.match(/permanent|perm gen/i)) {
            return "The pool containing all the reflective data of the virtual machine itself, such as class and method objects. With Java VMs that use class data sharing, this generation is divided into read-only and read-write areas.";
        }
        else {
            return "Unknown";
        }
    }

})();