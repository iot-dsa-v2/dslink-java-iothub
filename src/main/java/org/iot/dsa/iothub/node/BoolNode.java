package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a Boolean value.
 *
 * @author Daniel Shapiro
 */
public class BoolNode extends ValueNode {

    @Override
    public Object getObject() {
        return toElement().toBoolean();
    }

    public void updateValue(DSBool value) {
        super.updateValue(value);
    }

    @Override
    protected DSIValue getNullValue() {
        return DSBool.NULL;
    }

}
