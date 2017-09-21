package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSValueType;

public abstract class NumberNode extends ValueNode {

	@Override
	public DSValueType getValueType() {
		// TODO Auto-generated method stub
		return DSValueType.NUMBER;
	}

}
