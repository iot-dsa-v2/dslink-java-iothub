package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a String value.
 *
 * @author Daniel Shapiro
 */
public class StringNode extends ValueNode {

    @Override
    public DSValueType getValueType() {
        return DSValueType.STRING;
    }

    public void updateValue(DSString value) {
        super.updateValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toString();
    }

    @Override
    protected DSIValue getNullValue() {
        return DSString.EMPTY;
    }

}
