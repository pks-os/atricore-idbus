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

package org.atricore.idbus.capabilities.samlr2.main.common.plans.actions;

import oasis.names.tc.saml._2_0.assertion.AssertionType;
import oasis.names.tc.saml._2_0.protocol.ResponseType;
import oasis.names.tc.saml._2_0.protocol.StatusResponseType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.main.common.AbstractSamlR2Mediator;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.SamlR2Signer;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.planning.IdentityArtifact;
import org.jbpm.graph.exe.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: SignResponseAction.java 1335 2009-06-24 16:34:38Z sgonzalez $
 */
public class SignResponseAction extends AbstractSamlR2Action {

    private static final Log logger = LogFactory.getLog(SignResponseAction.class);

    protected void doExecute(IdentityArtifact in, IdentityArtifact out, ExecutionContext executionContext) throws Exception {

        StatusResponseType response = (StatusResponseType) out.getContent();

        FederationChannel channel = (FederationChannel) executionContext.getContextInstance().getVariable(VAR_CHANNEL);
        AbstractSamlR2Mediator mediator = (AbstractSamlR2Mediator) channel.getIdentityMediator();
        SamlR2Signer signer = mediator.getSigner();

        // TODO : Add enable assertion signature property
        if (!mediator.isEnableSignature()) {
            logger.debug("Signature is disabled for " + channel.getName());
            return ;
        }

        // If the response has an assertion, remove the signature and re-sign it ... (we're discarding STS signature!)
        if (((ResponseType)response).getAssertionOrEncryptedAssertion().size() > 0) {


            List<Object> assertions = new ArrayList<Object>();
            for (Object o : ((ResponseType)response).getAssertionOrEncryptedAssertion()) {

                if (o instanceof AssertionType) {

                    AssertionType assertion = (AssertionType) o;

                    if (logger.isDebugEnabled())
                        logger.debug("Signing SAMLR2 Assertion: " + assertion.getID() + " in channel " + channel.getName());

                    // The Assertion is now eveloped within a SAML Response so we need to sign a second time within this context.
                    assertion.setSignature(null);

                    AssertionType signedAssertion = signer.sign(assertion);
                    assertions.add(signedAssertion);


                }

            }
            // Replace assertions
            ((ResponseType)response).getAssertionOrEncryptedAssertion().clear();
            ((ResponseType)response).getAssertionOrEncryptedAssertion().addAll(assertions);

        }

        if (logger.isDebugEnabled())
            logger.debug("Signing SAMLR2 Response: " + response.getID() + " in channel " + channel.getName());

        out.replaceContent(signer.sign(response));


    }
}