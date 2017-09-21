package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSLong;

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
