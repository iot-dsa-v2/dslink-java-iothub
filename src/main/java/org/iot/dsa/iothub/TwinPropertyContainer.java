package org.iot.dsa.iothub;

import org.iot.dsa.node.DSInfo;

/**
 * Represents a node that holds Device Twin Properties or Tags as children.
 *
 * @author Daniel Shapiro
 */
public interface TwinPropertyContainer {
	
	public void onChange(DSInfo info);
	
	public void onDelete(DSInfo info);

}
