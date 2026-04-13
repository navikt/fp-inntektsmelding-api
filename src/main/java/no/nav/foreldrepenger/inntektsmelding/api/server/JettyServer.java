package no.nav.foreldrepenger.inntektsmelding.api.server;

import static no.nav.vedtak.mapper.json.DefaultJsonMapper.toJson;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import no.nav.foreldrepenger.inntektsmelding.api.server.app.api.ApiConfig;
import no.nav.foreldrepenger.inntektsmelding.api.server.app.internal.InternalApiConfig;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.log.mdc.MDCOperations;

public class JettyServer {
    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private static final Environment ENV = Environment.current();
    protected static final String APPLICATION = "jakarta.ws.rs.Application";

    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    static void main() throws Exception {
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
            context.setErrorHandler(new JsonErrorHandler());

            // Statiske ressurser (Swagger UI)
            var factory = ResourceFactory.of(context);
            context.setBaseResource(ResourceFactory.combine(
                factory.newClassLoaderResource("/META-INF/resources/webjars/", false),
                factory.newClassLoaderResource("/web", false)));

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
        var defaultServlet = new ServletHolder(new ResourceServlet());
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
        // Swagger UI statiske ressurser
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, "/swagger/*"));
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, "/swagger-ui/*"));
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

    private static class JsonErrorHandler extends ErrorHandler {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            // Set the content type to application/json
            response.getHeaders().put("Content-Type", "application/json;charset=utf-8");

            int code = response.getStatus();
            var message = HttpStatus.getMessage(code);
            var errorResponse = new ErrorResponse("[%s] %s".formatted(code, message), MDCOperations.generateCallId());

            // Write the JSON response
            response.write(true, ByteBuffer.wrap(toJson(errorResponse).getBytes(StandardCharsets.UTF_8)), callback);

            // Return true to indicate that the request has been handled
            return true;
        }
    }
}
