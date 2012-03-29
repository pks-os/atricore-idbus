package com.atricore.idbus.console.settings.main.impl;

import com.atricore.idbus.console.settings.main.spi.PersistenceServiceConfiguration;
import com.atricore.idbus.console.settings.main.spi.ServiceConfigurationException;
import com.atricore.idbus.console.settings.main.spi.ServiceType;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class ConsolePersistenceServiceConfigurationHandler extends OsgiServiceConfigurationHandler<PersistenceServiceConfiguration> {

    public ConsolePersistenceServiceConfigurationHandler() {
        super("com.atricore.idbus.console.db");
    }

    public boolean canHandle(ServiceType type) {
        return type.equals(ServiceType.PERSISTENCE);
    }

    public PersistenceServiceConfiguration loadConfiguration(ServiceType type) throws ServiceConfigurationException {
        // THis is a write only handler, DO NO  implement this
        return null;
    }

    public void storeConfiguration(PersistenceServiceConfiguration config) throws ServiceConfigurationException {
        try {
            // Some service validations:

            // DB Port
            if (config.getPort() != null) {
                int port = config.getPort();
                if (port < 1 && port > 65535)
                    throw new ServiceConfigurationException("Invalid DB Port value " + port);
            } else {
                throw new ServiceConfigurationException("Invalid DB Port value null");
            }

            // DB Username
            if (config.getUsername() == null)
                throw new ServiceConfigurationException("Invalid DB Username value null");

            // DB Password
            if (config.getPassword() == null)
                throw new ServiceConfigurationException("Invalid DB Password value null");

            Dictionary<String, String> d = toDictionary(config);
            updateProperties(d);
        } catch (IOException e) {
            throw new ServiceConfigurationException("Error storing Persistence configuration properties " + e.getMessage(), e);
        }
    }

    protected Dictionary<String, String> toDictionary(PersistenceServiceConfiguration config) {
        Dictionary<String, String> d = new Hashtable<String, String>();

        if (config.getPort() != null)
            d.put("jdbc.ConnectionURL", "jdbc:derby://localhost:" + config.getPort() + "/atricore-console;create=true");

        if (config.getUsername() != null)
            d.put("jdbc.ConnectionUserName", config.getUsername());

        if (config.getPort() != null)
            d.put("jdbc.ConnectionPassword", config.getPassword());

        return d;
    }
}
