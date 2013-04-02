package com.developerb.dd;

import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.impl.VerticleManager;

import java.io.File;
import java.net.URL;

/**
 * Fragile boot script making it possible to try out Dropwizard Dashboard
 * without installing or really knowing anything about Vertx.
 *
 * There has to be a more convenient way to do this..?
 *
 * @author Kim A. Betti <kim@developer-b.com>
 */
public class DropwizardDashboardStarter {

    private final VerticleManager manager;

    public static void main(String... args) throws Exception {
        DropwizardDashboardStarter dash = new DropwizardDashboardStarter();
        dash.runVertx();
        dash.join();
    }

    public DropwizardDashboardStarter() throws Exception {
        VertxInternal vertx = new DefaultVertx();
        manager = new VerticleManager(vertx, null);
    }

    private void runVertx() throws Exception {
        String main = "src/main/groovy/DropwizardProxy.groovy";
        boolean worker = false;
        JsonObject config = null;
        int instances = 1;
        String includes = null;
        File currentModDir = new File(".");

        URL[] urls = new URL[]{
                new File(".").toURI().toURL()
        };

        manager.deployVerticle(worker, main, config, urls, instances, currentModDir, includes, doneHandler);
    }

    private void join() throws InterruptedException {
        manager.block();
    }

    private final Handler<String> doneHandler = new Handler<String>() {

        @Override
        public void handle(String event) {
            System.out.println("I'm done?!");
        }

    };

}
