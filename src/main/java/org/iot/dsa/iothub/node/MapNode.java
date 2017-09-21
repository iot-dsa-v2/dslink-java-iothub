package org.iot.dsa.iothub.node;

import org.iot.dsa.iothub.Util;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSValueType;

public class MapNode extends ValueNode {
	
	public MapNode() {
		setValue(new DSMap());
	}

	@Override
	public DSValueType getValueType() {
		return DSValueType.MAP;
	}
	
	public void setValue(DSMap value) {
		super.setValue(value);
	}

	@Override
	public Object getObject() {
		return Util.dsMapToMap(toElement().toMap());
	}

}
