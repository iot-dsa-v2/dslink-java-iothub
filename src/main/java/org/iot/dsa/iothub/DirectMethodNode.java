package org.iot.dsa.iothub;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.AbstractInvokeHandler;
import org.iot.dsa.dslink.requester.OutboundInvokeHandler;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.iothub.node.RemovableNode;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.event.DSValueTopic;
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
            return super.parse(source.replaceFirst(":(?=[0-9]{2}$)", ""), pos);
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

    public DirectMethodNode() {}

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
            final OutboundInvokeHandler handler = new AbstractInvokeHandler() {
                @Override
                public void onError(String type, String msg, String detail) {
                }
                
                @Override
                public void onClose() {
                    
                }
                
                @Override
                public void onUpdate(DSList row) {
                    synchronized (results) {
                        results.add(row);
                    }
                }
                
                @Override
                public void onTableMeta(DSMap map) {                    
                }
                
                @Override
                public void onReplace(int start, int end, DSList rows) {
                }
                
                @Override
                public void onMode(Mode mode) {
                    synchronized (results) {
                        results.notifyAll();
                    }
                    getStream().closeStream();
                }
                
                @Override
                public void onInsert(int index, DSList rows) {                    
                }
                
                @Override
                public void onColumns(DSList list) {
                }
            };
            try {
                DSIRequester requester = MainNode.getRequester();
                requester.invoke(thepath, parameters, handler);
            } catch (Exception e) {
                return new DeviceMethodData(METHOD_FAILED, e.getMessage());
            }

            synchronized (results) {
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
            invokeList.add(new DSMap().put("Timestamp", dateFormat.format(new Date()))
                    .put("Parameters", parameters));
            fire(VALUE_TOPIC, DSValueTopic.Event.CHILD_CHANGED, invokes);
        } catch (Exception e) {
            warn(e);
        }
    }

}
