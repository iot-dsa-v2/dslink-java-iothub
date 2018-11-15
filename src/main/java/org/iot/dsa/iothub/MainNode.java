package org.iot.dsa.iothub;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.event.DSIEvent;
import org.iot.dsa.node.event.DSISubscriber;
import org.iot.dsa.node.event.DSITopic;

/**
 * This is the root node of the link.
 *
 * @author Daniel Shapiro
 */
public class MainNode extends DSMainNode {

    private static DSIRequester requester;

    public static DSIRequester getRequester() {
        return requester;
    }

    public static void setRequester(DSIRequester requester) {
        MainNode.requester = requester;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).handleAddIotHub(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Connection String", DSValueType.STRING, null);
        declareDefault("Add IoT Hub", act);
        declareDefault("Docs", DSString.valueOf(
                "https://github.com/iot-dsa-v2/dslink-java-v2-iothub/blob/develop/README.md"))
                .setTransient(true).setReadOnly(true);
    }

    @Override
    protected void onStarted() {
        getLink().getConnection().subscribe(DSLinkConnection.CONNECTED, null, new DSISubscriber() {
            @Override
            public void onEvent(DSNode node, DSInfo child, DSIEvent event) {
                MainNode.setRequester(getLink().getConnection().getRequester());
            }

            @Override
            public void onUnsubscribed(DSITopic topic, DSNode node, DSInfo child) {
            }
        });
    }

    private void handleAddIotHub(DSMap parameters) {
        String name = parameters.getString("Name");
        String connString = parameters.getString("Connection String");

        IotHubNode hub = new IotHubNode(connString);
        add(name, hub);
    }
}
