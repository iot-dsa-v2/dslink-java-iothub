package org.iot.dsa.iothub;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.iot.dsa.dslink.DSRequesterInterface;
import org.iot.dsa.dslink.requester.InboundInvokeResponse;
import org.iot.dsa.dslink.requester.OutboundInvokeRequest;
import org.iot.dsa.dslink.requester.OutboundRequest;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.iothub.node.RemovableNode;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.security.DSPermission;

import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;

/**
 * An instance of this node This node represents a direct method of a local device. The IoT Hub that
 * the device is registered in can invoke this method. Whenever this happens, details of the
 * invocation will be stored by this node. If this node has an associated path to a DSA action, then
 * this will also cause that action to be invoked.
 *
 * @author Daniel Shapiro
 */
public class DirectMethodNode extends RemovableNode {
	@SuppressWarnings("serial")
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") { 
	    public Date parse(String source, ParsePosition pos) {    
	        return super.parse(source.replaceFirst(":(?=[0-9]{2}$)",""),pos);
	    }
	};
	static final int METHOD_SUCCESS = 200;
	static final int METHOD_NOT_DEFINED = 404;
	static final int METHOD_FAILED = 500;
	static final int METHOD_NOT_IMPLEMENTED = 501;
	
	private String methodName;
	private String path;
	private DSInfo invokes;
	private DSList invokeList = new DSList();
	
	public DirectMethodNode() {
	}
	
	public DirectMethodNode(String methodName, String path) {
		this.methodName = methodName;
		this.path = path;
	}
	
	@Override
	protected void onStable() {
		if (methodName == null) {
			methodName = getName();
		}
		if (path == null) {
			DSIObject p = get("Path");
			path = p instanceof DSString ? p.toString() : "";
		} else {
			put("Path", DSString.valueOf(path)).setReadOnly(true);
		}
		invokes = add("Invocations", invokeList);
		invokes.setTransient(true).setReadOnly(true);
	}

	public DeviceMethodData handle(Object methodData) {
		
		DSMap params = null;
		if (methodData != null) {
			JsonReader reader = null;
			try {
				reader = new JsonReader(new String((byte[]) methodData));
				String s = reader.getElement().toString();
				reader.close();
				reader = new JsonReader(s);
				params = reader.getMap();
			} catch (Exception e) {
			} finally {
				if (reader != null) { 
					reader.close();
				}
			}
		}
		final DSMap parameters = (params != null) ? params : null;
		recordInvoke(parameters);
		if (!path.isEmpty()) {
			final DSList results = new DSList();
			final String thepath = path;
			OutboundRequest req = new OutboundInvokeRequest() {
				
				@Override
				public boolean onResponse(InboundInvokeResponse response) {
					Iterator<DSList> iter = response.getRows();
					synchronized(results) {
						while(iter.hasNext()) {
							results.add(iter.next().copy());
						}
						results.notifyAll();
					}
					return false;
				}
				
				@Override
				public DSPermission getPermission() {
					return null;
				}
				
				@Override
				public String getPath() {
					return thepath;
				}
				
				@Override
				public DSMap getParameters() {
					// TODO Auto-generated method stub
					return parameters;
				}
			};
			try {
				DSRequesterInterface session = RootNode.getRequesterSession();
				session.sendRequest(req);
			} catch (Exception e) {
				return new DeviceMethodData(METHOD_FAILED, e.getMessage());
			}
			
			synchronized(results) {
				try {
					results.wait();
				} catch (InterruptedException e) {
				}
			}
			return new DeviceMethodData(METHOD_SUCCESS, results.toString());
		} else {
			return new DeviceMethodData(METHOD_SUCCESS, "Success");
		}
	}
	
	public void recordInvoke(DSMap parameters) {
		try {
			invokeList.add(new DSMap().put("Timestamp", dateFormat.format(new Date())).put("Parameters", parameters));
			childChanged(invokes);
		} catch (Exception e) {
			warn(e);
		}
	}
	
}
