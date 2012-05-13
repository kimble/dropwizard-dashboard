(function() {

    function InfoBindings() {
        var self = this;

        self.vminfo = ko.observable({
            name    : 'Unknown',
            version : 'Unknown',
            time    : new Date(0),
            uptime  : 0
        });

        self.readableVmUptime = ko.computed(function() {
            var uptime = self.vminfo().uptime;
            return moment.humanizeDuration(uptime * 1000);
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
            var jvm = metrics.jvm;
            var vm = jvm.vm;

            bindings.vminfo({
                name    : vm.name,
                version : vm.version,
                time    : jvm.current_time,
                uptime  : jvm.uptime
            });
        }
    });

})();