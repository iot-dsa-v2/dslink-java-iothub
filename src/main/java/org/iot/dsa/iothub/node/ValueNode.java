package org.iot.dsa.iothub.node;

import org.iot.dsa.iothub.TwinProperty;
import org.iot.dsa.iothub.TwinPropertyContainer;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.event.DSValueTopic.Event;

/**
 * The base class for a node that is also a value, used to represent Device Twin Properties and
 * Tags.
 *
 * @author Daniel Shapiro
 */
public abstract class ValueNode extends RemovableNode implements DSIValue, TwinProperty {

    private DSInfo valueInfo = getInfo("Value");
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Value", getNullValue()).setReadOnly(true).setHidden(true);
    }
    
    protected abstract DSIValue getNullValue();

    /**
     * This fires the NODE_CHANGED topic when the value child changes.  Overrides should call
     * super.onChildChanged.
     */
    @Override
    public void onChildChanged(DSInfo child) {
        if (child == valueInfo) {
            fire(VALUE_TOPIC, Event.NODE_CHANGED, null);
        }
    }
    
    @Override
    public DSValueType getValueType() {
        return valueInfo.getValue().getValueType();
    }


    @Override
    public DSElement toElement() {
        return valueInfo.getValue().toElement();
    }

    @Override
    public DSIValue valueOf(DSElement element) {
        return valueInfo.getValue().valueOf(element);
    }


    public void updateValue(DSIValue value) {
        put(valueInfo, value);
    }

    public void onSet(DSIValue value) {
        updateValue(value);
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
