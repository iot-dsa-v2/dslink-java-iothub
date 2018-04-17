package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSLong;

/**
 * A node that is also a Long value.
 *
 * @author Daniel Shapiro
 */
public class LongNode extends NumberNode {

    public void updateValue(DSLong value) {
        super.updateValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toLong();
    }

    @Override
    protected DSIValue getNullValue() {
        return DSLong.NULL;
    }

}
