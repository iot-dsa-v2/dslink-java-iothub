package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSString;

/**
 * A node that is also a String value.
 *
 * @author Daniel Shapiro
 */
public class StringNode extends ValueNode {

    @Override
    public Object getObject() {
        return toElement().toString();
    }

    public void updateValue(DSString value) {
        super.updateValue(value);
    }

    @Override
    protected DSIValue getNullValue() {
        return DSString.EMPTY;
    }

}
