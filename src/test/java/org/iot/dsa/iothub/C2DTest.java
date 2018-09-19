package org.iot.dsa.iothub;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSString;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatcher;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class C2DTest {
    public static String DANIEL_CONNECTION_STRING =
            "HostName=DanielFreeHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU=";

    @Test
    public void c2dWithAMQPSToMQTT() throws Exception {
        testC2D("Test", IotHubClientProtocol.MQTT, IotHubServiceClientProtocol.AMQPS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPSToMQTT"), 4000);
    }

    @Test
    public void c2dWithAMQPS_WSToMQTT() throws Exception {
        testC2D("Test", IotHubClientProtocol.MQTT, IotHubServiceClientProtocol.AMQPS_WS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPS_WSToMQTT"), 6000);
    }

    // @Test
    // public void c2dWithAMQPSToHTTPS() throws Exception {
    // testC2D("Test", IotHubClientProtocol.HTTPS, IotHubServiceClientProtocol.AMQPS, "Hello,
    // World!", new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPSToHTTPS"), 4000);
    // }
    //
    // @Test
    // public void c2dWithAMQPS_WSToHTTPS() throws Exception {
    // testC2D("Test", IotHubClientProtocol.HTTPS, IotHubServiceClientProtocol.AMQPS_WS, "Hello,
    // World!", new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPS_WSToHTTPS"), 4000);
    // }

    @Test
    public void c2dWithAMQPSToAMQPS() throws Exception {
        testC2D("Test", IotHubClientProtocol.AMQPS, IotHubServiceClientProtocol.AMQPS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPSToAMQPS"), 4000);
    }

    @Test
    public void c2dWithAMQPS_WSToAMQPS() throws Exception {
        testC2D("Test", IotHubClientProtocol.AMQPS, IotHubServiceClientProtocol.AMQPS_WS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPS_WSToAMQPS"), 4000);
    }

    @Test
    public void c2dWithAMQPSToAMQPS_WS() throws Exception {
        testC2D("Test", IotHubClientProtocol.AMQPS_WS, IotHubServiceClientProtocol.AMQPS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPSToAMQPS_WS"), 4000);
    }

    @Test
    public void c2dWithAMQPS_WSToAMQPS_WS() throws Exception {
        testC2D("Test", IotHubClientProtocol.AMQPS_WS, IotHubServiceClientProtocol.AMQPS_WS,
                "Hello, World!",
                new DSMap().put("source", "C2DTest").put("test", "c2dWithAMQPS_WSToAMQPS_WS"),
                4000);
    }


    public void testC2D(final String deviceId, IotHubClientProtocol rprotocol,
            IotHubServiceClientProtocol sprotocol, final String message, final DSMap props,
            long timeout) throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode(DANIEL_CONNECTION_STRING);

        LocalDeviceNode recvDev = spy(new LocalDeviceNode(recvHub, deviceId, rprotocol));
        recvDev.registerDeviceIdentity();
        recvDev.setupClient();

        RemoteDeviceNode sendDev = new RemoteDeviceNode(sendHub, deviceId);
        DSMap parameters = new DSMap().put("Protocol", sprotocol.toString()).put("Message", message)
                .put("Properties", props);
        sendDev.sendC2DMessage(parameters);

        class IsMatchingMap extends ArgumentMatcher<DSMap> {
            public boolean matches(Object o) {
                DSMap map = (DSMap) o;
                boolean isMatch = map.size() >= 4 + props.size()
                        && map.get("Body").equals(DSString.valueOf(message));
                if (isMatch) {
                    for (Entry entry : props) {
                        if (!entry.getValue().equals(map.get(entry.getKey()))) {
                            return false;
                        }
                    }
                }
                return isMatch;
            }
        }

        Thread.sleep(timeout);

        verify(recvDev).incomingMessage(argThat(new IsMatchingMap()));
    }
}
