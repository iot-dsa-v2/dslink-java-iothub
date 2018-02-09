package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a List value.
 *
 * @author Daniel Shapiro
 */
public class ListNode extends ValueNode {

    @Override
    public DSValueType getValueType() {
        return DSValueType.LIST;
    }

    public void updateValue(DSList value) {
        super.updateValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toList();
    }

    @Override
    protected DSIValue getNullValue() {
        return new DSList();
    }

}
