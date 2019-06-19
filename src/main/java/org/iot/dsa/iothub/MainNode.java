package org.iot.dsa.iothub;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.util.DSException;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

/**
 * This is the root node of the link.
 *
 * @author Daniel Shapiro
 */
public class MainNode extends DSMainNode {

    private static final Object requesterLock = new Object();
    private static DSIRequester requester;

    public static DSIRequester getRequester() {
        synchronized (requesterLock) {
            while (requester == null) {
                try {
                    requesterLock.wait();
                } catch (InterruptedException e) {
                    DSException.throwRuntime(e);
                }
            }
            return requester;
        }
    }

    public static void setRequester(DSIRequester requester) {
        synchronized (requesterLock) {
            MainNode.requester = requester;
            requesterLock.notifyAll();
        }
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Docs", DSString.valueOf(
                "https://github.com/iot-dsa-v2/dslink-java-v2-iothub/blob/develop/README.md"))
                .setTransient(true).setReadOnly(true);
        
        declareDefault("Add Device by Connection String", makeAddDeviceByConnStrAction());
        //declareDefault("Add Device by DPS", makeAddDeviceByDPSAction());
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        getLink().getConnection().subscribe(((event, node, child, data) -> {
            if (event.equals(DSLinkConnection.CONNECTED_EVENT)) {
                MainNode.setRequester(getLink().getConnection().getRequester());
            }
        }));
    }
    
    private static DSAction makeAddDeviceByConnStrAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addDeviceByConnStr(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Connection String", DSValueType.STRING, null);
        act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubClientProtocol.MQTT), null);
        return act;
    }
    
    private void addDeviceByConnStr(DSMap parameters) {
        String connStr = parameters.getString("Connection String"); 
        String id = Util.getFromConnString(connStr, "DeviceId");
        if (id == null) {
            DSException.throwRuntime(new IllegalArgumentException("Device Connection String missing Device ID"));
        }
        String protocolStr = parameters.getString("Protocol");
        IotHubClientProtocol protocol = IotHubClientProtocol.valueOf(protocolStr);
        add(id, new LocalDeviceNode(id, protocol, connStr));
    }
}
