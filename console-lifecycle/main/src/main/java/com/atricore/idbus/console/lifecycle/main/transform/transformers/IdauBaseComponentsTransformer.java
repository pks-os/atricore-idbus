/*
 * Copyright (c) 2010., Atricore Inc.
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

package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.AuthenticationService;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.IdentityApplianceDefinition;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.Location;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.WindowsIntegratedAuthentication;
import com.atricore.idbus.console.lifecycle.main.transform.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.osgi.Reference;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.osgi.Service;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.Application;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.ContextParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.binding.SamlR2BindingFactory;
import org.atricore.idbus.capabilities.sso.main.binding.logging.SSOLogMessageBuilder;
import org.atricore.idbus.capabilities.sso.main.select.SSOEntitySelectorMediator;
import org.atricore.idbus.capabilities.sso.main.select.internal.EntitySelectorManagerImpl;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustImpl;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustManagerImpl;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.atricore.idbus.kernel.main.mediation.provider.EntitySelectorProviderImpl;
import org.atricore.idbus.kernel.main.mediation.select.SelectorChannelImpl;
import org.atricore.idbus.kernel.planning.IdentityPlanRegistryImpl;
import org.atricore.idbus.kernel.main.mediation.camel.OsgiCamelIdentityMediationUnitContainerImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;

import java.util.*;
import java.util.List;
import java.util.Set;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.setPropertyValue;

/**
 * Creates basic components configuration for an Identity Appliance Unit.
 * It assumes that the Identity Appliance has only one unit
 *
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class IdauBaseComponentsTransformer extends AbstractTransformer {

    private static Log logger = LogFactory.getLog(IdauBaseComponentsTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityApplianceDefinition;
    }

    @Override
    public void before(TransformEvent event) {

        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();
        IdentityApplianceDefinition ida = (IdentityApplianceDefinition) event.getData();
        IdApplianceTransformationContext context = event.getContext();
        IdProjectModule module = context.getCurrentModule();

        Date now = new Date();
        Beans idauBeans = newBeans(ida.getName() + " : IdAU Configuration generated by Atricore Console on " + now.toGMTString());
        Beans idauBeansOsgi = newBeans(ida.getName() + " : IdAU OSGi Configuration generated by Atricore Console on " + now.toGMTString());

        // Identity Appliance Unit beans ... (for now only one IDAU is generated!)
        event.getContext().put("beans", idauBeans);
        event.getContext().put("beansOsgi", idauBeansOsgi);

        if (logger.isDebugEnabled())
            logger.debug("Defining Identity Appliance Unit base components based on " + ida.getName());

        // Create IDAU Basic beans:
        String idauName = ida.getName();

        // Set the IdAU path/package name
        String idauPath = event.getContext().get("idaBasePath") + "/idau/";
        String idauPackage = event.getContext().get("idaBasePackage") + ".idau";
        event.getContext().put("idauPath", idauPath);
        event.getContext().put("idauPackage", idauPackage);

        // Set the module package and base path based on the previous properties
        module.setPath(idauPath);
        module.setPackage(idauPackage);

        // -------------------------------------------------------
        // Define Identity Mediation Unit bean
        // -------------------------------------------------------
        Bean idMediationUnit = newBean(idauBeans, idauName + "-mediation-unit", OsgiIdentityMediationUnit.class.getName());
        idMediationUnit.setDependsOn(idauName + "-cot," + idauName + "-cot-manager,bpms-manager,cxf");

        // Properties
        setPropertyValue(idMediationUnit, "name", idMediationUnit.getName());
        setPropertyRef(idMediationUnit, "container", idauName + "-container");

        // -------------------------------------------------------
        // Define Mediation Unit Container bean
        // -------------------------------------------------------
        Bean idContainer = newBean(idauBeans, idauName + "-container", OsgiCamelIdentityMediationUnitContainerImpl.class.getName());

        // Properties, the Identity Mediation unit container name is the Identity Appliance Unit name:
        setPropertyValue(idContainer, "name", idauName);
        setPropertyRef(idContainer, "cxfBus", "cxf");

        // ----------------------------------------
        // Kernel CFG Properties
        // ----------------------------------------
        Reference idbusCfg = new Reference();
        idbusCfg.setId("idbus-config");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(idbusCfg);
        idbusCfg.setInterface("org.atricore.idbus.kernel.main.util.ConfigurationContext");

        // -------------------------------------------------------
        // Define Circle Of Trust bean
        // -------------------------------------------------------
        Bean cot = newBean(idauBeans, idauName + "-cot", CircleOfTrustImpl.class.getName());

        // Properties
        setPropertyValue(cot, "name", cot.getName());

        // -------------------------------------------------------
        // Define Circle Of Trust Manager bean
        // -------------------------------------------------------

        Bean cotMgr = newBean(idauBeans, idauName + "-cot-manager", CircleOfTrustManagerImpl.class.getName());

        // Properties
        setPropertyRef(cotMgr, "cot", cot.getName());

        // -------------------------------------------------------
        // Define Session Event Manager
        // -------------------------------------------------------
        Bean sessionEventManager = newBean(idauBeans, "session-event-manager",
                "org.atricore.idbus.kernel.main.session.SSOSessionEventManager");
        sessionEventManager.setFactoryMethod("getInstance");

        // -------------------------------------------------------
        // Define Cache Manager bean
        // -------------------------------------------------------
        Bean cacheManager = newBean(idauBeans, idauName + "-cache-manager",
                "net.sf.ehcache.CacheManager");
        cacheManager.setFactoryBean("cache-manager-factory");
        cacheManager.setFactoryMethod("getCacheManager");

        // -------------------------------------------------------
        // Define State Manager bean
        // -------------------------------------------------------
        Bean stateManager = newBean(idauBeans, idauName + "-state-manager",
                "org.atricore.idbus.kernel.main.mediation.state.EHCacheProviderStateManagerImpl");

        setConstructorArgRef(stateManager, 0, idbusCfg.getId());
        setPropertyRef(stateManager, "cacheManager", cacheManager.getName());
        setPropertyValue(stateManager, "cacheName", module.getName() + "-psm-cache");  //cache name needs to be unique
        setPropertyValue(stateManager, "forceNonDirtyStorage", false);

        // -------------------------------------------------------
        // Define Entity Selector Provider
        // -------------------------------------------------------

        Bean selectorMgr = newBean(idauBeans, appliance.getName() + "-entity-selector-mgr", EntitySelectorManagerImpl.class);
        setPropertyRef(selectorMgr, "registry", "entity-selection-strategies-registry");

        Bean entitySelectorProvider = newBean(idauBeans, appliance.getName() + "-entity-selector", EntitySelectorProviderImpl.class);
        setPropertyValue(entitySelectorProvider, "name", entitySelectorProvider.getName());
        setPropertyValue(entitySelectorProvider, "role", "{urn:org:atricore:idbus:sso:metadata}EntitySelectorDescriptor");
        setPropertyRef(entitySelectorProvider, "unitContainer", appliance.getName() + "-container");
        setPropertyRef(entitySelectorProvider, "cotManager", cotMgr.getName());
        setPropertyRef(entitySelectorProvider, "stateManager", stateManager.getName());
        setPropertyRef(entitySelectorProvider, "channel", idauName + "-entity-selector-channel");


        Bean ssoEntitySelectorMediator = newBean(idauBeans, appliance.getName() + "-entity-selector-mediator", SSOEntitySelectorMediator.class);
        setPropertyValue(ssoEntitySelectorMediator, "logMessages", "true");
        setPropertyRef(ssoEntitySelectorMediator, "selectorManager", selectorMgr.getName());

        // Configure IdP selection strategy
        if (ida.getIdpSelector() != null)
            setPropertyValue(ssoEntitySelectorMediator, "preferredStrategy", ida.getIdpSelector().getName());
        else
            setPropertyValue(ssoEntitySelectorMediator, "preferredStrategy", "requested-preferred-idp-selector");

        setPropertyRef(ssoEntitySelectorMediator, "artifactQueueManager", "artifactQueueManager");

        Bean samlr2BindingFactory = newAnonymousBean(SamlR2BindingFactory.class);
        setPropertyBean(ssoEntitySelectorMediator, "bindingFactory", samlr2BindingFactory);

        // logger
        List<Bean> entitySelectorLogBuilders = new ArrayList<Bean>();
        entitySelectorLogBuilders.add(newAnonymousBean(SSOLogMessageBuilder.class));
        entitySelectorLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
        entitySelectorLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

        Bean entitySelectorLogger = newAnonymousBean(DefaultMediationLogger.class.getName());
        entitySelectorLogger.setName(entitySelectorProvider.getName() + "-mediation-logger");
        setPropertyValue(entitySelectorLogger, "category", ida.getNamespace() + ".wire." + entitySelectorProvider.getName());
        setPropertyAsBeans(entitySelectorLogger, "messageBuilders", entitySelectorLogBuilders);
        setPropertyBean(ssoEntitySelectorMediator, "logger", entitySelectorLogger);

        // errorUrl
        setPropertyValue(ssoEntitySelectorMediator, "errorUrl", resolveUiErrorLocation(appliance));

        // warningUrl
        setPropertyValue(ssoEntitySelectorMediator, "warningUrl", resolveUiWarningLocation(appliance));

        // dashboardUrl
        setPropertyValue(ssoEntitySelectorMediator, "dashboardUrl", resolveUiSsoLocation(appliance));

        // Channel

        Bean entitySelectorChannel = newBean(idauBeans, appliance.getName() + "-entity-selector-channel", SelectorChannelImpl.class);
        setPropertyValue(entitySelectorChannel, "name", entitySelectorChannel.getName());

        setPropertyValue(entitySelectorChannel, "location", resolveLocationUrl(ida.getLocation()) + "/SSO/SELECTOR");
        setPropertyRef(entitySelectorChannel, "unitContainer", appliance.getName() + "-container");
        setPropertyRef(entitySelectorChannel, "identityMediator", ssoEntitySelectorMediator.getName());
        setPropertyRef(entitySelectorChannel, "provider", entitySelectorProvider.getName());

        List<Bean> endpoints = new ArrayList<Bean>();

        Bean ssoIdPSelectEndpoint = newBean(idauBeans, appliance.getName() + "-entity-selector-channel-sso-idpselect-http-art", IdentityMediationEndpointImpl.class);
        setPropertyValue(ssoIdPSelectEndpoint, "name", ssoIdPSelectEndpoint.getName());
        setPropertyValue(ssoIdPSelectEndpoint, "type", "{urn:org:atricore:idbus:sso:metadata}IdPSelectorService");
        setPropertyValue(ssoIdPSelectEndpoint, "binding", "urn:org:atricore:idbus:sso:bindings:HTTP-Artifact");
        setPropertyValue(ssoIdPSelectEndpoint, "location", "/IDP");
        endpoints.add(ssoIdPSelectEndpoint);

        setPropertyAsBeans(entitySelectorChannel, "endpoints", endpoints);

        // Wire channels to Unit Container
        addPropertyBeansAsRefs(idMediationUnit, "channels", entitySelectorChannel);

        // -------------------------------------------------------
        // Define MBean Server Factory bean
        // -------------------------------------------------------
        Bean mBeanServer = newBean(idauBeans, "mBeanServer",
                "org.springframework.jmx.support.MBeanServerFactoryBean");
        mBeanServer.setScope("singleton");
        setPropertyValue(mBeanServer, "locateExistingServerIfPossible", true);

        // ---------------------------------------
        // Identity Mediation Unit OSGI Exporter
        // ---------------------------------------

        Service idMediationUnitExporter = new Service();

        idMediationUnitExporter.setId(idauName + "-mediation-unit-exporter");
        idMediationUnitExporter.setRef(idMediationUnit.getName());
        idMediationUnitExporter.setInterface(OsgiIdentityMediationUnit.class.getName());

        idauBeansOsgi.getImportsAndAliasAndBeen().add(idMediationUnitExporter);

        // ----------------------------------------
        // CXF OSGI Importer
        // ----------------------------------------
        Reference cxfImporter = new Reference();
        cxfImporter.setId("cxf");
        cxfImporter.setCardinality("1..1");
        cxfImporter.setTimeout(60L);
        cxfImporter.setInterface("org.apache.cxf.Bus");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(cxfImporter);

        // -------------------------------------------------------
        // JDBC Driver Manager Importer
        // -------------------------------------------------------
        Reference jdbcManagerImporter = new Reference();
        jdbcManagerImporter.setId("jdbc-manager");
        jdbcManagerImporter.setCardinality("1..1");
        jdbcManagerImporter.setTimeout(60L);
        jdbcManagerImporter.setInterface("org.atricore.idbus.kernel.common.support.jdbc.JDBCDriverManager");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(jdbcManagerImporter);


        // ----------------------------------------
        // BPMS Manager OSGI Importer
        // ----------------------------------------
        Reference bpmsManagerImporter = new Reference();
        bpmsManagerImporter.setId("bpms-manager");
        bpmsManagerImporter.setCardinality("1..1");
        bpmsManagerImporter.setTimeout(60L);
        bpmsManagerImporter.setInterface("org.atricore.idbus.kernel.planning.jbpm.BPMSManager");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(bpmsManagerImporter);

        // ----------------------------------------
        // Indentity Plans registry
        // ----------------------------------------
        Bean identityPlansRegistry = newBean(idauBeans, "identity-plans-registry", IdentityPlanRegistryImpl.class);
        identityPlansRegistry.setScope("singleton");

        // ----------------------------------------
        // Message Queue Manager
        // ----------------------------------------
        Reference messageQueueManager = new Reference();
        //messageQueueManager.setId(idauName + "-aqm");
        messageQueueManager.setId("artifactQueueManager");
        messageQueueManager.setCardinality("1..1");
        messageQueueManager.setTimeout(60L);
        messageQueueManager.setInterface("org.atricore.idbus.kernel.main.mediation.MessageQueueManager");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(messageQueueManager);

        // ----------------------------------------
        // Identity Appliance Unit registry
        // ----------------------------------------
        Reference idmuRegistry = new Reference();
        idmuRegistry.setId("idsuRegistry");
        idmuRegistry.setCardinality("1..1");
        idmuRegistry.setTimeout(60L);
        idmuRegistry.setInterface("org.atricore.idbus.kernel.main.mediation.IdentityMediationUnitRegistry");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(idmuRegistry);

        // ----------------------------------------
        // Cache Manager Factory
        // ----------------------------------------
        Reference cacheManagerFactory = new Reference();
        cacheManagerFactory.setId("cache-manager-factory");
        cacheManagerFactory.setCardinality("1..1");
        cacheManagerFactory.setTimeout(60L);
        cacheManagerFactory.setInterface("org.atricore.idbus.bundles.ehcache.CacheManagerFactory");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(cacheManagerFactory);

        // ----------------------------------------
        // Selection Strategy Registry
        // ----------------------------------------
        Reference selectionStrategyRegistry = new Reference();
        selectionStrategyRegistry.setId("entity-selection-strategies-registry");
        selectionStrategyRegistry.setCardinality("1..1");
        selectionStrategyRegistry.setTimeout(60L);
        selectionStrategyRegistry.setInterface("org.atricore.idbus.capabilities.sso.main.select.spi.SelectionStrategiesRegistry");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(selectionStrategyRegistry);

        // ----------------------------------------
        // Monitoring Server
        // ----------------------------------------
        Reference monitoringServer = new Reference();
        monitoringServer.setId("monitoring-server");
        monitoringServer.setCardinality("1..1");
        monitoringServer.setTimeout(60L);
        monitoringServer.setInterface("org.atricore.idbus.kernel.monitoring.core.MonitoringServer");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(monitoringServer);

        // ----------------------------------------
        // Auditing Server
        // ----------------------------------------
        Reference auditingServer = new Reference();
        auditingServer.setId("auditing-server");
        auditingServer.setCardinality("1..1");
        auditingServer.setTimeout(60L);
        auditingServer.setInterface("org.atricore.idbus.kernel.auditing.core.AuditingServer");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(auditingServer);


        // ----------------------------------------
        // Container for Identity Flow Components
        // ----------------------------------------
        Reference identityFlowContainer = new Reference();
        identityFlowContainer.setId("identity-flow-container");
        identityFlowContainer.setCardinality("1..1");
        identityFlowContainer.setTimeout(60L);
        identityFlowContainer.setInterface("org.atricore.idbus.capabilities.sso.component.container.IdentityFlowContainer");

        idauBeansOsgi.getImportsAndAliasAndBeen().add(identityFlowContainer);

        // ----------------------------------------
        // Store beans as module resources
        // ----------------------------------------

        IdProjectResource<Beans> rBeans = new IdProjectResource<Beans>(idGen.generateId(), "beans", "spring-beans", idauBeans);
        rBeans.setClassifier("jaxb");
        module.addResource(rBeans);

        IdProjectResource<Beans> rBeansOsgi = new IdProjectResource<Beans>(idGen.generateId(), "beans-osgi", "spring-beans", idauBeansOsgi);
        rBeansOsgi.setClassifier("jaxb");
        module.addResource(rBeansOsgi);

    }

    @Override
    public Object after(TransformEvent event) {
        // Beans wiring
        IdProjectResource<Beans> rIdauBeans = null;

        for (IdProjectResource r : event.getContext().getCurrentModule().getResources()) {
            if (r.getValue() instanceof Beans && r.getName().equals("beans")) {
                rIdauBeans = r;
                break;
            }
        }

        if (rIdauBeans == null)
            throw new IllegalStateException("Cannot find beans definition");

        for (IdProjectResource r : event.getContext().getCurrentModule().getResources()) {
            if (r.getValue() instanceof Beans && r.getNameSpace() != null && r.getSubtype() == null) {
                if (logger.isDebugEnabled())
                    logger.debug("Importing beans " + r);
                Import i = new Import();
                i.setResource(r.getId());
                rIdauBeans.getValue().getImportsAndAliasAndBeen().add(i);
            }
        }


        return rIdauBeans;
    }
}

