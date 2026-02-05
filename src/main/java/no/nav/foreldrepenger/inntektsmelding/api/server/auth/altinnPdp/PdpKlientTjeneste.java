package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import java.util.List;
import java.util.Set;

public class PdpKlientTjeneste {
    public PdpRequest lagPdpMultiRequest(PdpKlient.System system, Set<String> orgnrSet, Set<String> ressurser) {
        List<PdpRequest.XacmlJsonCategoryExternal> resources = kombiner(orgnrSet, ressurser)
            .stream()
            .map(pair -> new PdpRequest.XacmlJsonCategoryExternal(
                "r" + orgnrSet.hashCode() + ressurser.hashCode(), // or use a unique identifier if needed
                List.of(
                    new PdpRequest.XacmlJsonAttributeExternal(
                        "urn:altinn:resource",
                        pair.getSecond(),
                        null
                    ),
                    new PdpRequest.XacmlJsonAttributeExternal(
                        "urn:altinn:organization:identifier-no",
                        pair.getFirst(),
                        null
                    )
                )
            ))
            .toList();

        List<PdpRequest.RequestReferenceExternal> multiReqs = kombiner(orgnrSet, ressurser)
            .stream()
            .map(pair -> new PdpRequest.RequestReferenceExternal(
                List.of("s1", "a1", "r" + pair)
            ))
            .toList();

        return new PdpRequest(
            new PdpRequest.XacmlJsonRequestExternal(
                true,
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
                resources,
                new PdpRequest.MultiRequestsExternal(multiReqs)
            )
        );
    }

    private <T, R> List<Pair<T, R>> kombiner(Set<T> first, Set<R> second) {
        return first.stream()
            .flatMap(f -> second.stream()
                .map(s -> new Pair<>(f, s))
            )
            .toList();
    }

    static class Pair<T, R> {
        private final T first;
        private final R second;Pair(T first, R second) {
            this.first = first;
            this.second = second;
        }

        T getFirst() {
            return first;
        }

        R getSecond() {
            return second;
        }
    }

}
