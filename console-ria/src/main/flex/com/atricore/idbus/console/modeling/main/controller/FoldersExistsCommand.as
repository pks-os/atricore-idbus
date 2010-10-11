package com.atricore.idbus.console.modeling.main.controller {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.service.ServiceRegistry;
import com.atricore.idbus.console.modeling.diagram.model.request.CheckFoldersRequest;
import com.atricore.idbus.console.modeling.diagram.model.response.CheckFoldersResponse;
import com.atricore.idbus.console.services.spi.request.CheckFoldersExistenceRequest;
import com.atricore.idbus.console.services.spi.response.CheckFoldersExistenceResponse;

import mx.rpc.Fault;
import mx.rpc.IResponder;
import mx.rpc.events.FaultEvent;
import mx.rpc.remoting.mxml.RemoteObject;

import org.puremvc.as3.interfaces.INotification;
import org.springextensions.actionscript.puremvc.patterns.command.IocSimpleCommand;

public class FoldersExistsCommand extends IocSimpleCommand implements IResponder {

    public static const FOLDERS_EXISTENCE_CHECKED : String = "FoldersExistsCommand.FOLDERS_EXISTENCE_CHECKED";
    public static const FAILURE : String = "FoldersExistsCommand.FAILURE";

    private var _registry:ServiceRegistry;

    public function get registry():ServiceRegistry {
        return _registry;
    }

    public function set registry(value:ServiceRegistry):void {
        _registry = value;
    }

    override public function execute(notification:INotification):void {
//        var folder:String = notification.getBody() as String;
        var cif:CheckFoldersRequest = notification.getBody() as CheckFoldersRequest;

        var service:RemoteObject = registry.getRemoteObjectService(ApplicationFacade.IDENTITY_APPLIANCE_MANAGEMENT_SERVICE);
        var req:CheckFoldersExistenceRequest = new CheckFoldersExistenceRequest();
        req.folders = cif.folders;
        req.environmentName = cif.environmentName;
        var call:Object = service.checkFoldersExistence(req);
        call.addResponder(this);
    }

    public function fault(info:Object):void {
        var fault:Fault = (info as FaultEvent).fault;
        var msg:String = fault.faultString.substring((fault.faultString.indexOf('.') + 1), fault.faultString.length);
        trace(msg);
        sendNotification(FAILURE, msg);
    }

    public function result(data:Object):void {
        var resp:CheckFoldersExistenceResponse = data.result as CheckFoldersExistenceResponse;
        var checkResp:CheckFoldersResponse = new CheckFoldersResponse();
        checkResp.invalidFolders = resp.invalidFolders;
        checkResp.environmentName = resp.environmentName;
        sendNotification(FOLDERS_EXISTENCE_CHECKED, checkResp);
    }
}
}