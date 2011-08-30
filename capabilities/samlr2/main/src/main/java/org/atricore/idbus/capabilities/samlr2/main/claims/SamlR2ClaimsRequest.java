/*
 * Atricore IDBus
 *
 * Copyright (c) 2009, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.atricore.idbus.capabilities.samlr2.main.claims;

import oasis.names.tc.saml._2_0.protocol.RequestedAuthnContextType;
import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimsRequestImpl;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;

/**
 * TODO : Rename to SSOClaimsRequest
 *
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 *
 * @version $Id$
 */
public class SamlR2ClaimsRequest extends ClaimsRequestImpl {

    private RequestedAuthnContextType requestedAuthnCtxClass;

    private String targetRelayState;

    private String spAlias;

    public SamlR2ClaimsRequest(String id, Channel issuer, IdentityMediationEndpoint endpoint, ClaimChannel provider, String relayState) {
        super(id, issuer, endpoint, provider, relayState);
    }

    public String getTargetRelayState() {
        return targetRelayState;
    }

    public void setTargetRelayState(String targetRelayState) {
        this.targetRelayState = targetRelayState;
    }

    public String getSpAlias() {
        return spAlias;
    }

    public void setSpAlias(String spAlias) {
        this.spAlias = spAlias;
    }

    public RequestedAuthnContextType getRequestedAuthnCtxClass() {
        return requestedAuthnCtxClass;
    }

    public void setRequestedAuthnCtxClass(RequestedAuthnContextType requestedAuthnCtxClass) {
        this.requestedAuthnCtxClass = requestedAuthnCtxClass;
    }

    @Override
    public String toString() {
        return super.toString() +
                "[requestedAuthnCtxClass="+ requestedAuthnCtxClass +"]";
    }
}
