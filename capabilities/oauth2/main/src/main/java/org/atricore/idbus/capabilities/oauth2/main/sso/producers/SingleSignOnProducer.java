package org.atricore.idbus.capabilities.oauth2.main.sso.producers;

import org.apache.camel.Endpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.oauth2.common.OAuth2Constants;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2AuthnContext;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2BPMediator;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2Exception;
import org.atricore.idbus.capabilities.oauth2.main.ResourceServer;
import org.atricore.idbus.capabilities.sso.main.select.spi.EntitySelectorConstants;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sso.support.metadata.SSOService;
import org.atricore.idbus.common.sso._1_0.protocol.RequestAttributeType;
import org.atricore.idbus.common.sso._1_0.protocol.SPInitiatedAuthnRequestType;
import org.atricore.idbus.kernel.main.authn.util.CipherUtil;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrust;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustMemberDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelProducer;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedLocalProvider;
import org.atricore.idbus.kernel.main.mediation.provider.Provider;
import org.atricore.idbus.kernel.main.mediation.provider.ServiceProvider;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;

import java.net.URLDecoder;

/**
 * This is useful when accessing an OAuth2 application directly from Front-Channel (NON-SOA)
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class SingleSignOnProducer extends AbstractCamelProducer<CamelMediationExchange> {

    private static final Log logger = LogFactory.getLog(SingleSignOnProducer.class);

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    public SingleSignOnProducer(Endpoint endpoint) {
        super(endpoint);
    }

    /**
     * Acts as SP initiated SSO service
     */
    @Override
    protected void doProcess(CamelMediationExchange exchange) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();

        BindingChannel bChannel = (BindingChannel) channel;
        OAuth2BPMediator mediator = (OAuth2BPMediator) bChannel.getIdentityMediator();
        ResourceServer rServer = mediator.getResourceServer();
        // TODO : Implement a request object instead of reading vars from the state
        String idpAlias = null;
        String idpAliasB64 = in.getMessage().getState().getTransientVariable(OAuth2Constants.OAUTH2_IDPALIAS_VAR);

        // Passive option
        Boolean passive = null;
        String passiveStr = in.getMessage().getState().getTransientVariable(OAuth2Constants.OAUTH2_PASSIVE_VAR);
        if (passiveStr != null) {
            passive = Boolean.parseBoolean(passiveStr);
        }

        if (logger.isDebugEnabled())
            logger.debug("Passive login : " + passiveStr);

        OAuth2AuthnContext authnCtx = (OAuth2AuthnContext) in.getMessage().getState().getLocalVariable("urn:org:atricore:idbus:capabilities:oauth2:authnCtx");

        // Decode IDP Alias, if any
        if (idpAliasB64 != null) {
            idpAlias = URLDecoder.decode(new String(CipherUtil.decodeBase64(idpAliasB64)), "UTF-8");

            if (logger.isDebugEnabled())
                logger.debug("Using received idp alias " + idpAlias);
        }

        if (idpAlias == null) {
            idpAlias = authnCtx != null ? authnCtx.getIdpAlias() : null;

            if (logger.isDebugEnabled())
                logger.debug("Using previous idp alias " + idpAlias);
        }


        if (logger.isDebugEnabled())
            logger.debug("Starting OAuth2 SSO, resource server [" + rServer.getName() + "] " +
                    "resource ["+rServer.getResourceLocation()+"] idpAlias ["+idpAlias+"]");

        BindingChannel spChannel = resolveSpBindingChannel(bChannel);

        EndpointDescriptor destination = resolveSPInitiatedSSOEndpointDescriptor(exchange, spChannel);

        // Create SP AuthnRequest
        // TODO : Support on_error ?
        SPInitiatedAuthnRequestType request = buildAuthnRequest(exchange, idpAlias, passive);

        // Create context information
        authnCtx = new OAuth2AuthnContext();
        authnCtx.setIdpAlias(idpAlias);
        authnCtx.setAuthnRequest(request);

        // Store state
        in.getMessage().getState().setLocalVariable("urn:org:atricore:idbus:capabilities:josso:authnCtx", authnCtx);

        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
        out.setMessage(new MediationMessageImpl(request.getID(),
                request,
                "SSOAuthnRequest",
                null,
                destination,
                in.getMessage().getState()));

        exchange.setOut(out);

    }

    /**
     * @return
     */
    protected SPInitiatedAuthnRequestType buildAuthnRequest(CamelMediationExchange exchange, String idpAlias, Boolean passive) {

        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();

        SPInitiatedAuthnRequestType req = new SPInitiatedAuthnRequestType();
        req.setID(uuidGenerator.generateId());
        req.setPassive(passive);

        RequestAttributeType idpAliasAttr = new RequestAttributeType();
        idpAliasAttr.setName(EntitySelectorConstants.REQUESTED_IDP_ALIAS_ATTR);
        idpAliasAttr.setValue(idpAlias);
        req.getRequestAttribute().add(idpAliasAttr);

        // Send all transient vars to SP
        for (String tvarName : in.getMessage().getState().getTransientVarNames()) {
            RequestAttributeType a = new RequestAttributeType();
            a.setName(tvarName);
            a.setValue(in.getMessage().getState().getTransientVariable(tvarName));
        }

        return req;
    }


    protected EndpointDescriptor resolveSPInitiatedSSOEndpointDescriptor(CamelMediationExchange exchange,
                                                                         BindingChannel sp) throws OAuth2Exception {

        try {

            if (logger.isDebugEnabled())
                logger.debug("Looking for " + SSOService.SPInitiatedSingleSignOnService.toString());

            for (IdentityMediationEndpoint endpoint : sp.getEndpoints()) {

                if (logger.isDebugEnabled())
                    logger.debug("Processing endpoint : " + endpoint.getType() + "["+endpoint.getBinding()+"]");

                if (endpoint.getType().equals(SSOService.SPInitiatedSingleSignOnService.toString())) {

                    if (endpoint.getBinding().equals(SSOBinding.SSO_ARTIFACT.getValue())) {
                        // This is the endpoint we're looking for
                        return  sp.getIdentityMediator().resolveEndpoint(sp, endpoint);
                    }
                }
            }
        } catch (IdentityMediationException e) {
            throw new OAuth2Exception(e);
        }

        throw new OAuth2Exception("No SP endpoint found for SP Initiated SSO using SSO Artifact binding");
    }

    protected BindingChannel resolveSpBindingChannel(BindingChannel bChannel) throws OAuth2Exception {

        String spAlias = ((OAuth2BPMediator)bChannel.getIdentityMediator()).getSpAlias();

        CircleOfTrust cot = getFederatedProvider().getCircleOfTrust();

        for (Provider p : cot.getProviders()) {

            if (p instanceof ServiceProvider) {

                ServiceProvider sp = (ServiceProvider)p;
                for (CircleOfTrustMemberDescriptor m : sp.getMembers()) {
                    if (m.getAlias().equals(spAlias)) {
                        if (logger.isDebugEnabled())
                            logger.debug("Found Service Provider " + p.getName() + " for alias " + spAlias);

                        return ((ServiceProvider) p).getBindingChannel();

                    }
                }

            }
        }

        if (logger.isDebugEnabled())
            logger.debug("No Service Provider found for alias " + spAlias);

        return null;

    }

    protected FederatedLocalProvider getFederatedProvider() {
        if (channel instanceof FederationChannel) {
            return ((FederationChannel) channel).getFederatedProvider();
        } else if (channel instanceof BindingChannel) {
            return ((BindingChannel) channel).getFederatedProvider();
        } else if (channel instanceof ClaimChannel) {
            return ((ClaimChannel) channel).getFederatedProvider();
        } else {
            throw new IllegalStateException("Configured channel does not support Federated Provider : " + channel);
        }
    }


}