package org.iot.dsa.iothub.node;

import org.iot.dsa.iothub.TwinProperty;
import org.iot.dsa.iothub.TwinPropertyContainer;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSNode;

/**
 * The base class for a node that is also a value, used to represent Device Twin Properties and Tags.
 *
 * @author Daniel Shapiro
 */
public abstract class ValueNode extends RemovableNode implements DSIValue, TwinProperty {
	
	private DSInfo value;

	@Override
	public DSIValue restore(DSElement element) {
		return valueOf(element);
	}

	@Override
	public DSElement store() {
		return toElement();
	}

	@Override
	public DSElement toElement() {
		return value.getValue().toElement();
	}

	@Override
	public DSIValue valueOf(DSElement element) {
		return value.getValue().valueOf(element);
	}
	
	
	protected void setValue(DSElement element) {
		this.value = put("Value", element).setReadOnly(true).setHidden(true);
		if (getParent() != null) {
			getParent().childChanged(getInfo());
		}
	}
	
	public void onSet(DSIValue value) {
		setValue(value.toElement());
	}
	
	@Override
	public void delete() {
		DSNode parent = getParent();
		if (parent instanceof TwinPropertyContainer) {
			((TwinPropertyContainer) parent).onDelete(getInfo());
		}
		super.delete();
	}
	
	public abstract Object getObject();
	
}
