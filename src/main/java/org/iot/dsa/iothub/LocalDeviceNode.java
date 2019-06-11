package org.iot.dsa.iothub;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import java.net.URISyntaxException;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;

/**
 * An instance of this node represents a specific local device registered in an Azure IoT Hub.
 *
 * @author Daniel Shapiro
 */
public class LocalDeviceNode extends AbstractLocalDeviceNode {
    private String connectionString;

    public LocalDeviceNode() {
        super();
    }
    
    public LocalDeviceNode(String deviceId, IotHubClientProtocol protocol, String connectionString) {
        super(deviceId, protocol);
        this.connectionString = connectionString;
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        if (connectionString == null || connectionString.isEmpty()) {
            DSIObject cs = get("Connection String");
            connectionString = cs instanceof DSString ? cs.toString() : null;
        }
    }
    
    @Override
    protected void storeConfigs() {
        super.storeConfigs();
        if (connectionString != null) {
            put("Connection String", DSString.valueOf(connectionString)).setReadOnly(true);
        }
    }

    @Override
    protected DSAction makeEditAction() {
        DSAction act = super.makeEditAction();
        act.addDefaultParameter("Connection String", connectionString != null ? DSString.valueOf(connectionString) : DSString.EMPTY, null);
        return act;
    }
    
    @Override
    protected void editSpecifics(DSMap parameters) {
        connectionString = parameters.getString("Connection String");
    }
    
    @Override
    protected DeviceClient newClient() throws IllegalArgumentException, URISyntaxException {
        return new DeviceClient(connectionString, protocol);
    }
}
