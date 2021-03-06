package org.iot.dsa.iothub.node;

import org.iot.dsa.iothub.TwinProperty;
import org.iot.dsa.iothub.TwinPropertyContainer;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSValueType;

/**
 * The base class for a node that is also a value, used to represent Device Twin Properties and
 * Tags.
 *
 * @author Daniel Shapiro
 */
public abstract class ValueNode extends DSNode implements DSIValue, TwinProperty {

    private DSInfo valueInfo = getInfo("Value");

    public abstract Object getObject();

    @Override
    public DSValueType getValueType() {
        return toElement().getValueType();
    }

    @Override
    public boolean isNull() {
        return toElement().isNull();
    }

    /**
     * This fires the NODE_CHANGED topic when the value child changes.  Overrides should call
     * super.onChildChanged.
     */
    @Override
    public void onChildChanged(DSInfo child) {
        if (child == valueInfo) {
            fire(VALUE_CHANGED_EVENT, null, null);
        }
    }

    public void onSet(DSIValue value) {
        updateValue(value);
    }

    @Override
    public DSElement toElement() {
        return valueInfo.getValue().toElement();
    }

    public void updateValue(DSIValue value) {
        put(valueInfo, value);
    }

    @Override
    public DSIValue valueOf(DSElement element) {
        return valueInfo.getValue().valueOf(element);
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Value", getNullValue()).setReadOnly(true).setPrivate(true);
    }

    protected abstract DSIValue getNullValue();

    @Override
    protected void onRemoved() {
        super.onRemoved();
        DSNode parent = getParent();
        if (parent instanceof TwinPropertyContainer) {
            ((TwinPropertyContainer) parent).onDelete(getInfo());
        }
    }

}
