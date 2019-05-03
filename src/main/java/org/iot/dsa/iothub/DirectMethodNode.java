package org.iot.dsa.iothub;

import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.AbstractSubscribeHandler;
import org.iot.dsa.dslink.requester.SimpleInvokeHandler;
import org.iot.dsa.dslink.requester.SimpleRequestHandler;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSInt;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.node.DSString;
import org.iot.dsa.time.DSDateTime;

/**
 * An instance of this node This node represents a direct method of a local device. The IoT Hub that
 * the device is registered in can invoke this method. Whenever this happens, details of the
 * invocation will be stored by this node. If this node has an associated path to a DSA action, then
 * this will also cause that action to be invoked.
 *
 * @author Daniel Shapiro
 */
public class DirectMethodNode extends DSNode {

    static final int METHOD_SUCCESS = 200;
    static final int METHOD_NOT_DEFINED = 404;
    static final int METHOD_FAILED = 500;
    static final int METHOD_NOT_IMPLEMENTED = 501;
    @SuppressWarnings("serial")
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") {
        public Date parse(String source, ParsePosition pos) {
            return super.parse(source.replaceFirst(":(?=[0-9]{2}$)", ""), pos);
        }
    };
    private DSList invokeList = new DSList();
    private DSInfo invokes;
    private String methodName;
    private String path;
    private DSAMethod dsaMethod;

    public DirectMethodNode() {
    }

    public DirectMethodNode(String methodName, String path, DSAMethod dsaMethod) {
        this.methodName = methodName;
        this.path = path;
        this.dsaMethod = dsaMethod;
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
            final DSList results;
            final String thepath = formatPath(parameters);
            DSIRequester requester = MainNode.getRequester();
//            final DirectMethodHandler handler;
            try {
                switch(dsaMethod) {
                    case GET:
                        GetHandler ghandler = new GetHandler();
                        requester.subscribe(thepath, DSInt.valueOf(0), ghandler);
                        results = ghandler.getUpdate(5000);
                        break;
                    case INVOKE:
                        SimpleInvokeHandler ihandler = new SimpleInvokeHandler();
                        requester.invoke(thepath, parameters, ihandler);
                        results = ihandler.getUpdate(5000);
                        break;
                    case SET:
                        SimpleRequestHandler shandler = new SimpleRequestHandler();
                        requester.set(thepath, parameters.get("Value"), shandler);
                        results = new DSList();
                        break;
                    default:
                        results = new DSList();
                }
            } catch (Exception e) {
                return new DeviceMethodData(METHOD_FAILED, e.getMessage());
            }
            
            return new DeviceMethodData(METHOD_SUCCESS, results.toString());
        } else {
            return new DeviceMethodData(METHOD_SUCCESS, "Success");
        }
    }

    private String formatPath(DSMap parameters) {
        String fpath = path;
        if (parameters != null) {
            for (Entry entry: parameters) {
                String key = entry.getKey();
                String val = entry.getValue().toString();
                fpath = fpath.replaceAll("%" + key + "%", val);
            }
        }
        return fpath;
    }

    public void recordInvoke(DSMap parameters) {
        try {
            invokeList.add(new DSMap().put("Timestamp", dateFormat.format(new Date()))
                                      .put("Parameters", parameters));
            fire(VALUE_CHANGED_EVENT, invokes, null);
        } catch (Exception e) {
            warn(e);
        }
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
        if (dsaMethod == null) {
            DSIObject m = get("DSA Method");
            dsaMethod = m instanceof DSString ? DSAMethod.valueOf(m.toString()) : DSAMethod.INVOKE;
        } else {
            put("DSA Method", DSString.valueOf(dsaMethod)).setReadOnly(true);
        }
        invokes = add("Invocations", invokeList);
        invokes.setTransient(true).setReadOnly(true);
    }

    private class GetHandler extends AbstractSubscribeHandler {
        private DSList updates = null;
        
        @Override
        public void onUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
            synchronized (this) {
                updates = new DSList().add(value);
            }
            getStream().closeStream();
        }
        
        /**
         * The next available update, or null for actions return void.
         * Will wait for an update if one isn't available.  Will return all updates before
         * throwing any exceptions.
         *
         * @param timeout How long to wait for an update or the stream to close.
         * @return Null if the action doesn't return anything.
         * @throws RuntimeException if there is a timeout, or if there are any errors.
         */
        public DSList getUpdate(long timeout) {
            long end = System.currentTimeMillis() + timeout;
            synchronized (this) {
                while (updates == null) {
                    try {
                        wait(timeout);
                    } catch (Exception expected) {
                    }
                    if (System.currentTimeMillis() > end) {
                        break;
                    }
                }
                if (isError()) {
                    throw getError();
                }
                if (updates != null) {
                    return updates;
                }
                if (System.currentTimeMillis() > end) {
                    throw new IllegalStateException("Get timed out");
                }
                return null;
            }
        }
        
    }

}
