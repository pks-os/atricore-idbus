package com.atricore.idbus.console.liveservices.liveupdate.admin.command;

import com.atricore.idbus.console.liveservices.liveupdate.admin.service.LiveUpdateAdminService;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */

public abstract class LiveUpdateAdminCommandSupport {

    protected abstract Object doExecute(LiveUpdateAdminService svc) throws Exception ;
}
