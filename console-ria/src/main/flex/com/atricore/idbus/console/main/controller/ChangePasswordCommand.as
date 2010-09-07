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

package com.atricore.idbus.console.main.controller
{
import com.atricore.idbus.console.main.model.SecureContextProxy;
import com.atricore.idbus.console.main.service.ServiceRegistry;

import mx.rpc.IResponder;
import mx.rpc.events.FaultEvent;

import org.puremvc.as3.interfaces.INotification;
import org.springextensions.actionscript.puremvc.patterns.command.IocSimpleCommand;

public class ChangePasswordCommand extends IocSimpleCommand implements IResponder
{
    public static const SUCCESS:String = "com.atricore.idbus.console.main.controller.ChangePasswordCommand.SUCCESS";
    public static const FAILURE:String = "com.atricore.idbus.console.main.controller.ChangePasswordCommand.FAILURE";

    private var _registry:ServiceRegistry;
    private var _secureContext:SecureContextProxy;


    public function get registry():ServiceRegistry {
        return _registry;
    }

    public function set registry(value:ServiceRegistry):void {
        _registry = value;
    }

    public function get secureContext():SecureContextProxy {
        return _secureContext;
    }

    public function set secureContext(value:SecureContextProxy):void {
        _secureContext = value;
    }

    override public function execute(notification:INotification):void {

        sendNotification(SUCCESS);

        /** TODO: invoke change password operation in profile  management service 
        */
    }

    public function result(data:Object):void {
        sendNotification(SUCCESS);
    }

    public function fault(info:Object):void {
        trace((info as FaultEvent).fault.message);
        sendNotification(FAILURE);
    }

}
}