package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSNull;
import org.iot.dsa.node.DSValueType;

/**
 * A node that is also a Null value.
 *
 * @author Daniel Shapiro
 */
public class NullNode extends ValueNode {
	
	public NullNode() {
		setValue(DSNull.NULL);
	}

	@Override
	public DSValueType getValueType() {
		return null;
	}
	
	public void setValue(DSNull value) {
		super.setValue(value);
	}

	@Override
	public Object getObject() {
		return null;
	}

}
