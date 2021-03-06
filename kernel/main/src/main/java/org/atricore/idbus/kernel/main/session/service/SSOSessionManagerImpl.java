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

package org.atricore.idbus.kernel.main.session.service;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.auditing.core.AuditingServer;
import org.atricore.idbus.kernel.main.authn.SecurityToken;
import org.atricore.idbus.kernel.main.session.*;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.session.exceptions.SSOSessionException;
import org.atricore.idbus.kernel.main.session.exceptions.TooManyOpenSessionsException;
import org.atricore.idbus.kernel.main.store.session.SessionStore;
import org.atricore.idbus.kernel.main.util.ConfigurationContext;
import org.atricore.idbus.kernel.main.util.IDBusConfigurationConstants;
import org.atricore.idbus.kernel.monitoring.core.MonitoringServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @org.apache.xbean.XBean element="session-manager"
 *
 * This is the default implementation of the SSO Session Manager.
 *
 * @author <a href="mailto:sgonzalez@josso.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: SSOSessionManagerImpl.java 1331 2009-06-23 22:04:18Z sgonzalez $
 */

public class SSOSessionManagerImpl implements SSOSessionManager, InitializingBean, DisposableBean {

    private static final Log logger = LogFactory.getLog(SSOSessionManagerImpl.class);

    // Max inactive interval used for new sessions. Default is set to 30 minutes
    private int _maxInactiveInterval = 30;

    private int _maxSessionsPerUser = 1;

    private long _sessionMonitorInterval = 5000;

    private boolean _invalidateExceedingSessions = false;

    private boolean _initStats = true;

    private SSOSessionStats _stats;

    @Deprecated
    private String _securityDomainName;

    private String _node;

    private String _auditCategory;

    private ConfigurationContext _config;

    private AuditingServer _aServer;

    /**
     * This implementation uses a MemoryStore and a defaylt Session Id generator.
     */
    public SSOSessionManagerImpl() {
    }

    public SSOSessionManagerImpl(ConfigurationContext config) {
        _config = config;
    }

    public void afterPropertiesSet() throws Exception {
        initialize();
    }

    public void destroy() throws Exception {
        if (_monitor != null) {
            try {
                _monitor.stop();
            } catch (Exception e) {
                /* Ignore this*/
            }
        }
    }

    //-----------------------------------------------------
    // Instance variables :
    //-----------------------------------------------------

    private SessionStore _store;
    private SessionIdGenerator _idGen;

    // SSO Sessions monitor
    private SSOSessionMonitor _monitor;

    //------------------------------------------------------
    // SSO Session Manager
    //------------------------------------------------------

    public void setSecurityDomainName(String securityDomainName) {
        _securityDomainName = securityDomainName;
    }

    public void setNode(String node) {
        _node = node;
    }


    /**
     * Initializes the manager.
     */
    public synchronized void initialize() {

        if (_config != null) {
            // Retrieve properties from configuration context
            _node = _config.getProperty(IDBusConfigurationConstants.IDBUS_NODE, _node);
        }

        logger.info("[initialize()] : IdGenerator.................=" + _idGen.getClass().getName());
        logger.info("[initialize()] : Store.......................=" + _store.getClass().getName());

        logger.info("[initialize()] : MaxInactive.................=" + _maxInactiveInterval);
        logger.info("[initialize()] : MaxSessionsPerUser..........=" + _maxSessionsPerUser);
        logger.info("[initialize()] : InvalidateExceedingSessions.=" + _invalidateExceedingSessions);
        logger.info("[initialize()] : SessionMonitorInterval......=" + _sessionMonitorInterval);
        logger.info("[initialize()] : Node........................=" + _node);

        // Start session monitor.
        _monitor.start();

        // Register sessions in security domain !
        logger.info("[initialize()] : Restore Sec.Domain Registry.=" + _securityDomainName);

    }

    /**
     * Initiates a new session. The new session id is returned.
     *
     * @return the new session identifier.
     */
    public String initiateSession(String username, SecurityToken securityToken, SSOSessionContext ctx) throws SSOSessionException {
        // Convert minutes to seconds:
        return initiate(username, securityToken, ctx, getMaxInactiveInterval() * 60);
    }

    /**
     * Initiates a new session. The new session id is returned.
     *
     * @param sessionTimeoutInSeconds in seconds
     * @return
     * @throws SSOSessionException
     */
    public String initiateSession(String username, SecurityToken securityToken, SSOSessionContext ctx, int sessionTimeoutInSeconds) throws SSOSessionException {
        return initiate(username, securityToken, ctx, sessionTimeoutInSeconds);
    }

    /**
     * Method that initiates a new session.
     *
     * @param username
     * @param securityToken
     * @param sessionTimeout
     * @return
     * @throws SSOSessionException
     */
    protected String initiate(String username, SecurityToken securityToken, SSOSessionContext ctx, int sessionTimeout) throws SSOSessionException {

        // Invalidate sessions if necessary
        BaseSession sessions[] = _store.loadByUsername(username);

        // Check if we can open a new session for this user.
        if (!_invalidateExceedingSessions &&
                _maxSessionsPerUser != -1 &&
                _maxSessionsPerUser <= sessions.length) {
            throw new TooManyOpenSessionsException(sessions.length);
        }

        // Check if sessions should be auto-invalidated.
        if (_invalidateExceedingSessions && _maxSessionsPerUser != -1) {

            // Number of sessions to invalidate, since we're about to create a new session, add one to the result
            int invalidate = sessions.length - _maxSessionsPerUser + 1;
            if (logger.isDebugEnabled())
                logger.debug("Auto-invalidating " + invalidate + " sessions for user : " + username);

            for (int i = 0; i < sessions.length; i++) {

                if (invalidate < 1)
                    break;

                BaseSession session = sessions[i];
                if (session.getId().equals(securityToken.getId()))
                    continue;

                if (logger.isDebugEnabled())
                    logger.debug("Auto-invalidating " + session.getId() + " session for user : " + username);

                try {
                    invalidate(session.getId());
                } catch (NoSuchSessionException e) {
                    // ignore this
                }

                invalidate --;
            }

        }

        try {
            this.accessSession(securityToken.getId());
            throw new IllegalArgumentException("SSO Session ID already in use : " + securityToken.getId());
        } catch (NoSuchSessionException e) { /* Normal behaviour */ }

        // Build the new session.
        BaseSession session = doMakeNewSession();

        // Configure the new session ...
        session.setId(securityToken.getId()); // We use the securityToken ID as session ID ...!
        session.setCreationTime(System.currentTimeMillis());
        session.setValid(true);
        session.setMaxInactiveInterval(sessionTimeout); // in seconds.
        session.setUsername(username);
        session.setSecurityToken(securityToken);
        session.setLastNode(_node);

        // Store the session
        _store.save(session);

        // Update statistics:

        session.fireSessionEvent(BaseSession.SESSION_CREATED_EVENT, null);

        // Return its id.
        return session.getId();

    }

    /**
     * Gets an SSO session based on its id.
     *
     * @param sessionId the session id previously returned by initiateSession.
     * @throws NoSuchSessionException if the session id is not related to any sso session.
     */
    public SSOSession getSession(String sessionId) throws NoSuchSessionException, SSOSessionException {
        BaseSession s = _store.load(sessionId);
        if (s == null) {
            throw new NoSuchSessionException(sessionId);
        }

        return s;
    }

    /**
     * Gets an SecurityToken based on its SSO Session id
     *
     * @param sessionId the session id previously returned by initiateSession.
     * @throws NoSuchSessionException if the session id is not related to any sso session.
     */
    public SecurityToken getSecurityToken(String sessionId) throws NoSuchSessionException, SSOSessionException {
        SSOSession s = this.getSession(sessionId);
        return s.getSecurityToken();
    }

    /**
     * Gets all SSO sessions.
     */
    public Collection getSessions() throws SSOSessionException {
        return Arrays.asList(_store.loadAll());
    }

    /**
     * Gets an SSO session based on the associated user.
     *
     * @param username the username used when initiating the session.
     * @throws org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException
     *          if the session id is not related to any sso session.
     */
    public Collection getUserSessions(String username) throws NoSuchSessionException, SSOSessionException {
        BaseSession s[] = _store.loadByUsername(username);
        if (s.length < 1) {
            throw new NoSuchSessionException(username);
        }

        // Build the result
        List result = new ArrayList(s.length);
        for (int i = 0; i < s.length; i++) {
            result.add(s[i]);
        }

        return result;

    }

    /**
     * This method accesss the session associated to the received id.
     * This resets the session last access time and updates the access count.
     *
     * @param sessionId the session id previously returned by initiateSession.
     * @throws NoSuchSessionException if the session id is not valid or the session is not valid.
     */
    public void accessSession(String sessionId) throws NoSuchSessionException, SSOSessionException {

        try {

            if (logger.isTraceEnabled())
                logger.trace("[accessSession()] trying session : " + sessionId);

            // getSession will throw a NoSuchSessionException if not found.
            BaseSession s = (BaseSession) getSession(sessionId);
            if (!s.isValid()) {
                if (logger.isDebugEnabled())
                    logger.debug("[accessSession()] invalid session : " + sessionId);
                throw new NoSuchSessionException(sessionId);
            }

            s.access();
            s.setLastNode(_node);

            _store.save(s); // Update session information ...

            if (logger.isTraceEnabled())
                logger.trace("[accessSession()] ok");
        } finally {
            if (logger.isTraceEnabled())
                logger.trace("[accessSession()] ended for session : " + sessionId);


        }

    }

    /**
     * Invlalidates all open sessions.
     */
    public void invalidateAll() throws SSOSessionException {
        BaseSession[] sessions = _store.loadAll();
        for (BaseSession session : sessions) {
            // Mark session as expired (this will notify session listeners, if any)
            session.expire();
        }
    }

    /**
     * Invalidates a session.
     *
     * @param sessionId the session id previously returned by initiateSession.
     * @throws NoSuchSessionException if the session id is not related to any sso session.
     */
    public void invalidate(String sessionId) throws NoSuchSessionException, SSOSessionException {

        // Get current session.
        BaseSession s = (BaseSession) getSession(sessionId);

        // Remove it from the store
        try {
            _store.remove(sessionId);
        } catch(NoSuchSessionException e) {
            logger.trace("Can't remove session from store: " + e.getMessage(), e);
        } catch (SSOSessionException e) {
            logger.warn("Can't remove session from store: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Can't remove session from store\n" + e.getMessage(), e);
        }

        // Mark session as expired (this will notify session listeners, if any)
        s.expire(); // This will invalidate the session ...

    }

    /**
     * Check all sessions and remove those that are not valid from the store.
     * This method is invoked periodically to update sessions state.
     */
    public void checkValidSessions() {

        try {

            //---------------------------------------------
            // Verify invalid sessions ...
            //---------------------------------------------
            BaseSession sessions[] = _store.loadByValid(false);
            if (logger.isTraceEnabled())
                logger.trace("[checkValidSessions()] found " + sessions.length + " invalid sessions");

            checkValidSessions(sessions);

            //---------------------------------------------
            // Verify old sessions ...
            //---------------------------------------------

            // Convert Max Inactive Interval to MS
            long period = _maxInactiveInterval * 60L * 1000L;
            Date from = new Date(System.currentTimeMillis() - period);
            sessions = _store.loadByLastAccessTime(from);
            if (logger.isTraceEnabled())
                logger.trace("[checkValidSessions()] found " + sessions.length + " sessions last accessed before " + from);

            checkValidSessions(sessions);

        } catch (Exception e) {
            logger.error("Can't process expired sessions : " + e.getMessage(), e);
        }

    }

    public void checkValidSessions(BaseSession[] sessions) {
        for (int i = 0; i < sessions.length; i++) {
            try {

                // Ignore valid sessions, they have not expired yet.
                BaseSession session = sessions[i];

                // Only expire sessions handled by this node
                if (_node != null) {
                    // TODO: This doesn't work. The last node property value is never set (i.e. null)
                    String lastNode = session.getLastNode();
                    if (lastNode != null && !_node.equals(lastNode)) {
                        logger.trace("Session " + session.getId() + " is not handled by this node (" + _node + "/" + lastNode + ")");
                        continue;
                    }
                }

                if (!session.isValid()) {
                    // Remove invalid session from the store.
                    _store.remove(session.getId());

                    if (logger.isTraceEnabled())
                        logger.trace("[checkValidSessions()] Session expired : " + session.getId());
                } else {
                    logger.warn("Cannot remove session " + session.getId() + " from store as it's still valid");
                }


            } catch (Exception e) {
                logger.warn("Can't remove session [" + i + "]; " + e.getMessage() != null ? e.getMessage() : e.toString(), e);
            }
        }

    }

    /**
     * @org.apache.xbean.Property alias="session-store"
     * @param ss
     */
    public void setSessionStore(SessionStore ss) {
        _store = ss; // todo :  grab statistics ?!
    }

    @Override
    public SessionStore getSessionStore() {
        return _store;
    }

    /**
     * Dependency Injection of Session Id Generator.
     *
     * @org.apache.xbean.Property alias="session-id-generator"
     */
    public void setSessionIdGenerator(SessionIdGenerator g) {
        _idGen = g;
    }

    /**
     * Number of sessions registered in the manager.
     *
     * @return the number of sessions registered in this manager.
     */
    public int getSessionCount() throws SSOSessionException {
        return _store.getSize();
    }

    // ---------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------

    public int getMaxInactiveInterval() {
        return _maxInactiveInterval;
    }

    /**
     * @param maxInactiveInterval in minutes
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        _maxInactiveInterval = maxInactiveInterval;
    }

    public int getMaxSessionsPerUser() {
        return _maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(int maxSessionsPerUser) {
        _maxSessionsPerUser = maxSessionsPerUser;
    }

    public boolean isInvalidateExceedingSessions() {
        return _invalidateExceedingSessions;
    }

    public void setInvalidateExceedingSessions(boolean invalidateExceedingSessions) {
        _invalidateExceedingSessions = invalidateExceedingSessions;
    }

    public long getSessionMonitorInterval() {
        return _sessionMonitorInterval;
    }

    public void setSessionMonitorInterval(long sessionMonitorInterval) {
        _sessionMonitorInterval = sessionMonitorInterval;
        if (_monitor != null) {
            _monitor.setInterval(_sessionMonitorInterval);
        }

    }

    public SSOSessionStats getStats() {
        return _stats;
    }

    public void setStats(SSOSessionStats _stats) {
        this._stats = _stats;
    }


    public SSOSessionMonitor getMonitor() {
        return _monitor;
    }

    public void setMonitor(SSOSessionMonitor monitor) {
        _monitor = monitor;
    }

    public AuditingServer getAuditingServer() {
        return _aServer;
    }

    public void setAuditingServer(AuditingServer _aServer) {
        this._aServer = _aServer;
    }

    public String getAuditCategory() {
        return _auditCategory;
    }

    public void setAuditCategory(String _auditCategory) {
        this._auditCategory = _auditCategory;
    }

    // ---------------------------------------------------------------
    // Some stats
    // ---------------------------------------------------------------

    public long getStatsMaxSessions() {
        if (_stats != null)
            return _stats.getMaxSessions();

        return -1;
    }

    public long getStatsCreatedSessions() {
        if (_stats != null)
            return _stats.getCreatedSessions();

        return -1;
    }

    public long getStatsDestroyedSessions() {
        if (_stats != null)
            return _stats.getDestroyedSessions();

        return -1;
    }

    public long getStatsCurrentSessions() {
        if (_stats != null) {

            if (_initStats) {
                try {
                    _stats.init(_store.getSize());
                    _initStats = false;
                } catch (SSOSessionException e) {
                    logger.warn(e.getMessage());
                }
            }
            return _stats.getCurrentSessions();
        }

        return -1;
    }


    // ---------------------------------------------------------------
    // Protected utils.
    // ---------------------------------------------------------------

    /**
     * Get new session class to be used in the doLoad() method.
     */
    protected BaseSession doMakeNewSession() {
        return new BaseSessionImpl();
    }

    // ---------------------------------------------------------------
    // To expire threads periodically,
    // ---------------------------------------------------------------


}
