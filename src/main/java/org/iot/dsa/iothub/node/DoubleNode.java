package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSDouble;
import org.iot.dsa.node.DSIValue;

/**
 * A node that is also a Double value.
 *
 * @author Daniel Shapiro
 */
public class DoubleNode extends NumberNode {

    @Override
    public Object getObject() {
        return toElement().toDouble();
    }

    public void updateValue(DSDouble value) {
        super.updateValue(value);
    }

    @Override
    protected DSIValue getNullValue() {
        return DSDouble.valueOf(0);
    }

}
