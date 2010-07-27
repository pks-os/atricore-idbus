/*
 * Atricore Console
 *
 * Copyright 2009-2010, Atricore Inc.
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

package com.atricore.idbus.console.modeling.diagram.view.idp {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.form.FormUtility;
import com.atricore.idbus.console.main.view.form.IocFormMediator;
import com.atricore.idbus.console.services.dto.BindingDTO;
import com.atricore.idbus.console.services.dto.IdentityProviderDTO;
import com.atricore.idbus.console.services.dto.LocationDTO;
import com.atricore.idbus.console.services.dto.ProfileDTO;
import com.atricore.idbus.console.services.dto.ServiceProviderChannelDTO;

import flash.events.MouseEvent;

import mx.collections.ArrayCollection;
import mx.events.CloseEvent;

import org.puremvc.as3.interfaces.INotification;

public class IdentityProviderCreateMediator extends IocFormMediator {

    private var _projectProxy:ProjectProxy;
    private var _newIdentityProvider:IdentityProviderDTO;

    public function IdentityProviderCreateMediator(name:String = null, viewComp:IdentityProviderCreateForm = null) {
        super(name, viewComp);

    }

    public function get projectProxy():ProjectProxy {
        return _projectProxy;
    }

    public function set projectProxy(value:ProjectProxy):void {
        _projectProxy = value;
    }
    
    override public function setViewComponent(viewComponent:Object):void {
        if (getViewComponent() != null) {
            view.btnOk.removeEventListener(MouseEvent.CLICK, handleIdentityProviderSave);
            view.btnCancel.removeEventListener(MouseEvent.CLICK, handleCancel);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {
        view.btnOk.addEventListener(MouseEvent.CLICK, handleIdentityProviderSave);
        view.btnCancel.addEventListener(MouseEvent.CLICK, handleCancel);
    }

    private function resetForm():void {
        view.identityProviderName.text = "";
        view.identityProvDescription.text = "";
        view.idpLocationProtocol.selectedIndex = 0;
        view.idpLocationDomain.text = "";
        view.idpLocationPort.text = "";
        view.idpLocationContext.text = "";
        view.idpLocationPath.text = "";
        view.signAuthAssertionCheck.selected = false;
        view.encryptAuthAssertionCheck.selected = false;
        view.samlBindingHttpPostCheck.selected = false;
        view.samlBindingArtifactCheck.selected = false;
        view.samlBindingHttpRedirectCheck.selected = false;
        view.samlBindingSoapCheck.selected = false;
        view.samlProfileSSOCheck.selected = false;
        view.samlProfileSLOCheck.selected = false;

        FormUtility.clearValidationErrors(_validators);
    }

    override public function bindModel():void {

        var identityProvider:IdentityProviderDTO = new IdentityProviderDTO();

        identityProvider.name = view.identityProviderName.text;
        identityProvider.description = view.identityProvDescription.text;

        var loc:LocationDTO = new LocationDTO();
        loc.protocol = view.idpLocationProtocol.selectedLabel;
        loc.host = view.idpLocationDomain.text;
        loc.port = parseInt(view.idpLocationPort.text);
        loc.context = view.idpLocationContext.text;
        loc.uri = view.idpLocationPath.text;
        identityProvider.location = loc;

        identityProvider.signAuthenticationAssertions = view.signAuthAssertionCheck.selected;
        identityProvider.encryptAuthenticationAssertions = view.encryptAuthAssertionCheck.selected;

        var spChannel:ServiceProviderChannelDTO = new ServiceProviderChannelDTO();

        spChannel.name = identityProvider.name + " to sp default channel";

        var spChannelLoc:LocationDTO = new LocationDTO();
        spChannelLoc.protocol = view.idpLocationProtocol.selectedLabel;
        spChannelLoc.host = view.idpLocationDomain.text;
        spChannelLoc.port = parseInt(view.idpLocationPort.text);
        spChannelLoc.context = view.idpLocationContext.text;
        spChannelLoc.uri = view.idpLocationPath.text + "/SAML2";

        spChannel.location = spChannelLoc;

        spChannel.activeBindings = new ArrayCollection();
        if (view.samlBindingHttpPostCheck.selected) {
            spChannel.activeBindings.addItem(BindingDTO.SAMLR2_HTTP_POST);
        }
        if (view.samlBindingArtifactCheck.selected) {
            spChannel.activeBindings.addItem(BindingDTO.SAMLR2_ARTIFACT);
        }
        if (view.samlBindingHttpRedirectCheck.selected) {
            spChannel.activeBindings.addItem(BindingDTO.SAMLR2_HTTP_REDIRECT);
        }
        if (view.samlBindingSoapCheck.selected) {
            spChannel.activeBindings.addItem(BindingDTO.SAMLR2_SOAP);
        }

        spChannel.activeProfiles = new ArrayCollection();
        if (view.samlProfileSSOCheck.selected) {
            spChannel.activeProfiles.addItem(ProfileDTO.SSO);
        }
        if (view.samlProfileSLOCheck.selected) {
            spChannel.activeProfiles.addItem(ProfileDTO.SSO_SLO);
        }

        // TODO save remaining fields to defaultChannel, calling appropriate lookup methods
        //userInformationLookup
        //authenticationContract
        //authenticationMechanism
        //authenticationAssertionEmissionPolicy
        identityProvider.defaultChannel = spChannel;

        _newIdentityProvider = identityProvider;
    }

    private function handleIdentityProviderSave(event:MouseEvent):void {
        if (validate(true)) {
            bindModel();
            _projectProxy.currentIdentityAppliance.idApplianceDefinition.providers.addItem(_newIdentityProvider);
            _projectProxy.currentIdentityApplianceElement = _newIdentityProvider;
            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_CREATION_COMPLETE);
            sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
            sendNotification(ApplicationFacade.IDENTITY_APPLIANCE_CHANGED);
            closeWindow();
        }
        else {
            event.stopImmediatePropagation();
        }
    }

    private function handleCancel(event:MouseEvent):void {
        closeWindow();
    }

    private function closeWindow():void {
        resetForm();
        view.parent.dispatchEvent(new CloseEvent(CloseEvent.CLOSE));
    }

    protected function get view():IdentityProviderCreateForm
    {
        return viewComponent as IdentityProviderCreateForm;
    }


    override public function registerValidators():void {
        _validators.push(view.nameValidator);
        _validators.push(view.portValidator);
        _validators.push(view.domainValidator);
        _validators.push(view.pathValidator);
    }


    override public function listNotificationInterests():Array {
        return super.listNotificationInterests();
    }

    override public function handleNotification(notification:INotification):void {

        super.handleNotification(notification);


    }
}
}