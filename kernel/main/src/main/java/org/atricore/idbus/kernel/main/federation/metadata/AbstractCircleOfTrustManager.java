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

package org.atricore.idbus.kernel.main.federation.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.provider.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.util.*;

/**
 *
 *
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: AbstractCircleOfTrustManager.java 1359 2009-07-19 16:57:57Z sgonzalez $
 */
public abstract class AbstractCircleOfTrustManager implements CircleOfTrustManager, InitializingBean, DisposableBean  {

    private static final Log logger = LogFactory.getLog(AbstractCircleOfTrustManager.class);

    private CircleOfTrust cot;

    private Map<String, MetadataDefinition> definitions = new HashMap<String, MetadataDefinition>();

    boolean init = false;

    public CircleOfTrust getCot() {
        return cot;
    }

    public void setCot(CircleOfTrust cot) {
        this.cot = cot;
    }

    public void afterPropertiesSet() throws Exception {
        // Nothing to do ..
    }

    public void init() throws CircleOfTrustManagerException {

        synchronized (this) {

            if (init)
                return;

            logger.info("Initializing Circle Of Trust (COT) " + cot.getName());

            try {

                for (FederatedProvider provider : cot.getProviders()) {


                    for (CircleOfTrustMemberDescriptor member : provider.getMembers()) {

                        if (logger.isDebugEnabled())
                            logger.debug("Initializing Provider Member information " + provider.getName() + ", member " + member.getAlias());

                        registerMember(member);
                    }

                    if (provider instanceof AbstractFederatedProvider) {
                        AbstractFederatedProvider localProvider = (AbstractFederatedProvider) provider;
                        localProvider.setCircleOfTrust(cot);
                    } else if (provider instanceof FederatedRemoteProviderImpl) {
                        FederatedRemoteProviderImpl remoteProvider = (FederatedRemoteProviderImpl) provider;
                        remoteProvider.setCircleOfTrust(cot);
                    } else {
                        logger.debug("Unknown provider type " + provider + ", cannot inject COT");
                    }


                }

                init = true;

                logger.info("Initializing COT " + cot.getName() + " OK");
                
            } catch (Exception e) {
                logger.error("Initializing COT Manager : " + e.getMessage());
                throw new CircleOfTrustManagerException(e);
            }


        }
    }

    protected void registerMember(CircleOfTrustMemberDescriptor member) throws CircleOfTrustManagerException {

        if (logger.isDebugEnabled())
            logger.debug("Registering COT Member descriptor " + member);

        if (member instanceof ResourceCircleOfTrustMemberDescriptorImpl) {

            // Resolve Metadata Definition
            ResourceCircleOfTrustMemberDescriptorImpl m = (ResourceCircleOfTrustMemberDescriptorImpl) member;
            if (logger.isDebugEnabled())
                logger.debug("Loading metadata for member " + member.getId() + " [" + member.getAlias() + "]");
            
            MetadataDefinition def = loadMetadataDefinition(member, m.getResource());
            MetadataDefinition old = definitions.put(member.getAlias(), def);

            MetadataEntry md = findEntityMetadata(member.getAlias());
            if (md == null)
                logger.warn("No metadata found for COT Member " + member.getAlias());
            
            m.setMetadata(md);

            if (old != null)
                throw new CircleOfTrustManagerException("Duplicated COT Member descriptor for alias : " +
                        member.getAlias());

        } else {
            // TODO : Local providers should create their own springmetadata, can we do it here?
            logger.error("Unsupported COT Member descriptor type " + member);
        }

    }

    public void destroy() throws Exception {

    }

    public void exportCircleOfTrustMetadataDefinition(String memberAlias) {
        throw new UnsupportedOperationException("Not implemented");
    }


    public Collection<CircleOfTrustMemberDescriptor> getMembers() {
        List<CircleOfTrustMemberDescriptor> members = new ArrayList<CircleOfTrustMemberDescriptor>();
        for (FederatedProvider p : cot.getProviders()) {
            members.addAll(p.getMembers());
        }
        return members;
    }

    /**
     * This will look in all registered providers that realize the given role for the channel that targets the source
     * provider and return its COT Member descriptor.
     *
     */
    public CircleOfTrustMemberDescriptor lookupMemberForProvider(Provider srcProvider, String role) throws CircleOfTrustManagerException {

        if (logger.isDebugEnabled())
            logger.debug("Looking for COT Member descriptor for source provider " + srcProvider.getName() +
                    ", role " + role);


        for (Provider destProvider : cot.getProviders()) {
            if (destProvider.getRole().equals(role)) {
                if (logger.isDebugEnabled())
                    logger.debug("Provider " + destProvider .getName() + " has role " + role);

                CircleOfTrustMemberDescriptor selectedMember = lookupMemberForProvider(srcProvider, destProvider);

                if (selectedMember != null) {
                    if (logger.isDebugEnabled())
                        logger.debug("Selected COT Member '" + selectedMember + "' for provider '" + srcProvider.getName() +
                                "' with role '" + role + "' in COT " + cot.getName());
                    return selectedMember;
                }


            }
        }

        if (logger.isDebugEnabled())
            logger.debug("NO COT Member found for provider '" + srcProvider.getName() +
                    "' with role '" + role + "' in COT " + cot.getName());

        return null;

    }

    public CircleOfTrustMemberDescriptor lookupMemberForProvider(Provider srcProvider, Provider destProvider) throws CircleOfTrustManagerException {

        CircleOfTrustMemberDescriptor targetingMember = null;

        if (destProvider instanceof FederatedLocalProvider) {
            FederatedLocalProvider federatedDestProvider = (FederatedLocalProvider) destProvider;

            for (FederationChannel channel : federatedDestProvider.getChannels()) {
                if (channel.getTargetProvider().equals(srcProvider)) {
                    targetingMember = channel.getMember();
                    if (logger.isDebugEnabled())
                        logger.debug("Selected targeting member : " + targetingMember.getAlias());

                }
            }
        }


        if (targetingMember == null) {
            logger.debug("No Selected between source " + srcProvider.getName() + " and destination " +  destProvider.getName());
        }

        return targetingMember;


    }

    public boolean isLocalMember(String alias) throws CircleOfTrustManagerException {
        for (Provider p : cot.getProviders()) {
            if (p.getName().equals(alias)) {
                return !(p instanceof FederatedRemoteProvider);
            }
        }
        throw new CircleOfTrustManagerException("Unknonw entity " + alias);
    }

    public Collection<CircleOfTrustMemberDescriptor> lookupMembersForProvider(Provider provider, String role)
            throws CircleOfTrustManagerException {

        Set<CircleOfTrustMemberDescriptor> members = new HashSet<CircleOfTrustMemberDescriptor>();

        for (Provider destProvider : cot.getProviders()) {
            if (destProvider.getRole().equals(role)) {

                if (logger.isDebugEnabled())
                    logger.debug("Provider " + destProvider .getName() + " has role " + role);

                if (destProvider instanceof FederatedLocalProvider) {
                    FederatedLocalProvider federatedDestProvider = (FederatedLocalProvider) destProvider;

                    members.add(federatedDestProvider.getChannel().getMember());

                    for (FederationChannel channel : federatedDestProvider.getChannels()) {

                        if (channel.getTargetProvider().equals(provider)) {
                            members.add(channel.getMember());
                            if (logger.isDebugEnabled())
                                logger.debug("Selected targeting member : " + channel.getMember().getAlias());

                        }


                    }
                } else {
                    FederatedRemoteProvider destRemoteProvider = (FederatedRemoteProvider) destProvider;
                    for(CircleOfTrustMemberDescriptor m : destRemoteProvider.getMembers()) {
                        if (logger.isDebugEnabled())
                            logger.debug("Selected member : " + m.getAlias());

                        members.add(m);
                    }
                }



            }
        }

        return members;
    }

    public CircleOfTrustMemberDescriptor loolkupMemberByAlias(String alias) {

        for (FederatedProvider provider : cot.getProviders()) {

            for (CircleOfTrustMemberDescriptor member : provider.getMembers()) {
                if (member.getAlias().equals(alias)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Specific COT Member found for " + alias + " in provider " + provider.getName());
                    return member;
                }
            }
        }


        if (logger.isDebugEnabled())
            logger.debug("No COT Member registered with alias " + alias);

        // Not found !?
        return null;

    }

    public CircleOfTrustMemberDescriptor loolkupMemberById(String id) {
        for (FederatedProvider provider : cot.getProviders()) {

            for (CircleOfTrustMemberDescriptor member : provider.getMembers()) {
                if (member.getId().equals(id)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Specific COT Member found for " + id + " in provider " + provider.getName());
                    return member;
                }
            }
        }


        if (logger.isDebugEnabled())
            logger.debug("No COT Member registered with ID" + id);

        // Not found !?
        return null;
    }

    /**
     * The member alias must match a MD Definition ID.
     */
    public MetadataEntry findEntityMetadata(String memberAlias) throws CircleOfTrustManagerException {

        if (memberAlias == null)
            throw new NullPointerException("Member Alias cannot be null");

        CircleOfTrustMemberDescriptor member = loolkupMemberByAlias(memberAlias);
        if (member == null) {
            throw new CircleOfTrustManagerException("Entity ID is not a COT member alias : " + memberAlias + " in COT " + cot.getName());
        }

        MetadataDefinition md = definitions.get(member.getAlias());
        return this.searchEntityDefinition(md, memberAlias);
    }

    public MetadataEntry findEntityRoleMetadata(String memberAlias, String entityRole) throws CircleOfTrustManagerException {

        if (memberAlias == null)
            throw new NullPointerException("Member Alias cannot be null");
        if (entityRole == null)
            throw new NullPointerException("Entity Role cannot be null");

        CircleOfTrustMemberDescriptor member = loolkupMemberByAlias(memberAlias);
        if (member == null) {
            throw new CircleOfTrustManagerException("Entity ID is not a COT member alias : " + memberAlias + " in COT " + cot.getName());
        }

        MetadataDefinition md = definitions.get(member.getAlias());
        if (md == null) {
            logger.debug("Entity Role metadata not found for '" + memberAlias + "', role '"+entityRole+"'");
            return null;
        }

        return this.searchEntityRoleDefinition(md, memberAlias, entityRole);
    }

    public MetadataEntry findEndpointMetadata(String memberAlias, String entityRole, EndpointDescriptor endpoint) throws CircleOfTrustManagerException {
        if (memberAlias == null)
            throw new NullPointerException("Member Alias cannot be null");
        if (entityRole == null)
            throw new NullPointerException("Entity Role cannot be null");
        if (endpoint == null)
            throw new NullPointerException("IdentityMediationEndpoint cannot be null");

        CircleOfTrustMemberDescriptor member = loolkupMemberByAlias(memberAlias);
        if (member == null) {
            throw new CircleOfTrustManagerException("Entity ID is not a COT member alias : " + memberAlias + " in COT " + cot.getName());
        }

        MetadataDefinition md = definitions.get(member.getAlias());
        if (md == null) {
            logger.debug("Identity Mediation Endpoint metadata not found for '" + memberAlias +
                    "', role '" + entityRole +
                    "', endpoint='" + endpoint + "'");
            return null;
        }

        return this.searchEndpointDescriptor(md, memberAlias, entityRole, endpoint);
    }

    public Collection<MetadataEntry> findEndpointsMetadata(String memberAlias, String entityRole, EndpointDescriptor endpoint) throws CircleOfTrustManagerException {
        if (memberAlias == null)
            throw new NullPointerException("Member Alias cannot be null");
        if (entityRole == null)
            throw new NullPointerException("Entity Role cannot be null");
        if (endpoint == null)
            throw new NullPointerException("IdentityMediationEndpoint cannot be null");
        
        CircleOfTrustMemberDescriptor member = loolkupMemberByAlias(memberAlias);
        if (member == null) {
            throw new CircleOfTrustManagerException("Entity ID is not a COT member alias : " + memberAlias + " in COT " + cot.getName());
        }

        MetadataDefinition md = definitions.get(member.getAlias());
        if (md == null) {
            logger.debug("Endpoints metadata not found for '" + memberAlias +
                    "', role '" + entityRole +
                    "', endpoint '" + endpoint + "'");
            return null;
        }

        return this.searchEndpointDescriptors(md, memberAlias, entityRole, endpoint);
    }

    // --------------------------------------------------------------------------------------------
    // Abstracts Primitives, in the future this could be a different component,
    // associated to COT member somehow
    // --------------------------------------------------------------------------------------------

    protected abstract MetadataDefinition loadMetadataDefinition(CircleOfTrustMemberDescriptor member,
                                                                 Resource resource)
            throws CircleOfTrustManagerException;

    protected abstract MetadataEntry searchEntityDefinition(MetadataDefinition metadataDefinition,
                                                            String memberAlias)
            throws CircleOfTrustManagerException;

    protected abstract MetadataEntry searchEntityRoleDefinition(MetadataDefinition metadataDefinition, 
                                                                String memberAlias,
                                                                String roleType)
            throws CircleOfTrustManagerException;

    protected abstract MetadataEntry searchEndpointDescriptor(MetadataDefinition metadataDefinition,
                                                                  String memberAlias,
                                                                  String roleType,
                                                                  EndpointDescriptor endpoint)
            throws CircleOfTrustManagerException;

    protected abstract Collection<MetadataEntry> searchEndpointDescriptors(MetadataDefinition metadataDefinition,
                                                                  String memberAlias,
                                                                  String roleType,
                                                                  EndpointDescriptor endpoint)
            throws CircleOfTrustManagerException;;


}