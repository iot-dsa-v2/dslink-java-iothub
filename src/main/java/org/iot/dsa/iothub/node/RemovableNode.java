package org.iot.dsa.iothub.node;

import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;


public class RemovableNode extends DSNode {
	
	@Override
	protected void declareDefaults() {
		declareDefault("Remove", makeRemoveAction());
	}

	protected DSAction makeRemoveAction() {
		return new DSAction() {
			@Override
			public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
		        ((RemovableNode)info.getParent()).delete();
		        return null;
		    }
		};
	}
	
	public void delete() {
		getParent().remove(getInfo());
	}
	
}
