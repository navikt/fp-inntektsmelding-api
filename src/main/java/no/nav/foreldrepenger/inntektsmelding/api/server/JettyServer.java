package no.nav.foreldrepenger.inntektsmelding.api.server;

import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import no.nav.foreldrepenger.inntektsmelding.api.server.app.api.ApiConfig;
import no.nav.foreldrepenger.inntektsmelding.api.server.app.internal.InternalApiConfig;
import no.nav.foreldrepenger.konfig.Environment;

public class JettyServer {
    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private static final Environment ENV = Environment.current();
    protected static final String APPLICATION = "jakarta.ws.rs.Application";

    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    static void main(String[] args) throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    void bootStrap() throws Exception {
        System.setProperty("task.manager.runner.threads", "4");
        konfigurerLogging();
        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private static void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private void start() throws Exception {
            var server = new Server(getServerPort());
            LOG.info("Starter server");
            var context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

            // Sikkerhet
            context.setSecurityHandler(simpleConstraints());

            // Servlets
            registerDefaultServlet(context);
            registerServlet(context, 0, InternalApiConfig.API_URI, InternalApiConfig.class);
            registerServlet(context, 1, ApiConfig.API_URI, ApiConfig.class);

            // Enable Weld + CDI
            context.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
            context.addServletContainerInitializer(new CdiServletContainerInitializer());
            context.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

            server.setHandler(context);
            server.setStopAtShutdown(true);
            server.setStopTimeout(10000);
            server.start();

            LOG.info("Server startet på port: {}", getServerPort());
            server.join();
    }

    private static void registerDefaultServlet(ServletContextHandler context) {
        var defaultServlet = new ServletHolder(new DefaultServlet());
        context.addServlet(defaultServlet, "/*");
    }

    private static void registerServlet(ServletContextHandler context, int prioritet, String path, Class<?> appClass) {
        var servlet = new ServletHolder(new ServletContainer());
        servlet.setInitOrder(prioritet);
        servlet.setInitParameter(APPLICATION, appClass.getName());
        context.addServlet(servlet, path + "/*");
    }

    private static ConstraintSecurityHandler simpleConstraints() {
        var handler = new ConstraintSecurityHandler();
        // Slipp gjennom kall fra plattform til JaxRs. Foreløpig kun behov for GET
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, InternalApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ApiConfig.API_URI + "/*"));
        // Alt annet av paths og metoder forbudt - 403
        handler.addConstraintMapping(pathConstraint(Constraint.FORBIDDEN, "/*"));
        return handler;
    }

    private static ConstraintMapping pathConstraint(Constraint constraint, String path) {
        var mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path);
        return mapping;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
