(function() {

    var component = {
        name                : "Logging",
        shortDescription    : "Application logging statistics",
        dom_id              : "application_logging_container"
    };


    var bindings = {
        applicationLogging : ko.observable({}),

        applicationLoggingAll : ko.observable({}),
        applicationLoggingTrace : ko.observable({}),
        applicationLoggingInfo : ko.observable({}),
        applicationLoggingWarn : ko.observable({}),
        applicationLoggingError : ko.observable({})
    };

    var updateLogging = function(logging) {
        console.log("Updating logging")
    };

    Dropwizard.registerComponent({
        bindings : bindings,
        pageComponent : component,

        onMetrics : function(update) {
            var logging = update["ch.qos.logback.core.Appender"];
            bindings.applicationLoggingAll({
                count   : logging.all.count,
                m1      : roundNumber(logging.all.m1, 2)
            });

            bindings.applicationLoggingTrace({
                count : logging.trace.count
            });

            bindings.applicationLoggingInfo({
                count : logging.info.count
            });

            bindings.applicationLoggingWarn({
                count : logging.warn.count
            });

            bindings.applicationLoggingError({
                count : logging.error.count
            });
        },

        /**
         * Download and install the heap page component template and install it to
         * activate Knockout.js data binding.
         */
        beforeSocketConnect : function() {
            Dropwizard.installRemoteTemplate("logging-template", "/static/templates/logging.html");
            Dropwizard.appendTemplateTo("logging-template", document.getElementById(component.dom_id));

            bindings.applicationLogging.subscribe(updateLogging);
        }
    });

    function roundNumber(num, dec) {
        return Math.round(num*Math.pow(10,dec))/Math.pow(10,dec);
    }

})();