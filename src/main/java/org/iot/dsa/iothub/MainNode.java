package org.iot.dsa.iothub;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSList;
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
        declareDefault("Via Symmetric Key", makeAddDeviceBySymKeyAction()).getMetadata().setActionGroup("Add Device by DPS", null);
        declareDefault("Via X509", makeAddDeviceByX509Action()).getMetadata().setActionGroup("Add Device by DPS", null);
    }

    @Override
    protected void onStarted() {
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
    
    private DSAction makeAddDeviceBySymKeyAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addDeviceBySymKey(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Device ID", DSValueType.STRING, null);
        act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubClientProtocol.MQTT), null);
        act.addParameter("Scope ID", DSValueType.STRING, null);
        act.addDefaultParameter("Provisioning Service Endpoint", DSString.valueOf("global.azure-devices-provisioning.net"), null);
        act.addParameter("Registration ID", DSValueType.STRING, null);
        DSList enrollTypes = new DSList().add("Individual Enrollment").add("Group Enrollment");
        act.addParameter("Key Attestation Type", DSFlexEnum.valueOf("Individual Enrollment", enrollTypes), null);
        act.addParameter("Symmetric Key", DSValueType.STRING, null);
        return act;
    }
    
    private void addDeviceBySymKey(DSMap parameters) {
        String id = parameters.getString("Device ID");
        String protocolStr = parameters.getString("Protocol");
        IotHubClientProtocol protocol = IotHubClientProtocol.valueOf(protocolStr);
        String scopeId = parameters.getString("Scope ID");
        String endpoint = parameters.getString("Provisioning Service Endpoint");
        String regId = parameters.getString("Registration ID");
        boolean groupEnroll = "Group Enrollment".equals(parameters.getString("Key Attestation Type"));
        String symKey = parameters.getString("Symmetric Key");
        add(id, new DPSDeviceNodeBySymKey(id, protocol, scopeId, endpoint, regId, groupEnroll, symKey));
    }
    
    private DSAction makeAddDeviceByX509Action() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addDeviceByX509(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Device ID", DSValueType.STRING, null);
        act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubClientProtocol.MQTT), null);
        act.addParameter("Scope ID", DSValueType.STRING, null);
        act.addDefaultParameter("Provisioning Service Endpoint", DSString.valueOf("global.azure-devices-provisioning.net"), null);
        act.addParameter("Registration ID", DSValueType.STRING, null);
        act.addParameter("Certificate", DSValueType.STRING, "Either a path to the certificate PEM file, or the PEM text");
        return act;
    }

    protected void addDeviceByX509(DSMap parameters) {
        String id = parameters.getString("Device ID");
        String protocolStr = parameters.getString("Protocol");
        IotHubClientProtocol protocol = IotHubClientProtocol.valueOf(protocolStr);
        String scopeId = parameters.getString("Scope ID");
        String endpoint = parameters.getString("Provisioning Service Endpoint");
        String regId = parameters.getString("Registration ID");
        String cert = parameters.getString("Certificate");
        add(id, new DPSDeviceNodeByX509(id, protocol, scopeId, endpoint, regId, cert));
    }
}
