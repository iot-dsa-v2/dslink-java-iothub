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
        for (int i = 0; i < dsMap.size(); i++) {
            Entry en = dsMap.getEntry(i);
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
            vn.setValue(DSDouble.valueOf(((Number) o).doubleValue()));
            return vn;
        }
        if (o instanceof Boolean) {
            BoolNode vn = new BoolNode();
            vn.setValue(DSBool.valueOf(((Boolean) o).booleanValue()));
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
        vn.setValue(DSString.valueOf(o.toString()));
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
}
