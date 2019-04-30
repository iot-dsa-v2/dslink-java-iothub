package org.iot.dsa.iothub;

import java.util.HashMap;
import java.util.Map;
import org.iot.dsa.iothub.node.BoolNode;
import org.iot.dsa.iothub.node.DoubleNode;
import org.iot.dsa.iothub.node.StringNode;
import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSDouble;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSMetadata;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;

/**
 * Miscellaneous utility methods.
 *
 * @author Daniel Shapiro
 */
public class Util {

    public static DSMap makeColumn(String name, DSValueType type) {
        return new DSMetadata().setName(name).setType(type).getMap();
    }

    public static Map<String, String> dsMapToMap(DSMap dsMap) {
        Map<String, String> map = new HashMap<String, String>();
        for (Entry en : dsMap) {
            map.put(en.getKey(), en.getValue().toString());
        }
        return map;
    }

    private static DSList simpleVTs;

    public static DSList getSimpleValueTypes() {
        if (simpleVTs == null) {
            simpleVTs = new DSList().add("String").add("Number").add("Bool").add("Map");
        }
        return simpleVTs;
    }

    public static TwinProperty objectToValueNode(Object o) {
        if (o instanceof Number) {
            DoubleNode vn = new DoubleNode();
            vn.updateValue(DSDouble.valueOf(((Number) o).doubleValue()));
            return vn;
        }
        if (o instanceof Boolean) {
            BoolNode vn = new BoolNode();
            vn.updateValue(DSBool.valueOf(((Boolean) o).booleanValue()));
            return vn;
        }
        if (o instanceof Map) {
            TwinPropertyNode vn = new TwinPropertyNode();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                TwinProperty tp = Util.objectToValueNode(e.getValue());
                vn.put(e.getKey(), tp);
            }
            return vn;
        }

        StringNode vn = new StringNode();
        vn.updateValue(DSString.valueOf(o.toString()));
        return vn;
    }

    public static void putInMap(DSMap map, String key, Object value) {
        if (value instanceof Long) {
            map.put(key, (Long) value);
        } else if (value instanceof Integer) {
            map.put(key, (Integer) value);
        } else if (value instanceof Number) {
            map.put(key, ((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            map.put(key, (Boolean) value);
        } else {
            map.put(key, value.toString());
        }
    }
    
    public static String getFromConnString(String connStr, String key) {
        key = key + "=";
        int idx = connStr.indexOf(key);
        if (idx == -1) {
            return null;
        }
        String value = connStr.substring(idx + key.length());
        idx = value.indexOf(';');
        if (idx > -1) {
            value = value.substring(0, idx);
        }
        return value;
    }
    
    public static int iotHubStatusToHttpCode(IotHubStatusCode status) {
        switch (status) {
            case BAD_FORMAT:
                return 400;
            case ERROR:
                return 400;
            case HUB_OR_DEVICE_ID_NOT_FOUND:
                return 404;
            case INTERNAL_SERVER_ERROR:
                return 500;
            case MESSAGE_CANCELLED_ONCLOSE:
                return 400;
            case MESSAGE_EXPIRED:
                return 400;
            case OK:
                return 200;
            case OK_EMPTY:
                return 204;
            case PRECONDITION_FAILED:
                return 412;
            case REQUEST_ENTITY_TOO_LARGE:
                return 413;
            case SERVER_BUSY:
                return 503;
            case THROTTLED:
                return 429;
            case TOO_MANY_DEVICES:
                return 403;
            case UNAUTHORIZED:
                return 401;
            default:
                return 400;   
        }
    }
}
