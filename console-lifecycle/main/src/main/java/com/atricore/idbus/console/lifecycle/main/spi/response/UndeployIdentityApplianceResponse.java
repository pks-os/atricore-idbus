package com.atricore.idbus.console.lifecycle.main.spi.response;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;

/**
 * Author: Dejan Maric
 */
public class UndeployIdentityApplianceResponse extends AbstractManagementResponse {

    private IdentityAppliance appliance;

    public UndeployIdentityApplianceResponse() {

    }

    public UndeployIdentityApplianceResponse(IdentityAppliance appliance) {
        this.appliance = appliance;
    }

    public IdentityAppliance getAppliance() {
        return appliance;
    }

    public void setAppliance(IdentityAppliance appliance) {
        this.appliance = appliance;
    }
}