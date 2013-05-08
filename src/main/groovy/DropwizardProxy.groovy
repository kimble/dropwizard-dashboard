import groovy.json.JsonBuilder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.Base64
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.util.CharsetUtil
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

        client.onConnectionLost {
            println "Connection to remote Dropwizard server has been lost :("
            listeners.push("connectionLost", true)
        }
        
        client.onConnectionRestored {
            println "Connection restored"
            listeners.push("connectionRestored", true)
        }
    }
}

vx.setPeriodic(30000) {
    // Don't poll status if nobody is listening
    if (listeners.hasAtLeastOneListener()) {
        client.withHealthCheck { healthy, responseBody ->

            listeners.push(healthy ? "healthy" : "unhealthy", responseBody)
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

    Closure connectionRestoredHandler
    Closure connectionLostHandler

    DropwizardClient(params) {
        vx = params.vx
        host = params.host
        port = params.port

        newClient()
    }

    def onConnectionRestored(Closure handler) {
        connectionRestoredHandler = handler
    }

    def onConnectionLost(Closure handler) {
        connectionLostHandler = handler
    }

    static def basicAuthHeaders() {
        String authUser = "upsellsadmin"
        String authPass = "pha3zy5ry8mi"
        String authString = authUser + ":" + authPass;

        ChannelBuffer authChannelBuffer = ChannelBuffers.copiedBuffer(authString, CharsetUtil.UTF_8);
        ChannelBuffer encodedAuthChannelBuffer = Base64.encode(authChannelBuffer);
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaders.Names.AUTHORIZATION, "Basic " + encodedAuthChannelBuffer.toString(CharsetUtil.UTF_8));
        return headers;
    }

    def withMetrics(Closure handler) {

        client.getNow("/metrics", basicAuthHeaders()) { HttpClientResponse response ->
            connectionSucceeded()

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

    def withHealthCheck(Closure handler) {


        client.getNow("/healthcheck", basicAuthHeaders() ) { HttpClientResponse response ->
            connectionSucceeded()

            def healthy = response.statusCode == 200

            def buffer = new Buffer()

            response.dataHandler { Buffer data ->
                buffer << data
            }

            response.endHandler {
                handler(healthy, buffer.toString())
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
        if (ex instanceof java.net.ConnectException) {
            connectionFailed()
        } else {
            println("Http client exception: ${ex.message}")
            ex.printStackTrace(System.err)
        }
    }

    def connectionSucceeded() {
        if (serverUnreachable) {
            serverUnreachable = false
            connectionRestoredHandler()
        }
    }

    def connectionFailed() {
        if (!serverUnreachable) {
            connectionLostHandler()
        }

        serverUnreachable = true
        
        println("Attempting to reconnect...")
        client.close()
        newClient()
    }

}

class Listeners {

    Set<WebSocket> sockets = new CopyOnWriteArraySet<WebSocket>()

    void push(String topic, data) {
        JsonBuilder builder = new JsonBuilder()

        builder {
            namespace   topic
            payload     data
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
