package no.nav.foreldrepenger.inntektsmelding.api.server.app.api;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.AutentiseringFilter;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.inntektsmelding.api.server.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne.ForespørselRest;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    public static final String API_URI = "/v1";
    private static final Logger LOG = LoggerFactory.getLogger(ApiConfig.class);
    private static final Environment ENV = Environment.current();

    public ApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        // Sikkerhet
        register(AutentiseringFilter.class);
        registerOpenApi();
        // REST
        registerClasses(getApplicationClasses());

        registerExceptionMappers();
        register(JacksonJsonConfig.class);

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private void registerOpenApi() {
        var oas = new OpenAPI();
        var info = new Info().title(ENV.getNaisAppName())
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("API for inntektsmelding for foreldrepenger og svangerskapspenger");

        oas.info(info).addServersItem(new Server());
        var oasConfig = new SwaggerConfiguration().openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(getApplicationClasses().stream().map(Class::getName).collect(Collectors.toSet()));
        try {
            new GenericOpenApiContextBuilder<>().openApiConfiguration(oasConfig).buildContext(true).read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }

        register(OpenApiRest.class);
    }

    void registerExceptionMappers() {
        register(GeneralRestExceptionMapper.class);
        register(ConstraintViolationMapper.class);
        register(JsonMappingExceptionMapper.class);
        register(JsonParseExceptionMapper.class);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }
}
