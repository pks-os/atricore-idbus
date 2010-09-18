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

package com.atricore.idbus.console.modeling.main.view.deploy {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.form.IocFormMediator;
import com.atricore.idbus.console.main.view.progress.ProcessingMediator;
import com.atricore.idbus.console.modeling.main.ModelerMediator;
import com.atricore.idbus.console.modeling.main.controller.DeployIdentityApplianceCommand;

import flash.events.Event;
import flash.events.MouseEvent;

import mx.events.CloseEvent;

import org.puremvc.as3.interfaces.INotification;

public class DeployApplianceMediator extends IocFormMediator
{
    public static const RUN:String = "DeployApplianceMediator.RUN";

    private var _projectProxy:ProjectProxy;

    private var _processingStarted:Boolean;

    public function DeployApplianceMediator(name:String = null, viewComp:DeployApplianceView = null) {
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
            view.btnNext.removeEventListener(MouseEvent.CLICK, handleNextClick);
            if (view.parent != null) {
                view.parent.removeEventListener(CloseEvent.CLOSE, handleClose);
            }
        }

        super.setViewComponent(viewComponent);

        init();

    }

    private function init():void {
        view.selectedAppliance.text = _projectProxy.currentIdentityAppliance.idApplianceDefinition.name;
        view.btnNext.addEventListener(MouseEvent.CLICK, handleNextClick);
        view.parent.addEventListener(CloseEvent.CLOSE, handleClose);
    }

    override public function listNotificationInterests():Array {
        return [DeployIdentityApplianceCommand.SUCCESS,
            DeployIdentityApplianceCommand.FAILURE];
    }

    override public function handleNotification(notification:INotification):void {
        switch (notification.getName()) {
            case DeployIdentityApplianceCommand.SUCCESS:
                if (projectProxy.currentView == ModelerMediator.viewName) {
                    projectProxy.currentIdentityAppliance = projectProxy.commandResultIdentityAppliance;
                    sendNotification(ProcessingMediator.STOP);
                    sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
                    var msg:String = "Appliance has been successfully deployed.";
                    if (view.startAppliance.selected) {
                        msg = "Appliance has been successfully deployed and started.";
                    }
                    sendNotification(ApplicationFacade.SHOW_SUCCESS_MSG, msg);
                }
                break;
            case DeployIdentityApplianceCommand.FAILURE:
                if (projectProxy.currentView == ModelerMediator.viewName) {
                    sendNotification(ProcessingMediator.STOP);
                    sendNotification(ApplicationFacade.SHOW_ERROR_MSG,
                            "There was an error deploying appliance.");
                }
                break;
        }

    }

    private function handleNextClick(event:MouseEvent):void {
        _processingStarted = true;
        closeWindow();
        sendNotification(ProcessingMediator.START, "Deploying appliance ...");
        sendNotification(ApplicationFacade.DEPLOY_IDENTITY_APPLIANCE,
                [_projectProxy.currentIdentityAppliance.id.toString(), view.startAppliance.selected]);
    }

    private function closeWindow():void {
        view.parent.dispatchEvent(new CloseEvent(CloseEvent.CLOSE));
    }

    private function handleClose(event:Event):void {
    }

    protected function get view():DeployApplianceView
    {
        return viewComponent as DeployApplianceView;
    }
}
}