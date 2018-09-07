package org.iot.dsa.iothub;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSLinkConnection.Listener;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

/**
 * This is the root node of the link.
 *
 * @author Daniel Shapiro
 */
public class MainNode extends DSMainNode {
    
    private static DSIRequester requester;

    private void handleAddIotHub(DSMap parameters) {
        String name = parameters.getString("Name");
        String connString = parameters.getString("Connection String");

        IotHubNode hub = new IotHubNode(connString);
        add(name, hub);
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((MainNode) info.getParent()).handleAddIotHub(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Connection String", DSValueType.STRING, null);
        declareDefault("Add IoT Hub", act);
        declareDefault("Docs", DSString.valueOf("https://github.com/iot-dsa-v2/dslink-java-v2-iothub/blob/develop/README.md")).setTransient(true).setReadOnly(true);
    }
    
    @Override
    protected void onStarted() {
        getLink().getConnection().addListener(new Listener() {
            @Override
            public void onConnect(DSLinkConnection connection) {
                MainNode.setRequester(getLink().getConnection().getRequester());
            }

            @Override
            public void onDisconnect(DSLinkConnection connection) {
            }
        });
    }

    public static DSIRequester getRequester() {
        return requester;
    }

    public static void setRequester(DSIRequester requester) {
        MainNode.requester = requester;
    }
}
