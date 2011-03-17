package com.atricore.idbus.console.activation.main.impl;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
import com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType;
import com.atricore.idbus.console.activation._1_0.protocol.*;
import com.atricore.idbus.console.activation.main.spi.request.*;
import com.atricore.idbus.console.activation.main.spi.response.ActivateSamplesResponse;
import com.atricore.idbus.console.activation.main.spi.response.PlatformSupportedResponse;
import com.atricore.idbus.console.activation.main.spi.response.ActivateAgentResponse;
import com.atricore.idbus.console.activation.main.spi.response.ConfigureAgentResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class was generated by Apache CXF 2.2.2
 * Wed Mar 16 16:40:15 ART 2011
 * Generated source version: 2.2.2
 *
 */

// TODO : Authenticate requests using JAAS!
@javax.jws.WebService(
                      serviceName = "ActivationService",
                      portName = "soap",
                      targetNamespace = "urn:com:atricore:idbus:console:activation:1.0:wsdl",
                      endpointInterface = "com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType")

public class JOSSOActivationWServiceImpl implements ActivationPortType {

    private static final Log logger = LogFactory.getLog(JOSSOActivationWServiceImpl.class.getName());
    
    private JOSSOActivationServiceImpl activationService;
            

    public void afterPropertiesSet() throws Exception {
        System.setProperty("josso-gsh.home", System.getProperty("karaf.base") + "/josso");
    }

    /* (non-Javadoc)
     * @see com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType#configureAgent(ConfigureAgentRequestType  body )*
     */
    public ConfigureAgentResponseType configureAgent(ConfigureAgentRequestType wsRequest) {
        
        logger.debug("Executing operation configureAgent");
        
        try {
            ConfigureAgentRequest request = toRequest(wsRequest);
            ConfigureAgentResponse response = activationService.configureAgent(request);
            ConfigureAgentResponseType wsResponse = toWsResponse(response);
            
            return wsResponse;

        } catch (Exception e) {
            logger.error("Cannot process Activation request : " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType#activateAgent(ActivateAgentRequestType  request )*
     */
    public ActivateAgentResponseType activateAgent(ActivateAgentRequestType wsRequest) {
        logger.debug("Executing operation activateAgent");
        
        try {
            ActivateAgentRequest request = toRequest(wsRequest);
            ActivateAgentResponse response = activationService.activateAgent(request);
            ActivateAgentResponseType wsResponse = toWsResponse(response);
            
            return wsResponse;
        } catch (Exception e) {
            logger.error("Cannot process Activation request : " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType#activateSamples(ActivateSamplesRequestType  request )*
     */
    public ActivateSamplesResponseType activateSamples(ActivateSamplesRequestType wsRequest) {
        logger.debug("Executing operation activateSamples");
        
        try {
            ActivateSamplesRequest request = toRequest(wsRequest);
            
            ActivateSamplesResponse response = activationService.activateSamples(request);
            ActivateSamplesResponseType wsResponse = toWsResponse(response);
            
            return wsResponse;
        } catch (Exception e) {
            logger.error("Cannot process Activation request : " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.atricore.idbus.console.activation._1_0.wsdl.ActivationPortType#platformSupported(PlatformSupportedRequestType  request )*
     */
    public PlatformSupportedResponseType platformSupported(PlatformSupportedRequestType wsRequest) {
        logger.debug("Executing operation platformSupported");
        
        try {
            PlatformSupportedRequest request = toRequest(wsRequest);
            
            PlatformSupportedResponse response = activationService.isSupported(request);
            PlatformSupportedResponseType wsResponse = toWsResponse(response);
            
            return wsResponse;
        } catch (Exception e) {
            logger.error("Cannot process Activation request : " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    //---------------------------------------------------------------------------------------

    protected void copyAbstractRequest(AbstractActivationRequestType orig, AbstractActivationRequest dest) {
        dest.setForceInstall(orig.isForceInstall());
        dest.setIdpHostName(orig.getIdpHostName());
        dest.setIdpPort(orig.getIdpPort() + "");
        dest.setIdpType(orig.getIdpType());
        dest.setJbossInstallDir(orig.getJbossInstallDir());
        dest.setJbossInstance(orig.getJbossInstance());
        dest.setPassword(orig.getPassword());
        dest.setTarget(orig.getTarget());
        dest.setTargetPlatformId(orig.getTargetPlatformId());
        dest.setTomcatInstallDir(orig.getTomcatInstallDir());
        dest.setUser(orig.getUser());
        dest.setWeblogicDomain(orig.getWeblogicDomain());
    }

    protected ConfigureAgentRequest toRequest(ConfigureAgentRequestType wsRequest) {
        ConfigureAgentRequest request = new ConfigureAgentRequest();

        // Copy values
        copyAbstractRequest(wsRequest, request);
        request.setJossoAgentConfigUri(wsRequest.getJossoAgentConfigUri());
        request.setReplaceConfig(wsRequest.isReplaceConfig());


        for (AgentConfigResourceType wsResource : wsRequest.getAgentConfigResource()) {
            request.getReosurces().add(
                    new ConfigureAgentResource(wsResource.getName(), wsResource.getConfigResourceContent()));
        }


        return request;
    }

    protected ActivateAgentRequest toRequest(ActivateAgentRequestType wsRequest) {
        ActivateAgentRequest  request = new ActivateAgentRequest ();

        // Copy values
        copyAbstractRequest(wsRequest, request);
        return request;
    }

    protected ActivateSamplesRequest toRequest(ActivateSamplesRequestType wsRequest) {
        ActivateSamplesRequest  request = new ActivateSamplesRequest ();

        // Copy values
        copyAbstractRequest(wsRequest, request);
        return request;

    }

    protected PlatformSupportedRequest toRequest(PlatformSupportedRequestType wsRequest) {
        PlatformSupportedRequest request = new PlatformSupportedRequest();

        // Copy values
        request.setTargetPlatformId(wsRequest.getTargetPlatformId());
        return request;
    }

    protected ConfigureAgentResponseType toWsResponse(ConfigureAgentResponse response) {
        ConfigureAgentResponseType wsResponse = new ConfigureAgentResponseType ();
        return wsResponse;
    }

    protected ActivateAgentResponseType toWsResponse(ActivateAgentResponse response) {
        ActivateAgentResponseType wsResponse = new ActivateAgentResponseType ();
        return wsResponse;
    }

    protected ActivateSamplesResponseType toWsResponse(ActivateSamplesResponse response) {
        ActivateSamplesResponseType wsResponse = new ActivateSamplesResponseType ();
        return wsResponse;
    }

    protected PlatformSupportedResponseType toWsResponse(PlatformSupportedResponse response) {
        PlatformSupportedResponseType wsResponse = new PlatformSupportedResponseType ();
        wsResponse.setPlatformId(response.getPlatformId());
        wsResponse.setSupported(response.isSupported());
        return wsResponse;
    }

    public JOSSOActivationServiceImpl getActivationService() {
        return activationService;
    }

    public void setActivationService(JOSSOActivationServiceImpl activationService) {
        this.activationService = activationService;
    }
}
