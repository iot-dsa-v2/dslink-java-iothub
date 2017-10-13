package org.iot.dsa.iothub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.iot.dsa.iothub.node.BoolNode;
import org.iot.dsa.iothub.node.DoubleNode;
import org.iot.dsa.iothub.node.ListNode;
import org.iot.dsa.iothub.node.RemovableNode;
import org.iot.dsa.iothub.node.StringNode;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

/**
 * A node that represents a Device Twin Property or Tag whose value is a map.
 *
 * @author Daniel Shapiro
 */
public class TwinPropertyNode extends RemovableNode implements TwinProperty, TwinPropertyContainer {

    private Set<String> nulls = new HashSet<String>();

    public TwinPropertyNode() {

    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Add", makeAddAction());
    }

    @Override
    public void delete() {
        DSNode parent = getParent();
        if (parent instanceof TwinPropertyContainer) {
            ((TwinPropertyContainer) parent).onDelete(getInfo());
        }
        super.delete();
    }

    @Override
    protected void onChildChanged(DSInfo info) {
        onChange(info);
    }

    @Override
    public void onDelete(DSInfo info) {
        nulls.add(info.getName());
        onChange(info);
    }

    @Override
    public void onChange(DSInfo info) {
        if (info.isAction()) {
            return;
        }
        ((TwinPropertyContainer) getParent()).onChange(getInfo());
    }

    private static DSAction makeAddAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((TwinPropertyNode) info.getParent()).invokeAdd(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Value Type", DSFlexEnum.valueOf("String", Util.getSimpleValueTypes()),
                null);
        return act;
    }

    private void invokeAdd(DSMap parameters) {
        String name = parameters.getString("Name");
        String vt = parameters.getString("Value Type");
        handleAdd(name, vt);
    }

    public void handleAdd(String name, String type) {
        TwinProperty vn;
        switch (type.charAt(0)) {
            case 'S':
                vn = new StringNode();
                break;
            case 'N':
                vn = new DoubleNode();
                break;
            case 'B':
                vn = new BoolNode();
                break;
            case 'L':
                vn = new ListNode();
                break;
            case 'M':
                vn = new TwinPropertyNode();
                break;
            default:
                vn = null;
                break;
        }
        if (vn != null) {
            put(name, vn);
            if (vn instanceof DSNode) {
                onChange(((DSNode) vn).getInfo());
            }
        }
    }

    @Override
    public Object getObject() {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String name : nulls) {
            map.put(name, null);
        }
        for (DSInfo info : this) {
            if (!info.isAction()) {
                String name = info.getName();
                DSIObject value = info.getObject();
                if (value instanceof TwinProperty) {
                    map.put(name, ((TwinProperty) value).getObject());
                }
            }
        }
        return map;
    }
}
