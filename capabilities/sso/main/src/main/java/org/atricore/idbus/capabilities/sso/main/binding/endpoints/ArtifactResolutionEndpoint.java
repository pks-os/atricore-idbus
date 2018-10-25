package org.atricore.idbus.capabilities.sso.main.binding.endpoints;

import org.apache.camel.Component;
import org.apache.camel.Producer;
import org.atricore.idbus.capabilities.sso.main.binding.producers.ArtifactResolutionProducer;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;

import java.util.Map;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class ArtifactResolutionEndpoint extends AbstractCamelEndpoint {

    public ArtifactResolutionEndpoint(String uri, Component component, Map parameters ) throws Exception {
        super(uri, component, parameters);
    }

    public Producer createProducer () throws Exception {
        return new ArtifactResolutionProducer( this );
    }
}
