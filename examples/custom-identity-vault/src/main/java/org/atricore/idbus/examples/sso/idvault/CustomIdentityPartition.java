package org.atricore.idbus.examples.sso.idvault;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.common.support.services.IdentityServiceLifecycle;
import org.atricore.idbus.kernel.main.authn.SecurityToken;
import org.atricore.idbus.kernel.main.provisioning.domain.*;
import org.atricore.idbus.kernel.main.provisioning.exception.ProvisioningException;
import org.atricore.idbus.kernel.main.provisioning.impl.AbstractIdentityPartition;
import org.atricore.idbus.kernel.main.provisioning.spi.SchemaManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.List;

/**
 * Created by sgonzalez on 3/16/15.
 */
public class CustomIdentityPartition extends AbstractIdentityPartition
        implements InitializingBean,
        DisposableBean,
        IdentityServiceLifecycle {

    private static final Log logger = LogFactory.getLog(CustomIdentityPartition.class);

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public Group findGroupById(String id) throws ProvisioningException {
        return null;
    }

    @Override
    public Group findGroupByName(String name) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<Group> findGroupsByUserName(String name) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<Group> findAllGroups() throws ProvisioningException {
        return null;
    }

    @Override
    public Group addGroup(Group group) throws ProvisioningException {
        return null;
    }

    @Override
    public Group updateGroup(Group group) throws ProvisioningException {
        return null;
    }

    @Override
    public void deleteGroup(String id) throws ProvisioningException {

    }

    @Override
    public User addUser(User user) throws ProvisioningException {
        return null;
    }

    @Override
    public List<User> addUsers(List<User> users) throws ProvisioningException {
        return null;
    }

    @Override
    public void deleteUser(String id) throws ProvisioningException {

    }

    @Override
    public void deleteUsers(List<User> users) throws ProvisioningException {

    }

    @Override
    public User findUserById(String id) throws ProvisioningException {
        return null;
    }

    @Override
    public User findUserByUserName(String username) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<User> findAllUsers() throws ProvisioningException {
        return null;
    }

    @Override
    public User updateUser(User user) throws ProvisioningException {
        return null;
    }

    @Override
    public List<User> updateUsers(List<User> users) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<User> getUsersByGroup(Group group) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<User> findUsers(UserSearchCriteria searchCriteria, long fromResult, long resultCount, String sortColumn, boolean sortAscending) throws ProvisioningException {
        return null;
    }

    @Override
    public Long findUsersCount(UserSearchCriteria searchCriteria) throws ProvisioningException {
        return null;
    }

    @Override
    public Collection<String> findUserNames(List<String> usernames) throws ProvisioningException {
        return null;
    }

    @Override
    public void boot() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }


}
