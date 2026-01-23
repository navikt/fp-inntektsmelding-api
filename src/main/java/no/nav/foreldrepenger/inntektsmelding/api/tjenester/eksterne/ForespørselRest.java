package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ForespørselRest.BASE_PATH)
public class ForespørselRest {
    public static final String BASE_PATH = "/forespoersel";
    private static final String HENT_FORESPØRSEL = "/{uuid}";
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);

    public ForespørselRest() {
        // CDI
    }

    @GET
    @Path(HENT_FORESPØRSEL)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    // TODO legg på autorisering på altinn tilganger
    public Response hentForespørsel(@NotNull @Valid @PathParam("uuid") UUID forespørselUuid) {
        LOG.info("Innkomende kall på hent forespørsel {}", forespørselUuid);
        return Response.ok().build();
    }
}
