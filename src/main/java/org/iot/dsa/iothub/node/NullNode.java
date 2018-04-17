package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSNull;

/**
 * A node that is also a Null value.
 *
 * @author Daniel Shapiro
 */
public class NullNode extends ValueNode {

    public void updateValue(DSNull value) {
        super.updateValue(value);
    }

    @Override
    public Object getObject() {
        return null;
    }

    @Override
    protected DSIValue getNullValue() {
        return DSNull.NULL;
    }

}
