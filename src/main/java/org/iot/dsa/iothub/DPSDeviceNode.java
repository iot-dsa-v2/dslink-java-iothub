package org.iot.dsa.iothub;

import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

public abstract class DPSDeviceNode extends AbstractLocalDeviceNode {
    protected String scopeId;
    protected String endpoint;
    protected String regId;
    
    public DPSDeviceNode() {
        super();
    }
    
    public DPSDeviceNode(String deviceId, IotHubClientProtocol protocol, String scopeId, String endpoint, String regId) {
        super(deviceId, protocol);
        this.scopeId = scopeId;
        this.endpoint = endpoint;
        this.regId = regId;
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        if (scopeId == null || scopeId.isEmpty()) {
            DSIObject sid = get("Scope ID");
            scopeId = sid instanceof DSString ? sid.toString() : null;
        }
        if (endpoint == null || endpoint.isEmpty()) {
            DSIObject endpt = get("Provisioning Service Endpoint");
            endpoint = endpt instanceof DSString ? endpt.toString() : null;
        }
        if (regId == null || regId.isEmpty()) {
            DSIObject rid = get("Registration ID");
            regId = rid instanceof DSString ? rid.toString() : null;
        }
    }
    
    @Override
    protected void storeConfigs() {
        super.storeConfigs();
        if (scopeId != null) {
            put("Scope ID", DSString.valueOf(scopeId)).setReadOnly(true);
        }
        if (endpoint != null) {
            put("Provisioning Service Endpoint", DSString.valueOf(endpoint)).setReadOnly(true);
        }
        if (regId != null) {
            put("Registration ID", DSString.valueOf(regId)).setReadOnly(true);
        }
    }
    
    @Override
    protected DSAction makeEditAction() {
        DSAction act = super.makeEditAction();
        act.addDefaultParameter("Scope ID", scopeId != null ? DSString.valueOf(scopeId) : DSString.EMPTY, null);
        act.addDefaultParameter("Provisioning Service Endpoint", endpoint != null ? DSString.valueOf(endpoint) : DSString.EMPTY, null);
        act.addDefaultParameter("Registration ID", regId != null ? DSString.valueOf(regId) : DSString.EMPTY, null);
        return act;
    }
    
    @Override
    protected void editSpecifics(DSMap parameters) {
        scopeId = parameters.getString("Scope ID");
        endpoint = parameters.getString("Provisioning Service Endpoint");
        regId = parameters.getString("Registration ID");
    }

}
