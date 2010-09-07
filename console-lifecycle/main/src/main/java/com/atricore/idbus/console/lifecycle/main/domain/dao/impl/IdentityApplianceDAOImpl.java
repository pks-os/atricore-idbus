package com.atricore.idbus.console.lifecycle.main.domain.dao.impl;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityApplianceState;
import com.atricore.idbus.console.lifecycle.main.domain.dao.IdentityApplianceDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.Collection;

public class IdentityApplianceDAOImpl extends GenericDAOImpl<IdentityAppliance, Long>
        implements IdentityApplianceDAO {

    private static final Log logger = LogFactory.getLog(IdentityApplianceDAOImpl.class);

    public IdentityApplianceDAOImpl() {
        super();
    }

    public Collection<IdentityAppliance> list(boolean deployedOnly) {
        logger.debug("Listing all identity appliances");
        if (deployedOnly) {
            // TODO : Deployed
            PersistenceManager pm = getPersistenceManager();
            
            Query query = pm.newQuery("SELECT FROM com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance" +
                    //" WHERE this.idApplianceDeployment != null");
                    " WHERE this.state == '" + IdentityApplianceState.DEPLOYED + "'" +
                    "or this.state == '" + IdentityApplianceState.STARTED + "'");

            return (Collection<IdentityAppliance>) query.execute();
        } else {
            //TODO for now returning all appliances for list of projected
            return findAll();
        }
    }
}
