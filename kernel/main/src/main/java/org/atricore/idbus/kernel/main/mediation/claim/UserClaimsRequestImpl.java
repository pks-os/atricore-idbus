package org.atricore.idbus.kernel.main.mediation.claim;

import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;

/**
 * Created with IntelliJ IDEA.
 * User: sgonzalez
 * Date: 12/3/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserClaimsRequestImpl implements UserClaimsRequest {

    private String id;
    private String relayState;
    private Channel issuerChannel;
    private IdentityMediationEndpoint issuerEndpoint;
    private String lastErrorId;
    private String lastErrorMsg;

    public UserClaimsRequestImpl(String id, Channel issuerChannel, IdentityMediationEndpoint issuerEndpoint) {
        this.id = id;
        this.issuerChannel = issuerChannel;
        this.issuerEndpoint = issuerEndpoint;
    }

    public UserClaimsRequestImpl(String id, Channel issuerChannel, IdentityMediationEndpoint issuerEndpoint, String relayState) {
        this.id = id;
        this.issuerChannel = issuerChannel;
        this.issuerEndpoint = issuerEndpoint;
        this.relayState = relayState;
    }

    public String getId() {
        return id;
    }

    public String getRelayState() {
        return relayState;
    }

    public Channel getIssuerChannel() {
        return issuerChannel;
    }

    public IdentityMediationEndpoint getIssuerEndpoint() {
        return issuerEndpoint;
    }

    public String getLastErrorId() {
        return lastErrorId;
    }

    public String getLastErrorMsg() {
        return lastErrorMsg;
    }

    public void setLastErrorId(String lastErrorId) {
        this.lastErrorId = lastErrorId;
    }

    public void setLastErrorMsg(String lastErrorMsg) {
        this.lastErrorMsg = lastErrorMsg;
    }
}