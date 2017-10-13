package org.iot.dsa.iothub;

import org.iot.dsa.node.DSIObject;

/**
 * Represents a Device Twin Property or Tag.
 *
 * @author Daniel Shapiro
 */
public interface TwinProperty extends DSIObject {

    /**
     * @return My value as a standard java object.
     */
    public Object getObject();

}
