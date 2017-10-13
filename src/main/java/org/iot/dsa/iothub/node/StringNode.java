package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a String value.
 *
 * @author Daniel Shapiro
 */
public class StringNode extends ValueNode {

    public StringNode() {
        setValue(DSString.EMPTY);
    }

    @Override
    public DSValueType getValueType() {
        return DSValueType.STRING;
    }

    public void setValue(DSString value) {
        super.setValue(value);
    }

    @Override
    public Object getObject() {
        return toElement().toString();
    }

}
