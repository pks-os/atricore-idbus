package com.atricore.idbus.console.lifecycle.main.spi.request;

/**
 * Author: Dejan Maric
 */
public class LookupIdentityApplianceByIdRequest extends AbstractManagementRequest {

    private String identityApplianceId;

    public String getIdentityApplianceId() {
        return identityApplianceId;
    }

    public void setIdentityApplianceId(String identityApplianceId) {
        this.identityApplianceId = identityApplianceId;
    }

}
