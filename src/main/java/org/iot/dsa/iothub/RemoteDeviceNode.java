package org.iot.dsa.iothub;

import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.FeedbackBatch;
import com.microsoft.azure.sdk.iot.service.FeedbackReceiver;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.Message;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceMethod;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwinDevice;
import com.microsoft.azure.sdk.iot.service.devicetwin.MethodResult;
import com.microsoft.azure.sdk.iot.service.devicetwin.Pair;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import org.iot.dsa.node.DSInt;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSMap;
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

/**
 * An instance of this node represents a specific IoT Hub Device.
 *
 * @author Daniel Shapiro
 */
public class RemoteDeviceNode extends RemovableNode {

    private String deviceId;
    private IotHubNode hubNode;
    private DSInfo status;
    private TagsNode tagsNode;
    private DesiredPropsNode desiredNode;
    private DSNode reportedNode;

    private DeviceTwinDevice twin;

    public RemoteDeviceNode() {
    }

    public RemoteDeviceNode(IotHubNode hubNode, String deviceId) {
        this.deviceId = deviceId;
        this.hubNode = hubNode;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Tags", new TagsNode());
        declareDefault("Desired Properties", new DesiredPropsNode());
        declareDefault("Reported Properties", new DSNode());

        declareDefault("Invoke Direct Method", makeInvokeDirectMethodAction());
        declareDefault("Send C2D Message", makeSendMessageAction());
        declareDefault("Refresh", makeRefreshAction());
    }

    public static class TagsNode extends DSNode implements TwinPropertyContainer {

        @Override
        protected void declareDefaults() {
            declareDefault("Add Tag", makeAddTagAction());
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
                ((RemoteDeviceNode) info.getParent().getParent()).setTag(name, object);
            }
        }

        @Override
        public void onDelete(DSInfo info) {
            if (info.isAction()) {
                return;
            }
            String name = info.getName();
            ((RemoteDeviceNode) info.getParent().getParent()).setTag(name, null);
        }
    }

    public static class DesiredPropsNode extends DSNode implements TwinPropertyContainer {

        @Override
        protected void declareDefaults() {
            declareDefault("Add Desired Property", makeAddDesiredPropAction());
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
                ((RemoteDeviceNode) info.getParent().getParent()).setDesiredProperty(name, object);
            }
        }

        @Override
        public void onDelete(DSInfo info) {
            // if (info.isAction()) {
            // return;
            // }
            // String name = info.getName();
            // ((RemoteDeviceNode) info.getParent().getParent()).setDesiredProperty(name, null);
        }

    }

    @Override
    protected void onStable() {
        status = add("STATUS", DSString.valueOf("Connecting"));
        status.setTransient(true);
        status.setReadOnly(true);

        if (hubNode == null) {
            DSNode n = getParent().getParent();
            if (n instanceof IotHubNode) {
                hubNode = (IotHubNode) n;
            }
        }
        if (deviceId == null) {
            deviceId = getName();
        }

        tagsNode = (TagsNode) getNode("Tags");
        desiredNode = (DesiredPropsNode) getNode("Desired Properties");
        reportedNode = getNode("Reported Properties");

        twin = new DeviceTwinDevice(deviceId);

        init();
    }

    private void init() {
        try {
            hubNode.getTwinClient().getTwin(twin);

            updateTags();
            updateDesired();
            updateReported();

            put(status, DSString.valueOf("Ready"));
        } catch (IOException | IotHubException e) {
            warn(e);
            put(status, DSString.valueOf("Error retrieving device twin: " + e.getMessage()));
        }
    }

    private void updateTags() {
        tagsNode.clear();
        for (Pair p : twin.getTags()) {
            String name = p.getKey();
            Object object = p.getValue();
            tagsNode.put(name, Util.objectToValueNode(object)).setTransient(true);
        }
    }

    private void updateDesired() {
        desiredNode.clear();
        for (Pair p : twin.getDesiredProperties()) {
            String name = p.getKey();
            Object object = p.getValue();
            desiredNode.put(name, Util.objectToValueNode(object)).setTransient(true);
        }
    }

    private void updateReported() {
        reportedNode.clear();
        for (Pair p : twin.getReportedProperties()) {
            String name = p.getKey();
            Object object = p.getValue();
            reportedNode.put(name, DSString.valueOf(object)).setTransient(true).setReadOnly(true);
        }
    }

    private DSAction makeRefreshAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RemoteDeviceNode) info.getParent()).init();
                return null;
            }
        };
        return act;
    }

    private DSAction makeSendMessageAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RemoteDeviceNode) info.getParent()).sendC2DMessage(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubServiceClientProtocol.AMQPS), null);
        act.addParameter("Message", DSValueType.STRING, null);
        act.addDefaultParameter("Properties", new DSMap(), null);
        // act.setResultType(ResultType.VALUES);
        // act.addColumn("Feedback", DSValueType.STRING);
        return act;
    }

    private DSAction makeInvokeDirectMethodAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                return ((RemoteDeviceNode) info.getParent()).invokeDirectMethod(info,
                                                                                invocation
                                                                                        .getParameters());
            }
        };
        act.addParameter("Method Name", DSValueType.STRING, null);
        act.addDefaultParameter("Response Timeout", DSInt.valueOf(30),
                                "Response Timeout in Seconds");
        act.addDefaultParameter("Connect Timeout", DSInt.valueOf(5), "Connect Timeout in Seconds");
        act.addParameter("Payload", DSValueType.STRING, "Payload of direct method invocation");
        act.setResultType(ResultType.VALUES);
        act.addValueResult("Result Status", DSValueType.NUMBER);
        act.addValueResult("Result Payload", DSValueType.STRING);
        return act;
    }

    private static DSAction makeAddDesiredPropAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RemoteDeviceNode) info.getParent().getParent())
                        .addDesiredProp(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Value Type", DSFlexEnum.valueOf("String", Util.getSimpleValueTypes()),
                         null);
        return act;
    }

    private static DSAction makeAddTagAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RemoteDeviceNode) info.getParent().getParent())
                        .addTag(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Value Type", DSFlexEnum.valueOf("String", Util.getSimpleValueTypes()),
                         null);
        return act;
    }

    private void addTag(DSMap parameters) {
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
            tagsNode.add(name, vn).setTransient(true);
            if (vn instanceof DSNode) {
                tagsNode.onChange(((DSNode) vn).getInfo());
            }
        }
    }

    private void setTag(String name, Object value) {
        Set<Pair> tags = new HashSet<Pair>();
        tags.add(new Pair(name, value));
        twin.setTags(tags);
        try {
            hubNode.getTwinClient().updateTwin(twin);
        } catch (IotHubException | IOException e) {
            warn(e);
        }
    }

    private void addDesiredProp(DSMap parameters) {
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
            desiredNode.add(name, vn).setTransient(true);
            if (vn instanceof DSNode) {
                desiredNode.onChange(((DSNode) vn).getInfo());
            }
        }
    }

    private void setDesiredProperty(String name, Object value) {
        Set<Pair> props = new HashSet<Pair>();
        props.add(new Pair(name, value));
        twin.setDesiredProperties(props);
        try {
            hubNode.getTwinClient().updateTwin(twin);
        } catch (IotHubException | IOException e) {
            warn(e);
        }
    }


    public ActionResult invokeDirectMethod(DSInfo actionInfo, DSMap parameters) {
        final DSAbstractAction action = actionInfo.getAction();
        String methodName = parameters.getString("Method Name");
        long responseTimeout = TimeUnit.SECONDS.toSeconds(parameters.getLong("Response Timeout"));
        long connectTimeout = TimeUnit.SECONDS.toSeconds(parameters.getLong("Connect Timeout"));
        String invPayload = parameters.getString("Payload");
        DeviceMethod methodClient = hubNode.getMethodClient();
        if (methodClient == null) {
            warn("Method Client not initialized");
            throw new DSRequestException("Method Client not initialized");
        }

        try {
            MethodResult result = methodClient.invoke(deviceId, methodName, responseTimeout,
                                                      connectTimeout, invPayload);

            if (result == null) {
                throw new IOException("Invoke direct method returned null");
            }
            Integer status = result.getStatus();
            DSIValue v1 = status != null ? DSInt.valueOf(status) : DSInt.NULL;
            Object payload = result.getPayload();
            DSIValue v2 = payload != null ? DSString.valueOf(payload.toString()) : DSString.NULL;
            final List<DSIValue> vals = Arrays.asList(v1, v2);
            return new ActionValues() {
                @Override
                public ActionSpec getAction() {
                    return action;
                }

                @Override
                public int getColumnCount() {
                    return vals.size();
                }

                @Override
                public void getMetadata(int col, DSMap bucket) {
                    bucket.putAll(action.getValueResult(col));
                }

                @Override
                public DSIValue getValue(int col) {
                    return vals.get(col);
                }

                @Override
                public void onClose() {
                }
            };
        } catch (IotHubException | IOException e) {
            warn("Error invoking direct method: " + e);
            throw new DSRequestException(e.getMessage());
        }
    }

    public void sendC2DMessage(DSMap parameters) {
        String protocolStr = parameters.getString("Protocol");
        IotHubServiceClientProtocol protocol = protocolStr.endsWith("WS")
                ? IotHubServiceClientProtocol.AMQPS_WS : IotHubServiceClientProtocol.AMQPS;
        String message = parameters.getString("Message");
        DSMap properties = parameters.getMap("Properties");

        try {
            ServiceClient serviceClient = ServiceClient
                    .createFromConnectionString(hubNode.getConnectionString(), protocol);
            if (serviceClient != null) {
                serviceClient.open();
                FeedbackReceiver feedbackReceiver = serviceClient.getFeedbackReceiver();
                if (feedbackReceiver != null) {
                    feedbackReceiver.open();
                }

                Message messageToSend = new Message(message);
                messageToSend.setProperties(Util.dsMapToMap(properties));
                messageToSend.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);

                serviceClient.send(deviceId, messageToSend);

                FeedbackBatch feedbackBatch = feedbackReceiver.receive(10000);
                if (feedbackBatch != null) {
                    info("Message feedback received, feedback time: "
                                 + feedbackBatch.getEnqueuedTimeUtc().toString());
                }

                if (feedbackReceiver != null) {
                    feedbackReceiver.close();
                }
                serviceClient.close();
            }
        } catch (IOException | IotHubException | InterruptedException e) {
            warn("Error sending cloud-to-device message: " + e);
            throw new DSRequestException(e.getMessage());
        }
    }

}
