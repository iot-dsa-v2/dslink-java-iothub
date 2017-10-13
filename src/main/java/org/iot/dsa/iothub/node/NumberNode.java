package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a numeric value.
 *
 * @author Daniel Shapiro
 */
public abstract class NumberNode extends ValueNode {

    @Override
    public DSValueType getValueType() {
        return DSValueType.NUMBER;
    }

}
