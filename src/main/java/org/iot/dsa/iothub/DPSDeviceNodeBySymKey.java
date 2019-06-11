package org.iot.dsa.iothub;

import java.net.URISyntaxException;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

public class DPSDeviceNodeBySymKey extends DPSDeviceNode {
    private boolean groupEnrollment = false;
    private String symmetricKey;
    
    public DPSDeviceNodeBySymKey() {
        super();
    }
    
    public DPSDeviceNodeBySymKey(String deviceId, IotHubClientProtocol protocol, String scopeId, String endpoint, String regId, 
            boolean groupEnrollment, String symmetricKey) {
        super(deviceId, protocol, scopeId, endpoint, regId);
        this.groupEnrollment = groupEnrollment;
        this.symmetricKey = symmetricKey;
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        DSIObject attestation = get("Key Attestation Type");
        if (attestation instanceof DSString) {
            groupEnrollment = "Group Enrollment".equals(attestation.toString());
        }
        if (symmetricKey == null || symmetricKey.isEmpty()) {
            DSIObject sk = get("Symmetric Key");
            symmetricKey = sk instanceof DSString ? sk.toString() : null;
        }
    }
    
    @Override
    protected void storeConfigs() {
        super.storeConfigs();
        put("Key Attestation Type", DSString.valueOf(groupEnrollment ? "Group Enrollment" : "Individual Enrollment"));
        if (symmetricKey != null) {
            put("Symmetric Key", DSString.valueOf(symmetricKey)).setReadOnly(true);
        }
    }
    
    @Override
    protected DSAction makeEditAction() {
        DSAction act = super.makeEditAction();
        DSList enrollTypes = new DSList().add("Individual Enrollment").add("Group Enrollment");
        act.addDefaultParameter("Key Attestation Type", 
                DSFlexEnum.valueOf(groupEnrollment ? "Group Enrollment" : "Individual Enrollment", enrollTypes), null);
        act.addDefaultParameter("Symmetric Key", symmetricKey != null ? DSString.valueOf(symmetricKey) : DSString.EMPTY, null);
        return act;
    }
    
    @Override
    protected void editSpecifics(DSMap parameters) {
        groupEnrollment = "Group Enrollment".equals(parameters.getString("Key Attestation Type"));
        symmetricKey = parameters.getString("Symmetric Key");
    }

    @Override
    protected DeviceClient newClient() throws IllegalArgumentException, URISyntaxException {
        info("Device ID = " + deviceId);              // these log messages aren't necessary, just to show the available parameters
        info("Protocol = " + protocol.toString());
        info("Scope ID = " + scopeId);
        info("Provisioning Service Endpoint = " + endpoint);
        info("Registration ID = " + regId);
        info("Key Attestation Type = " + (groupEnrollment ? "Group Enrollment" : "Individual Enrollment"));
        info("Symmetric Key = " + symmetricKey);
        
        // TODO create a new DeviceClient instance using the parameters listed above
        return null;
    }

}
