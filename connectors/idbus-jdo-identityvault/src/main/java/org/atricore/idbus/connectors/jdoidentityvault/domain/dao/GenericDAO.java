package org.atricore.idbus.connectors.jdoidentityvault.domain.dao;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public interface GenericDAO<T, PK> {

    boolean exists(PK id);

    T findById(PK id);

    Collection<T> findAll();

    T save(T object);

    Collection<T> saveAll(Collection<T> objects);

    void delete(PK id);

    T detachCopy(T object, int fetchDepth);

    Collection<T> detachCopyAll(Collection<T> objects, int fetchDepth);

    void flush();
}
