package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a Boolean value.
 *
 * @author Daniel Shapiro
 */
public class BoolNode extends ValueNode {
	
	public BoolNode() {
		setValue(DSBool.NULL);
	}
	
	@Override
	public DSValueType getValueType() {
		return DSValueType.BOOL;
	}

	public void setValue(DSBool value) {
		super.setValue(value);
	}

	@Override
	public Object getObject() {
		return toElement().toBoolean();
	}

}
