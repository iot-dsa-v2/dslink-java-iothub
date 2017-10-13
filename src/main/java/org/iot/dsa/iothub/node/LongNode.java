package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSLong;

/**
 * A node that is also a Long value.
 *
 * @author Daniel Shapiro
 */
public class LongNode extends NumberNode {

    public LongNode() {
        setValue(DSLong.NULL);
    }

    public void setValue(DSLong value) {
        super.setValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toLong();
    }

}
