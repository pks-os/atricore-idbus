package org.atricore.idbus.capabilities.oauth2.common;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public interface OAuth2Constants {

    static final String SSO_SERVICE_BASE_URI="urn:org:atricore:idbus:sso:metadata";

    static final String OAUTH2_SERVICE_BASE_URI="urn:org:atricore:idbus:OAUTH:2.0:metadata";

    static final String SERVICE_TYPE = "urn:org:atricore:idbus:OAUTH:2.0";

    static final String TOKEN_SERVICE_TYPE = "{urn:org:atricore:idbus:OAUTH:2.0:metadata}TokenService";

    static final String OAUTH2_PROTOCOL_PKG = "org.atricore.idbus.common.oauth._2_0.protocol";

    static final String OAUTH2_PROTOCOL_NS = "urn:org:atricore:idbus:OAUTH:2.0:protocol";

    static final String OAUTH2_IDPALIAS_VAR = "oauth2_idp_alias";

    static final String OAUTH2_PASSIVE_VAR = "oauth2_passive";


}
