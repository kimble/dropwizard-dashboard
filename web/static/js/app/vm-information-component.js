(function() {

    function InfoBindings() {
        var self = this;

        self.vminfo = ko.observable({
            name    : 'Unknown',
            time    : new Date(0),
            uptime  : 0
        });

        self.readableVmUptime = ko.computed(function() {
            var uptime = self.vminfo().uptime;
            return moment.humanizeDuration(uptime);
        });

        self.readableServerTime = ko.computed(function() {
            var serverTime = self.vminfo().time;
            return moment(serverTime).format("Do MMMM YYYY HH:mm:ss");
        });
    }

    var bindings = new InfoBindings;

    Dropwizard.registerComponent({
        bindings : bindings,

        onMetrics : function(metrics) {
            var g = metrics.gauges;

            bindings.vminfo({
                name    : g['jvm.attribute.vendor'].value,
                time    : new Date(),
                uptime  : g['jvm.attribute.uptime'].value
            });
        }
    });

})();