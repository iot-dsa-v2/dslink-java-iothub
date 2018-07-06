package org.iot.dsa.iothub;

import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Device;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.service.DeviceStatus;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.iot.dsa.dslink.DSRequestException;
import org.iot.dsa.iothub.node.BoolNode;
import org.iot.dsa.iothub.node.DoubleNode;
import org.iot.dsa.iothub.node.ListNode;
import org.iot.dsa.iothub.node.RemovableNode;
import org.iot.dsa.iothub.node.StringNode;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.ActionSpec;
import org.iot.dsa.node.action.ActionSpec.ResultType;
import org.iot.dsa.node.action.ActionValues;
import org.iot.dsa.node.action.DSAbstractAction;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.event.DSValueTopic;

/**
 * An instance of this node represents a specific local device registered in an Azure IoT Hub.
 *
 * @author Daniel Shapiro
 */
public class LocalDeviceNode extends RemovableNode {
    private IotHubNode hubNode;
    private String deviceId;
    private String connectionString;
    private IotHubClientProtocol protocol;
    private DSInfo status;
    private DSInfo c2d;
    private DSList c2dList = new DSList();
    private DSNode methodsNode;
    private DSNode desiredNode;
    private ReportedPropsNode reportedNode;

    private DeviceClient client;
    private Device twin;

    public LocalDeviceNode() {}

    public LocalDeviceNode(IotHubNode hubNode, String deviceId, IotHubClientProtocol protocol) {
        this.hubNode = hubNode;
        this.deviceId = deviceId;
        this.protocol = protocol;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Methods", new MethodsNode());
        declareDefault("Desired Properties", new DSNode());
        declareDefault("Reported Properties", new ReportedPropsNode());

        declareDefault("Send D2C Message", makeSendMessageAction());
        declareDefault("Upload File", makeUploadFileAction());
        declareDefault("Refresh", makeRefreshAction());
    }

    public static class MethodsNode extends DSNode {
        @Override
        protected void declareDefaults() {
            declareDefault("Add Direct Method", makeAddMethodAction());
        }
    }

    public static class ReportedPropsNode extends DSNode implements TwinPropertyContainer {
        @Override
        protected void declareDefaults() {
            declareDefault("Add Reported Property", makeAddReportedPropAction());
        }

        @Override
        protected void onChildChanged(DSInfo info) {
            onChange(info);
        }

        @Override
        public void onChange(DSInfo info) {
            if (info.isAction()) {
                return;
            }
            String name = info.getName();
            DSIObject value = info.getObject();
            if (value instanceof TwinProperty) {
                Object object = ((TwinProperty) value).getObject();
                ((LocalDeviceNode) info.getParent().getParent()).setReportedProperty(name, object);
            }
        }

        @Override
        public void onDelete(DSInfo info) {
            // if (info.isAction()) {
            // return;
            // }
            // String name = info.getName();
            // ((LocalDeviceNode) info.getParent().getParent()).setReportedProperty(name, null);
        }

    }

    @Override
    protected void onStarted() {
        if (protocol == null) {
            DSIObject p = get("Protocol");
            protocol = p instanceof DSString ? IotHubClientProtocol.valueOf(p.toString())
                    : IotHubClientProtocol.MQTT;
        }
    }

    @Override
    protected void onStable() {
        status = add("STATUS", DSString.valueOf("Connecting"));
        status.setTransient(true);
        status.setReadOnly(true);

        if (hubNode == null) {
            DSNode n = getParent();
            n = n.getParent();
            if (n instanceof IotHubNode) {
                hubNode = (IotHubNode) n;
            }
        }
        if (deviceId == null) {
            deviceId = getName();
        }

        c2d = add("Cloud-To-Device Messages", c2dList);
        c2d.setTransient(true).setReadOnly(true);
        methodsNode = getNode("Methods");
        desiredNode = getNode("Desired Properties");
        reportedNode = (ReportedPropsNode) getNode("Reported Properties");

        init();
    }

    private void init() {
        put("Protocol", DSString.valueOf(protocol.toString())).setReadOnly(true);
        if (client != null) {
            try {
                client.closeNow();
            } catch (IOException e) {
                warn(e);
            }
        }
        try {
            DeviceStatus deviceStatus = registerDeviceIdentity();
            put(status, DSString.valueOf(deviceStatus.toString()));
        } catch (Exception e) {
            warn("Error getting device identity", e);
            put(status, DSString.valueOf("Error getting device identity: " + e.getMessage()));
        }
        try {
            setupClient();

            HashSet<Property> props = new HashSet<Property>();
            for (DSInfo info : reportedNode) {
                if (!info.isAction()) {
                    String name = info.getName();
                    DSIObject value = info.getObject();
                    if (value instanceof TwinProperty) {
                        Object object = ((TwinProperty) value).getObject();
                        props.add(new Property(name, object));
                    }
                }
            }
            if (!props.isEmpty()) {
                client.sendReportedProperties(props);
            }

        } catch (URISyntaxException | IOException e) {
            warn("Error initializing device client", e);
            put(status, DSString.valueOf("Error initializing device client: " + e.getMessage()));
        }
        put("Edit", makeEditAction()).setTransient(true);
    }

    public void setupClient() throws IOException, URISyntaxException {
        this.client = new DeviceClient(connectionString, protocol);
        MessageCallback callback = new C2DMessageCallback();
        client.setMessageCallback(callback, null);

        client.open();

        client.subscribeToDeviceMethod(new DirectMethodCallback(), null,
                new DirectMethodStatusCallback(), null);

        twin = new Device() {

            @Override
            public void PropertyCall(String propertyKey, Object propertyValue, Object context) {
                desiredNode.put(propertyKey, DSString.valueOf(propertyValue)).setTransient(true)
                        .setReadOnly(true);
            }
        };

        client.startDeviceTwin(new DeviceTwinStatusCallback(), null, twin, null);
        client.subscribeToDesiredProperties(twin.getDesiredProp());
    }

    public DeviceStatus registerDeviceIdentity() throws IOException, JsonSyntaxException,
            IotHubException, IllegalArgumentException, NoSuchAlgorithmException {
        String hubConnStr = hubNode.getConnectionString();
        RegistryManager registryManager = RegistryManager.createFromConnectionString(hubConnStr);

        com.microsoft.azure.sdk.iot.service.Device device =
                com.microsoft.azure.sdk.iot.service.Device.createFromId(deviceId, null, null);
        try {
            device = registryManager.addDevice(device);
        } catch (IotHubException iote) {
            device = registryManager.getDevice(deviceId);
        }

        int idx = hubConnStr.indexOf("HostName=");
        if (idx == -1) {
            throw new IOException("Connection String missing HostName");
        }
        String hostName = hubConnStr.substring(idx + 9);
        idx = hostName.indexOf(';');
        if (idx > -1) {
            hostName = hostName.substring(0, idx);
        }
        String deviceKey = device.getPrimaryKey();
        connectionString =
                "HostName=" + hostName + ";DeviceId=" + deviceId + ";SharedAccessKey=" + deviceKey;
        return device.getStatus();
    }

    private DSAction makeRefreshAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((LocalDeviceNode) info.getParent()).init();
                return null;
            }
        };
        return act;
    }

    private DSAction makeEditAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((LocalDeviceNode) info.getParent()).edit(invocation.getParameters());
                return null;
            }
        };
        act.addDefaultParameter("Protocol", DSJavaEnum.valueOf(protocol), null);
        return act;
    }

    protected void edit(DSMap parameters) {
        String protocolStr = parameters.getString("Protocol");
        protocol = IotHubClientProtocol.valueOf(protocolStr);
        init();
    }

    private static DSAction makeAddMethodAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((LocalDeviceNode) info.getParent().getParent())
                        .addDirectMethod(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Method Name", DSValueType.STRING, null);
        act.addDefaultParameter("Path", DSString.EMPTY, null);
        return act;
    }

    private DSAction makeUploadFileAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                return ((LocalDeviceNode) info.getParent()).uploadFile(info,
                        invocation.getParameters());
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Filepath", DSValueType.STRING, null).setPlaceHolder("myImage.png");
        act.setResultType(ResultType.VALUES);
        act.addValueResult("Response Status", DSValueType.STRING);
        return act;
    }

    private DSAction makeSendMessageAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                return ((LocalDeviceNode) info.getParent()).sendD2CMessage(info,
                        invocation.getParameters());
            }
        };
        act.addParameter("Message", DSValueType.STRING, null);
        act.addDefaultParameter("Properties", new DSMap(), null);
        act.setResultType(ResultType.VALUES);
        act.addValueResult("Response Status", DSValueType.STRING);
        return act;
    }

    private static DSAction makeAddReportedPropAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((LocalDeviceNode) info.getParent().getParent())
                        .addReportedProp(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Value Type", DSFlexEnum.valueOf("String", Util.getSimpleValueTypes()),
                null);
        return act;
    }

    private void addReportedProp(DSMap parameters) {
        String name = parameters.getString("Name");
        String vt = parameters.getString("Value Type");
        TwinProperty vn;
        switch (vt.charAt(0)) {
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
            reportedNode.put(name, vn);
            if (vn instanceof DSNode) {
                reportedNode.onChange(((DSNode) vn).getInfo());
            }
        }
    }

    private void setReportedProperty(String name, Object value) {
        HashSet<Property> props = new HashSet<Property>();
        props.add(new Property(name, value));
        try {
            client.sendReportedProperties(props);
        } catch (IOException e) {
            warn(e);
            throw new DSRequestException(e.getMessage());
        }
    }


    public ActionResult sendD2CMessage(DSInfo actionInfo, DSMap parameters) {
        if (client == null) {
            throw new DSRequestException("Client not initialized");
        }
        final DSAbstractAction action = actionInfo.getAction();
        String msgStr = parameters.getString("Message");
        Message msg = new Message(msgStr);
        DSMap properties = parameters.getMap("Properties");
        for (int i = 0; i < properties.size(); i++) {
            Entry entry = properties.getEntry(i);
            msg.setProperty(entry.getKey(), entry.getValue().toString());
        }
        msg.setMessageId(java.util.UUID.randomUUID().toString());
        final List<DSIValue> lockobj = new ArrayList<DSIValue>();
        client.sendEventAsync(msg, new ResponseCallback(), lockobj);

        synchronized (lockobj) {
            try {
                lockobj.wait();
            } catch (InterruptedException e) {
            }
            if (lockobj.isEmpty()) {
                lockobj.add(DSString.NULL);
            }
            return new ActionValues() {
                @Override
                public Iterator<DSIValue> getValues() {
                    return lockobj.iterator();
                }

                @Override
                public ActionSpec getAction() {
                    return action;
                }

                @Override
                public void onClose() {}
            };
        }
    }

    private void addDirectMethod(DSMap parameters) {
        String methodName = parameters.getString("Method Name");
        String path = parameters.getString("Path");
        methodsNode.add(methodName, new DirectMethodNode(methodName, path));
    }

    private ActionResult uploadFile(DSInfo actionInfo, DSMap parameters) {
        if (client == null) {
            warn("Device Client not initialized");
            throw new DSRequestException("Client not initialized");
        }
        final DSAbstractAction action = actionInfo.getAction();
        String name = parameters.getString("Name");
        String path = parameters.getString("Filepath");
        File file = new File(path);
        final List<DSIValue> lockobj = new ArrayList<DSIValue>();
        try {
            InputStream inputStream = new FileInputStream(file);
            long streamLength = file.length();
            client.uploadToBlobAsync(name, inputStream, streamLength, new ResponseCallback(),
                    lockobj);
        } catch (IllegalArgumentException | IOException e) {
            warn("Error uploading file", e);
            throw new DSRequestException(e.getMessage());
        }

        synchronized (lockobj) {
            try {
                lockobj.wait();
            } catch (InterruptedException e) {
            }
            if (lockobj.isEmpty()) {
                lockobj.add(DSString.NULL);
            }
            return new ActionValues() {
                @Override
                public Iterator<DSIValue> getValues() {
                    return lockobj.iterator();
                }

                @Override
                public ActionSpec getAction() {
                    return action;
                }

                @Override
                public void onClose() {}
            };
        }

    }

    @Override
    public void delete() {
        super.delete();
        if (client != null) {
            try {
                client.closeNow();
            } catch (IOException e) {
                warn(e);
            }
        }
    }


    private class ResponseCallback implements IotHubEventCallback {
        @SuppressWarnings("unchecked")
        @Override
        public void execute(IotHubStatusCode responseStatus, Object context) {
            DSIValue resp = responseStatus != null ? DSString.valueOf(responseStatus.toString())
                    : DSString.NULL;
            if (context != null) {
                synchronized (context) {
                    if (context instanceof List<?>) {
                        ((List<DSIValue>) context).add(resp);
                    }
                    context.notify();
                }
            }
        }
    }

    private class C2DMessageCallback implements MessageCallback {
        @Override
        public IotHubMessageResult execute(Message message, Object callbackContext) {
            DSMap msgMap = new DSMap();
            String body = new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET);
            String id = message.getMessageId();
            String corrid = message.getCorrelationId();
            MessageType type = message.getMessageType();
            String typeStr = type != null ? type.toString() : null;
            msgMap.put("ID", id).put("Correlation ID", corrid).put("Type", typeStr).put("Body",
                    body);
            for (MessageProperty prop : message.getProperties()) {
                msgMap.put(prop.getName(), prop.getValue());
            }
            incomingMessage(msgMap);
            return IotHubMessageResult.COMPLETE;
        }
    }

    public void incomingMessage(DSMap message) {
        c2dList.add(message);
        fire(VALUE_TOPIC, DSValueTopic.Event.CHILD_CHANGED, c2d);
    }

    private class DirectMethodCallback implements DeviceMethodCallback {
        @Override
        public DeviceMethodData call(String methodName, Object methodData, Object context) {
            DeviceMethodData deviceMethodData;
            DirectMethodNode child = getDirectMethod(methodName);
            if (child != null) {
                deviceMethodData = child.handle(methodData);
            } else {
                int status = DirectMethodNode.METHOD_NOT_DEFINED;
                deviceMethodData =
                        new DeviceMethodData(status, "Method '" + methodName + "' not found");
            }
            return deviceMethodData;
        }
    }

    public DirectMethodNode getDirectMethod(String methodName) {
        if (methodsNode == null) {
            return null;
        }
        DSIObject child = methodsNode.get(methodName);
        if (child instanceof DirectMethodNode) {
            return (DirectMethodNode) child;
        }
        return null;
    }

    private class DirectMethodStatusCallback implements IotHubEventCallback {
        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            info("IoT Hub responded to device method operation with status "
                    + responseStatus.name());
        }
    }

    private class DeviceTwinStatusCallback implements IotHubEventCallback {
        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            info("IoT Hub responded to device twin operation with status " + responseStatus.name());
        }
    }
}
