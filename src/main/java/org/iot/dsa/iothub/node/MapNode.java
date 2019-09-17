package org.iot.dsa.iothub.node;

import org.iot.dsa.iothub.Util;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a Map value.
 *
 * @author Daniel Shapiro
 */
public class MapNode extends ValueNode {

    public void updateValue(DSMap value) {
        super.updateValue(value);
    }

    @Override
    public Object getObject() {
        return Util.dsMapToMap(toElement().toMap());
    }

    @Override
    protected DSIValue getNullValue() {
        return new DSMap();
    }

}
