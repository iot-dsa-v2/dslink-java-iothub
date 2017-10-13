package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSDouble;

/**
 * A node that is also a Double value.
 *
 * @author Daniel Shapiro
 */
public class DoubleNode extends NumberNode {

    public DoubleNode() {
        setValue(DSDouble.valueOf(0));
    }

    public void setValue(DSDouble value) {
        super.setValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toDouble();
    }

}
