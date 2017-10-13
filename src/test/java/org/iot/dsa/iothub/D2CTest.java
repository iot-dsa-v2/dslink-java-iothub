package org.iot.dsa.iothub;

import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.ActionInvocation;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.*;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

@FixMethodOrder(MethodSorters.DEFAULT)
public class D2CTest {
    public static String DANIEL_CONNECTION_STRING =
            "HostName=DanielFreeHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU=";
    public static String DANIEL_EVENT_NAME = "iothub-ehub-danielfree-172452-a48c3b34bf";
    public static String DANIEL_EVENT_ENDPT =
            "Endpoint=sb://ihsuprodbyres053dednamespace.servicebus.windows.net/;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU=";

    @Test
    public void d2cWithMQTT() throws Exception {
        testD2C("Test", IotHubClientProtocol.MQTT, "1", "Hello, World!",
                new DSMap().put("source", "D2CTest").put("test", "d2cWithMQTT"));
    }

    @Test
    public void d2cWithAMQPS() throws Exception {
        testD2C("Test", IotHubClientProtocol.AMQPS, "1", "Hello, World!",
                new DSMap().put("source", "D2CTest").put("test", "d2cWithAMQPS"));
    }

    @Test
    public void d2cWithAMQPS_WS() throws Exception {
        testD2C("Test", IotHubClientProtocol.AMQPS_WS, "1", "Hello, World!",
                new DSMap().put("source", "D2CTest").put("test", "d2cWithAMQPS_WS"));
    }

    @Test
    public void d2cWithHTTPS() throws Exception {
        testD2C("Test", IotHubClientProtocol.HTTPS, "1", "Hello, World!",
                new DSMap().put("source", "D2CTest").put("test", "d2cWithHTTPS"));
    }

    public static void testD2C(final String deviceId, IotHubClientProtocol protocol,
            final String partitionId, final String message, final DSMap props) throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode();

        LocalDeviceNode sendDev = new LocalDeviceNode(sendHub, deviceId, protocol);
        sendDev.registerDeviceIdentity();
        sendDev.setupClient();

        boolean done = true;

        DSInfo actionInfo1 = mock(DSInfo.class);
        when(actionInfo1.getAction()).thenReturn(null);
        ActionInvocation invocation = mock(ActionInvocation.class);
        DSMap parameters1 = new DSMap().put("EventHub_Compatible_Name", DANIEL_EVENT_NAME)
                .put("EventHub_Compatible_Endpoint", DANIEL_EVENT_ENDPT)
                .put("Partition_ID", partitionId);
        when(invocation.getParameters()).thenReturn(parameters1);
        when(invocation.isOpen()).thenReturn(done);
        recvHub.readMessages(actionInfo1, invocation);

        DSInfo actionInfo2 = mock(DSInfo.class);
        when(actionInfo2.getAction()).thenReturn(null);
        DSMap parameters2 = new DSMap().put("Message", message).put("Properties", props);
        sendDev.sendD2CMessage(actionInfo2, parameters2);

        class IsMatchingRow extends ArgumentMatcher<DSList> {
            public boolean matches(Object o) {
                DSList row = (DSList) o;
                return row.size() == 6 && row.get(3).equals(DSString.valueOf(deviceId))
                        && row.get(4).equals(DSString.valueOf(message)) && row.get(5).equals(props);
            }
        }

        Thread.sleep(4000);
        done = false;
        verify(invocation).send(argThat(new IsMatchingRow()));
    }

}
