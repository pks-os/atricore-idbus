package com.atricore.idbus.console.liveservices.liveupdate.main.service;

import com.atricore.idbus.console.liveservices.liveupdate.main.LiveUpdateException;
import com.atricore.idbus.console.liveservices.liveupdate.main.LiveUpdateManager;
import com.atricore.idbus.console.liveservices.liveupdate.main.engine.UpdateEngine;
import com.atricore.idbus.console.liveservices.liveupdate.main.notifications.NotificationHandler;
import com.atricore.idbus.console.liveservices.liveupdate.main.notifications.NotificationScheme;
import com.atricore.idbus.console.liveservices.liveupdate.main.notifications.NotificationSchemeStore;
import com.atricore.idbus.console.liveservices.liveupdate.main.profile.ProfileManager;
import com.atricore.idbus.console.liveservices.liveupdate.main.repository.ArtifactRepository;
import com.atricore.idbus.console.liveservices.liveupdate.main.repository.MetadataRepository;
import com.atricore.idbus.console.liveservices.liveupdate.main.repository.Repository;
import com.atricore.idbus.console.liveservices.liveupdate.main.repository.RepositoryTransport;
import com.atricore.idbus.console.liveservices.liveupdate.main.repository.impl.*;
import com.atricore.idbus.console.liveservices.liveupdate.main.util.FilePathUtil;
import com.atricore.liveservices.liveupdate._1_0.md.InstallableUnitType;
import com.atricore.liveservices.liveupdate._1_0.md.UpdateDescriptorType;
import com.atricore.liveservices.liveupdate._1_0.md.UpdatesIndexType;
import com.atricore.liveservices.liveupdate._1_0.profile.ProfileType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Periodically analyze MD and see if updates apply.
 * Keep track of current version/update
 * Queue update processes, to be triggered on reboot.
 * Manage update lifecycle.
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class LiveUpdateManagerImpl implements LiveUpdateManager, BundleContextAware {

    private static final Log logger = LogFactory.getLog(LiveUpdateManagerImpl.class);

    private List<RepositoryTransport> transports;

    private BundleContext bundleContext;

    private ProfileManager profileManager;

    private UpdateEngine engine;

    private UpdatesMonitor updatesMonitor;

    private ScheduledThreadPoolExecutor stpe;

    private int updatesCheckInterval;

    private SystemStartupMonitor systemStartupMonitor;
    private ScheduledThreadPoolExecutor startupStpe;
    private int startupCheckInterval;
    private int startupRunLevel;
    
    private String dataFolder;

    private Properties config;

    private MetadataRepositoryManagerImpl mdManager;

    private ArtifactRepositoryManagerImpl artManager;

    private String certProviderName = "SUN";

    private CertStore certStore;

    private List<NotificationHandler> notificationHandlers;

    private List<NotificationSchemeStore> notificationStores;

    public void init() throws LiveUpdateException {

        try {

            // Crate Collection CertStore using SUN Provider
            CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(new ArrayList()),
                    certProviderName);
            // TODO : Add CRLs, etc.

            // Start update check thread.
            logger.info("Initializing LiveUpdate service ...");
            Set<String> used = new HashSet<String>();

            for (Object k : config.keySet()) {
                String key = (String) k;

                if (key.startsWith("repo.md.")) {

                    // We need to configure a repo, get repo base key.
                    String repoId = key.substring("repo.md.".length());
                    repoId = repoId.substring(0, repoId.indexOf('.'));

                    String repoKeys = "repo.md." + repoId;
                    if (used.contains(repoKeys))
                        continue;

                    used.add(repoKeys);

                    try {
                        MetadataRepository repo = (MetadataRepository) buildVFSRepository(VFSMetadataRepositoryImpl.class, repoKeys);
                        logger.info("Using LiveUpdate MD Repository at " + repo.getLocation());
                        mdManager.addRepository(repo);

                    } catch (LiveUpdateException e) {
                        logger.error("Ignoring MD repository definition : " + e.getMessage());

                        // When debugging, error log includs stack trace.
                        if (logger.isDebugEnabled())
                            logger.error("Ignoring MD repository definition : " + e.getMessage(), e);
                    }


                } else if (key.startsWith("repo.art.")) {
                    // We need to configure a repo, get repo base key.
                    String repoId = key.substring("repo.art.".length());
                    repoId = repoId.substring(0, repoId.indexOf('.'));

                    String repoKeys = "repo.art." + repoId;
                    if (used.contains(repoKeys))
                        continue;

                    used.add(repoKeys);

                    try {
                        ArtifactRepository repo = (ArtifactRepository) buildVFSRepository(VFSArtifactRepositoryImpl.class, repoKeys);
                        logger.info("Using LiveUpdate Artifact Repository at " + repo.getLocation());
                        artManager.addRepository(repo);
                    } catch (LiveUpdateException e) {
                        logger.error("Ignoring Artifact repository definition : " + e.getMessage());

                        // When debugging, error log includs stack trace.
                        if (logger.isDebugEnabled())
                            logger.error("Ignoring Artifact repository definition : " + e.getMessage(), e);
                    }
                }

            }

        } catch (NoSuchAlgorithmException e) {
            throw new LiveUpdateException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new LiveUpdateException(e);
        } catch (NoSuchProviderException e) {
            throw new LiveUpdateException(e);
        }

        updatesMonitor = new UpdatesMonitor(this, updatesCheckInterval * 1000);

        stpe = new ScheduledThreadPoolExecutor(3);
        stpe.scheduleAtFixedRate(updatesMonitor, 60,
                updatesCheckInterval,
                TimeUnit.SECONDS);

        systemStartupMonitor = new SystemStartupMonitor(engine, startupRunLevel, getBundleContext());

        startupStpe = new ScheduledThreadPoolExecutor(3);
        startupStpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        startupStpe.scheduleAtFixedRate(systemStartupMonitor, startupCheckInterval,
                startupCheckInterval, TimeUnit.SECONDS);
    }

    public void shutdown() {
        try {
            if (stpe != null)
                stpe.shutdown();
        } catch (Exception e) {
            logger.warn (e.getMessage());
        }
    }

    public ProfileType getCurrentProfile(boolean rebuild) throws LiveUpdateException {
        return this.profileManager.getCurrentProfile(rebuild);
    }

    public Collection<Repository> getRepositories() {

        List<Repository> repos = new ArrayList<Repository>();

        repos.addAll(mdManager.getRepositories());
        repos.addAll(artManager.getRepositories());

        return repos;
    }

    public UpdatesIndexType getRepositoryUpdates(String repoId) throws LiveUpdateException {
        return mdManager.getUpdatesIndex(repoId, true);
    }

    public Collection<UpdateDescriptorType> getAvailableUpdates() throws LiveUpdateException {
        Collection<UpdateDescriptorType> updates = mdManager.getUpdates();
        ProfileType profile = profileManager.getCurrentProfile(true);

        Map<String, UpdateDescriptorType> availableUpdates = new HashMap<String, UpdateDescriptorType>();

        for (InstallableUnitType installed : profile.getInstallableUnit()) {
            Collection<UpdateDescriptorType> uds = profileManager.getAvailableUpdates(installed, updates);
            for (UpdateDescriptorType ud : uds) {
                availableUpdates.put(ud.getID(), ud);
            }
        }

        return availableUpdates.values();

    }

    public Collection<UpdateDescriptorType> getAvailableUpdates(String group, String name, String version) throws LiveUpdateException {
        Collection<UpdateDescriptorType> updates = mdManager.getUpdates();
        ProfileType profile = profileManager.getCurrentProfile(true);

        for (InstallableUnitType installed : profile.getInstallableUnit()) {
            String installedFqn = installed.getGroup() + "/" + installed.getName() + "/" + installed.getVersion();
            if (installedFqn.equals(group + "/" + name + "/" + version)) {
                return profileManager.getAvailableUpdates(installed, updates);
            }

        }
        
        throw new LiveUpdateException("Install Unit not found in current profile");
    }

    public void cleanRepository(String repoId) throws LiveUpdateException {
        mdManager.clearRepository(repoId);
        artManager.clearRepository(repoId);
    }

    public void cleanAllRepositories() throws LiveUpdateException {
        mdManager.clearRepositories();
        artManager.clearRepositories();
    }

    // Analyze MD and see if updates apply. (use license information ....)
    public Collection<UpdateDescriptorType> checkForUpdates() throws LiveUpdateException {
        mdManager.refreshRepositories();
        Collection<UpdateDescriptorType> updates = getAvailableUpdates();
        for (NotificationScheme scheme : listNotificationSchemes()) {
            if (scheme.isEnabled()) {
                for (NotificationHandler handler : notificationHandlers) {
                    if (handler.canHandle(scheme)) {
                        handler.notify(updates, scheme);
                    }
                }
            }
        }
        return updates;
    }

    public Collection<UpdateDescriptorType> checkForUpdates(String group, String name, String version) throws LiveUpdateException {
        mdManager.refreshRepositories();
        return getAvailableUpdates(group, name , version);
    }

    // Apply update
    public void applyUpdate(String group, String name, String version, boolean offline) throws LiveUpdateException {

        if (logger.isDebugEnabled())
            logger.debug("Trying to apply update for " + group + "/" + name + "/" + version);

        if (!offline)
            mdManager.refreshRepositories();

        Collection<UpdateDescriptorType> availableUpdates = getAvailableUpdates();
        InstallableUnitType installable  = null;
        UpdateDescriptorType update = null;

        for (UpdateDescriptorType ud : availableUpdates) {
            InstallableUnitType iu = ud.getInstallableUnit();
            if (iu.getGroup().equals(group) && iu.getName().equals(name) && iu.getVersion().equals(version)) {
                installable = iu;
                update = ud;
                if (logger.isDebugEnabled())
                    logger.debug("Found IU " + iu.getID() + " for " + group + "/" + name + "/" + version);
                break;
            }
        }

        if (installable == null) {
            throw new LiveUpdateException("Update not available for current setup : " +
                    group + "/" + name + "/" + version);
        }

        logger.info("Applying Update " + group + "/" + name + "/" + version);

        Collection<UpdateDescriptorType> updates = mdManager.getUpdates();
        ProfileType updateProfile = profileManager.buildUpdateProfile(installable, updates);

        engine.execute("defaultUpdatePlan", updateProfile);
    }

    public ProfileType getUpdateProfile() throws LiveUpdateException {
        Collection<UpdateDescriptorType> updates = mdManager.refreshRepositories();
        ProfileType profile = getCurrentProfile(true);

        for (InstallableUnitType iu : profile.getInstallableUnit()) {
            profileManager.buildUpdateProfile(iu, updates);
        }
        throw new UnsupportedOperationException("implement me");

    }


    public ProfileType getUpdateProfile(String group, String name, String version) throws LiveUpdateException {
        mdManager.refreshRepositories();
        UpdateDescriptorType ud = mdManager.getUpdate(group, name, version);
        if (ud == null)
            throw new LiveUpdateException("No update found for " + group +"/"+name+"/"+version);
        
        return this.profileManager.buildUpdateProfile(ud.getInstallableUnit(), mdManager.getUpdates());
    }

    // -------------------------------------------< Utilities >

    protected AbstractVFSRepository buildVFSRepository(Class repoType, String repoKeys) throws LiveUpdateException {

        try {
            // Get id,name, location, enabled
            if (logger.isTraceEnabled())
                logger.trace("Adding new repository : " + repoKeys);

            // Repository ID
            String id = config.getProperty(repoKeys + ".id");
            if (id == null)
                throw new LiveUpdateException("Repository ID is required. Configuration keys " + repoKeys);

            // Repository Name
            String name = config.getProperty(repoKeys + ".name");
            if (name == null)
                throw new LiveUpdateException("Repository name is required for " + id);

            // Enabled
            boolean enabled = Boolean.parseBoolean(config.getProperty(repoKeys + ".enabled", "false"));

            // DS Validation
            boolean validateSignature = Boolean.parseBoolean(config.getProperty(repoKeys + ".validateSignature", "true"));


            // Certificate for DS validation
            // TODO : Use java.security.cert.CertStore to keep track of all certs :) !?
            String certFile =  config.getProperty(repoKeys + ".certificate");
            certFile = FilePathUtil.fixFilePath(certFile);
            // TODO : Read/Decode/Validate the certificate file , use CertStore instance ..
            byte[] certificate = null;

            if (validateSignature && certificate == null) {
                throw new LiveUpdateException("Repository " + id + " has Digital Signature enabled, but no certificate was provided" );
            }

            // Repository Location
            URI location = null;
            try {
                // Since we're handling the configuration properties, we cannot rely on spring properties resolver.
                String l = config.getProperty(repoKeys + ".location");
                l = l.replaceAll("\\$\\{karaf\\.data\\}", dataFolder.replace("\\", "/"));
                l = FilePathUtil.fixFilePath(l);
                location = new URI(l);

            } catch (Exception e) {
                logger.error("Invalid URI ["+config.getProperty(repoKeys + ".location")+"] for repository " + id + " " + name);
                return null;
            }

            if (logger.isDebugEnabled())
                logger.debug("Adding new VFS Repository ["+id+"] " + name +
                        (enabled ? "enabled" : "disabled" ) + " at " + location);


            AbstractVFSRepository repo = (AbstractVFSRepository) repoType.newInstance();
            repo.setId(id);
            repo.setName(name);
            repo.setLocation(location);
            repo.setEnabled(enabled);
            repo.setCertValue(certificate);
            repo.setSignatureValidationEnabled(validateSignature);
            // TODO : Take from license
            //repo.setUsername();
            //repo.setPassword();

            repo.setRepoFolder(new URI(FilePathUtil.fixFilePath(dataFolder + "/liveservices/liveupdate/repos/cache/" + id)));

            return repo;
        } catch (Exception e) {
            throw new LiveUpdateException("Cannot configure repository. " + e.getMessage(), e);
        }

    }

    public void saveNotificationScheme(NotificationScheme scheme) throws LiveUpdateException {
        for (NotificationHandler handler : notificationHandlers) {
            if (handler.canHandle(scheme)) {
                handler.saveNotificationScheme(scheme);
            }
        }
    }

    public void removeNotificationScheme(NotificationScheme scheme) throws LiveUpdateException {
        for (NotificationHandler handler : notificationHandlers) {
            if (handler.canHandle(scheme)) {
                handler.removeNotificationScheme(scheme);
            }
        }
    }

    public Collection<NotificationScheme> listNotificationSchemes() throws LiveUpdateException {
        List<NotificationScheme> schemes = new ArrayList<NotificationScheme>();
        for (NotificationSchemeStore store : notificationStores) {
            schemes.addAll(store.loadAll());
        }
        return schemes;
    }

    public NotificationScheme getNotificationScheme(String name) throws LiveUpdateException {
        NotificationScheme scheme = null;
        for (NotificationSchemeStore store : notificationStores) {
            scheme = store.load(name);
            if (scheme != null) {
                break;
            }
        }
        return scheme;
    }

    // -------------------------------------------< Properties >

    public void setConfig(Properties config) {
        this.config = config;
    }

    public Properties getConfig() {
        return config;
    }

    public void setMetadataRepositoryManager(MetadataRepositoryManagerImpl metadataRepositoryManager) {
        this.mdManager = metadataRepositoryManager;
    }

    public MetadataRepositoryManagerImpl getMetadataRepositoryManager() {
        return mdManager;
    }

    public void setArtifactRepositoryManager(ArtifactRepositoryManagerImpl artifactRepositoryManager) {
        this.artManager = artifactRepositoryManager;
    }

    public ArtifactRepositoryManagerImpl getArtifactRepositoryManager() {
        return artManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public void setProfileManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public UpdateEngine getEngine() {
        return engine;
    }

    public void setEngine(UpdateEngine engine) {
        this.engine = engine;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    public String getCertProviderName() {
        return certProviderName;
    }

    public void setCertProviderName(String certProviderName) {
        this.certProviderName = certProviderName;
    }

    //

    public void setUpdatesCheckInterval(int updatesCheckInterval) {
        this.updatesCheckInterval = updatesCheckInterval;
    }

    public int getUpdatesCheckInterval() {
        return updatesCheckInterval;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public int getStartupRunLevel() {
        return startupRunLevel;
    }

    public void setStartupRunLevel(int startupRunLevel) {
        this.startupRunLevel = startupRunLevel;
    }

    public int getStartupCheckInterval() {
        return startupCheckInterval;
    }

    public void setStartupCheckInterval(int startupCheckInterval) {
        this.startupCheckInterval = startupCheckInterval;
    }

    public List<NotificationHandler> getNotificationHandlers() {
        return notificationHandlers;
    }

    public void setNotificationHandlers(List<NotificationHandler> notificationHandlers) {
        this.notificationHandlers = notificationHandlers;
    }

    public List<NotificationSchemeStore> getNotificationStores() {
        return notificationStores;
    }

    public void setNotificationStores(List<NotificationSchemeStore> notificationStores) {
        this.notificationStores = notificationStores;
    }
}