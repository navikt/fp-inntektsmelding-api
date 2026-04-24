package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import no.nav.vedtak.server.rest.jackson.Jackson3Mapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;


public class Jackson3ApiFeature implements Feature {

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        // Register Jackson.
        if (!config.isRegistered(JacksonJsonProvider.class)) {
            context.register(JacksonJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        }
        context.register(Jackson3Mapper.class);
        context.register(DatabindExceptionMapper.class);
        context.register(StreamReadExceptionMapper.class);

        return true;
    }
}
