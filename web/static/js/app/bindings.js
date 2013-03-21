(function() {

    var defaultBindings = {
        googleChartsLoaded : ko.observable(false),
        connectionToProxyEstablished : ko.observable(false),
        connectionToProxyLost : ko.observable(false),
        connectionError : ko.observable(),

        proxyConnectionToDropwizardLost : ko.observable(false),
        proxyConnectionToDropwizardRestored : ko.observable(false),

        beforeSocketConnect : ko.observable(),

        healthCheckFailed: ko.observable(false),

        pageComponents : ko.observableArray(),
        metrics : ko.observable()
    };

    var initializerContext = {
        bindings : defaultBindings
    };

    Dropwizard = {

        registerComponent : function(component) {
            for (var bindingName in component.bindings) {
                defaultBindings[bindingName] = component.bindings[bindingName];
            }

            if (component.hasOwnProperty("pageComponent")) {
                var pageComponent = component.pageComponent;
                defaultBindings.pageComponents.push(pageComponent);
            }

            if (component.hasOwnProperty("onMetrics")) {
                defaultBindings.metrics.subscribe(component.onMetrics);
            }

            if (component.hasOwnProperty("onDropwizardConnectionLost")) {
                defaultBindings.proxyConnectionToDropwizardLost.subscribe(component.onDropwizardConnectionLost);
            }

            if (component.hasOwnProperty("onDropwizardConnectionRestored")) {
                defaultBindings.proxyConnectionToDropwizardRestored.subscribe(component.onDropwizardConnectionRestored);
            }

            if (component.hasOwnProperty("beforeSocketConnect")) {
                defaultBindings.beforeSocketConnect.subscribe(component.beforeSocketConnect);
            }
        },

        bindings : defaultBindings,

        applyBindings : function() {
            ko.applyBindings(defaultBindings);
        },

        onMetrics : function(update) {
            this.bindings.metrics(update);
        },

        installRemoteTemplate : function(id, url) {
            jQuery.ajax({
                url:     url,
                async:   false,
                success: function(template) {
                    $("body").append("<script id=\"" + id + "\" type=\"text/html\">" + template + "<\/script>");
                },
                error : function() {
                    alert("Failed to install template " + id);
                }
            });
        },

        appendTemplateTo : function(templateId, node) {
            $('<div id="'+templateId+'-node" data-bind="template: { name: \''+templateId+'\' }"></div>').appendTo(node);
            var element = document.getElementById(templateId + "-node");
            ko.applyBindingsToNode(element, null, Dropwizard.bindings);
        }

    };

})();