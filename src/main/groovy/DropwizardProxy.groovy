import groovy.json.JsonBuilder
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.http.HttpClient
import org.vertx.groovy.core.http.HttpClientResponse
import org.vertx.groovy.core.http.HttpServer
import org.vertx.groovy.core.http.WebSocket

import java.util.concurrent.CopyOnWriteArraySet
import groovy.json.JsonSlurper

def metricsHost = "localhost"
def metricsPort = 8081
def dashboardPort = 9000

Vertx vx = vertx
HttpServer server = vx.createHttpServer()

DropwizardClient client = new DropwizardClient(vx: vx, host: metricsHost, port: metricsPort)
Listeners listeners = new Listeners()

vx.setPeriodic(1000) {

    // Don't poll status if nobody is listening
    if (listeners.hasAtLeastOneListener()) {
        client.withMetrics { metrics ->
            listeners.push("metrics", metrics)
        }
    }
}

server.websocketHandler { WebSocket socket ->
    listeners.addListener socket

    socket.endHandler {
        listeners.removeListener socket
    }
}


class DropwizardClient {

    Vertx vx
    HttpClient client
    def host
    def port
    def serverUnreachable

    DropwizardClient(params) {
        vx = params.vx
        host = params.host
        port = params.port

        newClient()
    }

    def withMetrics(Closure handler) {
        client.getNow("/metrics") { HttpClientResponse response ->
            if (serverUnreachable) {
                println "Connection restored"
                serverUnreachable = false
            }

            def buffer = new Buffer()

            // It should be possible to use bodyHandler, but that didn't work as adverted
            response.dataHandler { Buffer data ->
                buffer << data
            }

            response.endHandler {
                String data = buffer.toString() // Bug in the overloaded method allowing encoding to be set
                handler(new JsonSlurper().parseText(data))
            }
        }
    }

    def newClient() {
        if (host == null || host.isEmpty() || port == null || port < 1) {
            throw new IllegalArgumentException("Invalid host / port: ${host}:${port}")
        }

        client = vx.createHttpClient(host: host, port: port)
        client.exceptionHandler this.&exceptionHandler
    }

    def exceptionHandler(Exception ex) {
        System.err.println("Http client exception: ${ex.message}")

        if (ex instanceof java.net.ConnectException) {
            serverUnreachable = true
            client.close()
            newClient()
        } else {
            ex.printStackTrace(System.err)
        }
    }

}

class Listeners {

    Set<WebSocket> sockets = new CopyOnWriteArraySet<WebSocket>()

    void push(String topic, Map metrics) {
        JsonBuilder builder = new JsonBuilder()

        builder {
            namespace   topic
            payload     metrics
        }

        distribute builder.toString()
    }

    void distribute(String message) {
        sockets.each { socket ->
            socket << message
        }
    }

    void addListener(WebSocket socket) {
        sockets << socket
    }

    void removeListener(WebSocket socket) {
        sockets.remove(socket)
    }

    boolean hasAtLeastOneListener() {
        return !sockets.isEmpty()
    }

}


server.requestHandler { request ->
    if (request.uri == "/") {
        request.response.sendFile "web/index.html"
    }
    else if (request.uri.startsWith("/static")) {
        request.response.sendFile "web" + request.uri
    }
    else {
        request.response.sendFile "web/404.html"
    }
}

server.listen(dashboardPort)
println "Running at port $dashboardPort"
