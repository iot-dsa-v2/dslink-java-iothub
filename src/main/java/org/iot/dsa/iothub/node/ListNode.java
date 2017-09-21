package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSValueType;

public class ListNode extends ValueNode {
	
	public ListNode() {
		setValue(new DSList());
	}

	@Override
	public DSValueType getValueType() {
		return DSValueType.LIST;
	}
	
	public void setValue(DSList value) {
		super.setValue(value);
	}

	@Override
	public Object getObject() {
		return toElement().toList();
	}

}
