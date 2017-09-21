package org.iot.dsa.iothub;

import org.iot.dsa.node.DSInfo;

public interface TwinPropertyContainer {
	
	public void onChange(DSInfo info);
	
	public void onDelete(DSInfo info);

}
