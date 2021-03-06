package org.atricore.idbus.kernel.main.mediation.state;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public abstract class AbstractLocalState implements LocalState {

    private String id;

    private Map<String, String> alternativeIds;

    public AbstractLocalState(String id) {
        this.id = id;
        this.alternativeIds = Collections.synchronizedMap(new HashMap<String, String>());
    }

    public String getId() {
        return id;
    }

    public void addAlternativeId(String idName, String id) {
        this.alternativeIds.put(idName, id);
    }

    public void removeAlternativeId(String idName) {
        alternativeIds.remove(idName);
    }

    public String getAlternativeId(String idName) {
        return this.alternativeIds.get(idName);
    }

    public Collection<String> getAlternativeIdNames() {
        return alternativeIds.keySet();
    }
}
