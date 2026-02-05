package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import java.util.List;

public class PdpKlientTjeneste {

    private PdpKlientTjeneste() {
        // Skjuler default
    }

    public static PdpRequest lagPdpRequest(PdpKlient.System system, String orgnr, String ressurs) {
        // Resource
        var attributes = List.of(new PdpRequest.XacmlJsonAttributeExternal(
            "urn:altinn:resource",
            ressurs,
            null
        ), new PdpRequest.XacmlJsonAttributeExternal(
            "urn:altinn:organization:identifier-no",
            orgnr,
            null
        ));
        var resource = new PdpRequest.XacmlJsonCategoryExternal("r", attributes);

        return new PdpRequest(
            new PdpRequest.XacmlJsonRequestExternal(
                true,
                // Access subject
                List.of(
                    new PdpRequest.XacmlJsonCategoryExternal(
                        "s1",
                        List.of(
                            new PdpRequest.XacmlJsonAttributeExternal(
                                system.attributeId(),
                                system.id(),
                                null
                            )
                        )
                    )
                ),
                // Action
                List.of(
                    new PdpRequest.XacmlJsonCategoryExternal(
                        "a1",
                        List.of(
                            new PdpRequest.XacmlJsonAttributeExternal(
                                "urn:oasis:names:tc:xacml:1.0:action:action-id",
                                "access",
                                "http://www.w3.org/2001/XMLSchema#string"
                            )
                        )
                    )
                ),
                // Ressource
                List.of(resource),
                null
            )
        );
    }
}
