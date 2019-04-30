package org.iot.dsa.iothub;

import org.iot.dsa.dslink.restadapter.ResponseWrapper;
import org.iot.dsa.dslink.restadapter.SubscriptionRule;
import org.iot.dsa.node.DSMap;

public class D2CRule extends SubscriptionRule {
    
    private LocalDeviceNode deviceNode = null;

    public D2CRule(D2CRuleNode node, String subPath, DSMap messageParameters, String body, double minRefreshRate, double maxRefreshRate,
            int rowNum) {
        super(node, subPath, null, null, messageParameters, body, minRefreshRate, maxRefreshRate, rowNum);
    }
    
    private LocalDeviceNode getDeviceNode() {
        if (deviceNode == null) {
            deviceNode = (LocalDeviceNode) getNode().getAncestor(LocalDeviceNode.class);
        }
        return deviceNode;
    }
    
    @Override
    protected ResponseWrapper doSend(DSMap properties, String body) {
        ResponseWrapper resp = getDeviceNode().doSendD2C(properties, body, true);
        getNode().responseRecieved(resp, rowNum);
        return resp;
    }

}
