package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PdpRequest(
    @JsonProperty("request")
    XacmlJsonRequestExternal request
) {

    public record XacmlJsonRequestExternal(
        @JsonProperty("returnPolicyIdList")
        Boolean returnPolicyIdList,

        @JsonProperty("accessSubject")
        List<XacmlJsonCategoryExternal> accessSubject,

        @JsonProperty("action")
        List<XacmlJsonCategoryExternal> action,

        @JsonProperty("resource")
        List<XacmlJsonCategoryExternal> resource,

        @JsonProperty("multiRequests")
        MultiRequestsExternal multiRequests
    ) {}

    public record XacmlJsonCategoryExternal(
        @JsonProperty("id")
        String id,

        @JsonProperty("attribute")
        List<XacmlJsonAttributeExternal> attribute
    ) {}

    public record XacmlJsonAttributeExternal(
        @JsonProperty("attributeId")
        String attributeId,

        @JsonProperty("value")
        String value,

        @JsonProperty("dataType")
        String dataType
    ) {}

    public record MultiRequestsExternal(
        @JsonProperty("requestReference")
        List<RequestReferenceExternal> requestReference
    ) {}

    public record RequestReferenceExternal(
        @JsonProperty("referenceId")
        List<String> referenceId
    ) {}
}
