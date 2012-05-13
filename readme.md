Vertx experiment - Real time Dropwizard dashboard
=======================================

This was just a weekend experiment to refresh my Javascript knowledge and try out some technologies I've been looking at for some time.
Don't expect it to be of production quality or that I'll maintain it.

Dropwizard is a well instrumented and productive framework for building production ready restful web services. It exports a lot of statistics on a admin port. I thought it would be fun to create a proxy polling this endpoint and feeding the data to clients connected via web sockets. The proxy is implemented using the fairly new Vertx framework enabling highly concurrent polygot application development on the JVM.

While I was working on the client side of the dashboard I thought it would be fun to have a look at Knockout.js for data binding. I quite liked the declarative way it solves a lot of common problems related to data binding.


Screenshots
------------
![CSS dark](https://github.com/kimble/dropwizard-dashboard/raw/master/screenshots/dashboard.png)

Relevant technologies and libraries
-------------------------------------
http://vertx.io/
http://dropwizard.codahale.com/

http://knockoutjs.com/
http://momentjs.com/
http://smoothiecharts.org/
https://google-developers.appspot.com/chart/