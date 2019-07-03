package org.iot.dsa.iothub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.iot.dsa.conn.DSConnection;
import org.iot.dsa.dslink.DSRequestException;
import org.iot.dsa.dslink.restadapter.Constants;
import org.iot.dsa.dslink.restadapter.ResponseWrapper;
import org.iot.dsa.iothub.node.BoolNode;
import org.iot.dsa.iothub.node.DoubleNode;
import org.iot.dsa.iothub.node.ListNode;
import org.iot.dsa.iothub.node.StringNode;
import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.ActionSpec;
import org.iot.dsa.node.action.ActionValues;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.action.ActionSpec.ResultType;
import org.iot.dsa.time.DSDateTime;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Device;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

public abstract class AbstractLocalDeviceNode extends DSConnection {
    private DSInfo c2d;
    private DSList c2dList = new DSList();
    private DeviceClient client;
    private Object clientLock = new Object();
    private DSNode desiredNode;
    private DSNode methodsNode;
    private DSNode rulesNode;
    private ReportedPropsNode reportedNode;
    private DSInfo status;
    private Device twin;
    
    protected String deviceId;
    protected IotHubClientProtocol protocol;
    
    public AbstractLocalDeviceNode() {
    }
    
    public AbstractLocalDeviceNode(String deviceId, IotHubClientProtocol protocol) {
        this.deviceId = deviceId;
        this.protocol = protocol;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Methods", new MethodsNode());
        declareDefault("Desired Properties", new DSNode());
        declareDefault("Reported Properties", new ReportedPropsNode());
        declareDefault("D2C Rules", new D2CRoutingNode());

        declareDefault("Send D2C Message", makeSendMessageAction());
        declareDefault("Upload File", makeUploadFileAction());
        declareDefault("Refresh", makeRefreshAction());
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        if (protocol == null) {
            DSIObject p = get("Protocol");
            protocol = p instanceof DSString ? IotHubClientProtocol.valueOf(p.toString())
                    : IotHubClientProtocol.MQTT;
        }
    }
    
    @Override
    protected void onRemoved() {
        super.onRemoved();
        synchronized(clientLock) {
            if (client != null) {
                try {
                    client.closeNow();
                } catch (IOException e) {
                    warn(e);
                }
            }
        }
    }
    
    @Override
    protected void onStable() {
        status = add("STATUS", DSString.valueOf("Connecting"));
        status.setTransient(true);
        status.setReadOnly(true);

        if (deviceId == null) {
            deviceId = getName();
        }

        c2d = add("Cloud-To-Device Messages", c2dList);
        c2d.setTransient(true).setReadOnly(true);
        methodsNode = getNode("Methods");
        desiredNode = getNode("Desired Properties");
        reportedNode = (ReportedPropsNode) getNode("Reported Properties");
        rulesNode = getNode("D2C Rules");
    }
    
    private void init() {
        storeConfigs();
        synchronized(clientLock) {
            if (client != null) {
                try {
                    client.closeNow();
                } catch (IOException e) {
                    warn(e);
                }
            }
        }
        
        try {
            setupClient();

            HashSet<Property> props = new HashSet<Property>();
            for (DSInfo info : reportedNode) {
                if (!info.isAction()) {
                    String name = info.getName();
                    DSIObject value = info.get();
                    if (value instanceof TwinProperty) {
                        Object object = ((TwinProperty) value).getObject();
                        props.add(new Property(name, object));
                    }
                }
            }
            if (!props.isEmpty()) {
                client.sendReportedProperties(props);
            }
            //put(status, DSString.valueOf("Connected"));
        } catch (Exception e) {
            warn("Error initializing device client", e);
            put(status, DSString.valueOf("Error initializing device client: " + e.getMessage()));
            connDown("Error initializing device client: " + e.getMessage());
        }
        put("Edit", makeEditAction()).setTransient(true);
    }
    
    private void setupClient() throws IOException, URISyntaxException {
        synchronized (clientLock) {
            DeviceClient client = newClient();
            MessageCallback callback = new C2DMessageCallback();
            client.setMessageCallback(callback, null);
            client.registerConnectionStatusChangeCallback(new ConnectionStatusCallback(), null);

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
    }
    
    protected abstract DeviceClient newClient() throws IllegalArgumentException, URISyntaxException;
    
    protected void storeConfigs() {
        if (protocol != null) {
            put("Protocol", DSString.valueOf(protocol.toString())).setReadOnly(true);
        }
    }
    
    protected DSAction makeEditAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((AbstractLocalDeviceNode) target.get()).edit(invocation.getParameters());
                return null;
            }
        };
        act.addDefaultParameter("Protocol", DSJavaEnum.valueOf(protocol), null);
        return act;
    }
    
    protected void edit(DSMap parameters) {
        String protocolStr = parameters.getString("Protocol");
        protocol = IotHubClientProtocol.valueOf(protocolStr);
        editSpecifics(parameters);
        init();
    }
    
    protected abstract void editSpecifics(DSMap parameters);
    
    private static DSAction makeAddMethodAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((AbstractLocalDeviceNode) target.getParent()).addDirectMethod(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Method Name", DSValueType.STRING, null);
        act.addDefaultParameter("Path", DSString.EMPTY, null);
        act.addParameter("DSA Method", DSJavaEnum.valueOf(DSAMethod.INVOKE), "Whether to invoke, set a value, or get a value at the specified path");
        return act;
    }

    private static DSAction makeAddReportedPropAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((AbstractLocalDeviceNode) target.getParent()).addReportedProp(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Value Type", DSFlexEnum.valueOf("String", Util.getSimpleValueTypes()),
                         null);
        return act;
    }
    
    private static DSAction makeAddRuleAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation request) {
                ((AbstractLocalDeviceNode) target.getParent()).addRule(request.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addParameter(Constants.SUB_PATH, DSValueType.STRING, null);
        act.addDefaultParameter("Properties", new DSMap(), null);
        act.addParameter(Constants.REQUEST_BODY, DSValueType.STRING, null);
        act.addParameter(Constants.MIN_REFRESH_RATE, DSValueType.NUMBER, "Optional, ensures at least this many seconds between updates");
        act.addParameter(Constants.MAX_REFRESH_RATE, DSValueType.NUMBER, "Optional, ensures an update gets sent every this many seconds");
        return act;
    }
    
    private DSAction makeRefreshAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((AbstractLocalDeviceNode) target.get()).init();
                return null;
            }
        };
        return act;
    }

    private DSAction makeSendMessageAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                return ((AbstractLocalDeviceNode) target.get()).sendD2CMessage(this,
                                                                       invocation.getParameters());
            }
        };
        act.addParameter("Message", DSValueType.STRING, null);
        act.addDefaultParameter("Properties", new DSMap(), null);
        act.addDefaultParameter("Await Response", DSBool.TRUE, null);
        act.setResultType(ResultType.VALUES);
        act.addColumnMetadata("Response Status", DSValueType.STRING);
        return act;
    }

    private DSAction makeUploadFileAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                return ((AbstractLocalDeviceNode) target.get()).uploadFile(this,
                                                                   invocation.getParameters());
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Filepath", DSValueType.STRING, null).setPlaceHolder("myImage.png");
        act.setResultType(ResultType.VALUES);
        act.addColumnMetadata("Response Status", DSValueType.STRING);
        return act;
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

    private ActionResult uploadFile(final DSAction action, DSMap parameters) {
        if (client == null) {
            warn("Device Client not initialized");
            throw new DSRequestException("Client not initialized");
        }
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
                public ActionSpec getAction() {
                    return action;
                }

                @Override
                public int getColumnCount() {
                    return lockobj.size();
                }

                @Override
                public void getMetadata(int col, DSMap bucket) {
                    action.getColumnMetadata(col, bucket);
                }

                @Override
                public DSIValue getValue(int col) {
                    return lockobj.get(col);
                }

                @Override
                public void onClose() {
                }
            };
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
    
    public void incomingMessage(DSMap message) {
        c2dList.add(message);
        fire(VALUE_CHANGED_EVENT, c2d, null);
    }
    
    private void addDirectMethod(DSMap parameters) {
        String methodName = parameters.getString("Method Name");
        String path = parameters.getString("Path");
        DSAMethod dsaMethod = DSAMethod.valueOf(parameters.getString("DSA Method"));
        methodsNode.add(methodName, new DirectMethodNode(methodName, path, dsaMethod));
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
    
    private void addRule(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        rulesNode.add(name, new D2CRuleNode(parameters));
    }
    
    public ActionResult sendD2CMessage(final DSAction action, DSMap parameters) {
        String msgStr = parameters.getString("Message");
        DSMap properties = parameters.getMap("Properties");
        boolean awaitResponse = parameters.getBoolean("Await Response");
        
        final ResponseWrapper resp = doSendD2C(properties, msgStr, awaitResponse);
        
        return new ActionValues() {

            @Override
            public ActionSpec getAction() {
                return action;
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public void getMetadata(int col, DSMap bucket) {
                action.getColumnMetadata(col, bucket);
            }

            @Override
            public DSIValue getValue(int col) {
                return DSString.valueOf(resp.getData());
            }

            @Override
            public void onClose() {
            }
        };
    }
    
    public ResponseWrapper doSendD2C(DSMap properties, String messageBody, boolean awaitResponse) {
        final List<IotHubStatusCode> lockobj = new ArrayList<IotHubStatusCode>();
        synchronized(clientLock) {
            if (client == null) {
                throw new DSRequestException("Client not initialized");
            }
            Message msg = new Message(messageBody);
            for (Entry entry : properties) {
                msg.setProperty(entry.getKey(), entry.getValue().toString());
            }
            msg.setMessageId(java.util.UUID.randomUUID().toString());
            
            if (!awaitResponse) {
                client.sendEventAsync(msg, null, null);
                return new SimpleResponseWrapper(202, "Message sent, not waiting for response", DSDateTime.now());
            }
            client.sendEventAsync(msg, new ResponseCallback(), lockobj);
        }
        synchronized (lockobj) {
            try {
                lockobj.wait();
            } catch (InterruptedException e) {
            }
            if (lockobj.isEmpty()) {
                return new SimpleResponseWrapper(408, "No response from Iot Hub", DSDateTime.now());
            }
            IotHubStatusCode respStatus = lockobj.get(0);
            return new SimpleResponseWrapper(Util.iotHubStatusToHttpCode(respStatus), respStatus.toString(), DSDateTime.now());
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

    private class DeviceTwinStatusCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            info("IoT Hub responded to device twin operation with status " + responseStatus.name());
        }
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

    private class DirectMethodStatusCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            info("IoT Hub responded to device method operation with status "
                         + responseStatus.name());
        }
    }
    
    private class ConnectionStatusCallback implements IotHubConnectionStatusChangeCallback {
        @Override
        public void execute(IotHubConnectionStatus newStatus,
                IotHubConnectionStatusChangeReason statusChangeReason, Throwable throwable,
                Object callbackContext) {
            put(status, DSString.valueOf(newStatus + ": " + statusChangeReason));
            info("Connection status changed to " + newStatus + "; for reason " + statusChangeReason);
            if (throwable != null) {
                warn("", throwable);
            }
            if (newStatus == IotHubConnectionStatus.DISCONNECTED) {
                connDown(statusChangeReason.toString());
            } else if (newStatus == IotHubConnectionStatus.CONNECTED) {
                connOk();
            }
        }
    }

    public static class MethodsNode extends DSNode {

        @Override
        protected void declareDefaults() {
            declareDefault("Add Direct Method", makeAddMethodAction());
        }
    }

    public static class ReportedPropsNode extends DSNode implements TwinPropertyContainer {

        @Override
        public void onChange(DSInfo info) {
            if (info.isAction()) {
                return;
            }
            String name = info.getName();
            DSIObject value = info.get();
            if (value instanceof TwinProperty) {
                Object object = ((TwinProperty) value).getObject();
                ((AbstractLocalDeviceNode) info.getParent().getParent()).setReportedProperty(name, object);
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

        @Override
        protected void declareDefaults() {
            declareDefault("Add Reported Property", makeAddReportedPropAction());
        }

        @Override
        protected void onChildChanged(DSInfo info) {
            onChange(info);
        }

    }
    
    public static class D2CRoutingNode extends DSNode {
        
        @Override
        protected void declareDefaults() {
            super.declareDefaults();
            declareDefault(Constants.ACT_ADD_RULE, makeAddRuleAction());
        }
    }
    
    
    private class ResponseCallback implements IotHubEventCallback {

        @SuppressWarnings("unchecked")
        @Override
        public void execute(IotHubStatusCode responseStatus, Object context) {
            if (context != null) {
                synchronized (context) {
                    if (context instanceof List<?> && responseStatus != null) {
                        ((List<IotHubStatusCode>) context).add(responseStatus);
                    }
                    context.notify();
                }
            }
        }
    }
    
    @Override
    protected void doConnect() {
        init();
    }

    @Override
    protected void doDisconnect() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void checkConfig() {
        // TODO Auto-generated method stub
    }
}
