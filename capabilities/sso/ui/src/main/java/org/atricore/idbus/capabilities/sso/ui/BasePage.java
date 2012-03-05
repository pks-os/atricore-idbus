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
package org.atricore.idbus.capabilities.sso.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.atricore.idbus.capabilities.sso.ui.spi.ApplicationRegistry;
import org.atricore.idbus.capabilities.sso.ui.spi.WebBrandingService;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationUnitRegistry;
import org.atricore.idbus.kernel.main.mediation.MessageQueueManager;
import org.ops4j.pax.wicket.api.PaxWicketBean;
import org.osgi.framework.BundleContext;

/**
 * Convenience base page for concrete SSO pages requiring a common layout and theme.
 *
 * @author <a href="mailto:gbrigandi@atricore.org">Gianluca Brigandi</a>
 */
public class BasePage extends WebPage {

    private static final Log logger = LogFactory.getLog(BasePage.class);

    @PaxWicketBean(name = "bundleContext", injectionSource = "spring")
    protected BundleContext bundleContext;

    @PaxWicketBean(name = "idsuRegistry", injectionSource = "spring")
    protected IdentityMediationUnitRegistry idsuRegistry;

    @PaxWicketBean(name = "artifactQueueManager", injectionSource = "spring")
    protected MessageQueueManager artifactQueueManager;

    @PaxWicketBean(name = "webAppConfigRegistry", injectionSource = "spring")
    protected ApplicationRegistry appConfigRegistry;

    @PaxWicketBean(name = "webBrandingService", injectionSource = "spring")
    protected WebBrandingService brandingService;

    private String variant;

    @SuppressWarnings("serial")
    public BasePage() {

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        String brandingId = getAppConfig().getBrandingId();

        WebBranding branding = brandingService.lookup(brandingId);
        if (branding != null) {

            if (logger.isTraceEnabled())
                logger.trace("Using 'variant' ["+branding.getSkin()+"] based on " + branding.getId());

            if (branding.getSkin() != null)
                setVariation(branding.getSkin());

            for (BrandingResource resource : branding.getResources()) {
                switch (resource.getType()) {
                    case CSS:
                        add(CSSPackageResource.getHeaderContribution(BasePage.class, resource.getPath()));
                        break;
                    case IMAGE:
                        add(new Image(resource.getId(), new ResourceReference(BasePage.class, resource.getPath())));
                        break;
                    case LABEL:
                        add(new Label(resource.getId(), resource.getValue()));
                        break;
                    default:
                        logger.error("Unknown resource type : " + resource.getType().name());
                        break;
                }
            }

        }

    }


    public void setVariation(String variation) {
        this.variant = variation;
    }

    @Override
    public String getVariation() {
        return variant;
    }


    public WebAppConfig getAppConfig() {
        WebAppConfig cfg = appConfigRegistry.lookupConfig(getApplication().getApplicationKey());
        if (cfg == null)
            logger.error("No configuration found for Wicket application " + getApplication().getApplicationKey());

        return cfg;
    }


}
