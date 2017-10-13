package org.iot.dsa.iothub;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.iot.dsa.dslink.DSRequestException;
import org.iot.dsa.dslink.requester.OutboundInvokeRequest;
import org.iot.dsa.dslink.requester.OutboundRequest;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatcher;
import com.acuity.iot.dsa.dslink.DSRequesterSession;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DirectMethodEffectsTest {
    public static String DANIEL_CONNECTION_STRING =
            "HostName=DanielFreeHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU=";

    @Test
    public void recordsInvoke_MQTT() throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        String deviceId = "Test";
        String method = "doNothing";
        DSMap payload = new DSMap().put("a", 123).put("b", "abc");

        LocalDeviceNode recvDev =
                spy(new LocalDeviceNode(recvHub, deviceId, IotHubClientProtocol.MQTT));
        recvDev.registerDeviceIdentity();
        recvDev.setupClient();

        DirectMethodNode methodNode = spy(new DirectMethodNode(method, ""));

        when(recvDev.getDirectMethod(method)).thenReturn(methodNode);


        RemoteDeviceNode sendDev = new RemoteDeviceNode(sendHub, deviceId);
        DSInfo actionInfo = mock(DSInfo.class);
        when(actionInfo.getAction()).thenReturn(null);
        DSMap parameters = new DSMap().put("Method_Name", method).put("Response_Timeout", 30)
                .put("Connect_Timeout", 5).put("Payload", payload.toString());
        sendDev.invokeDirectMethod(actionInfo, parameters);

        verify(methodNode).recordInvoke(payload);
    }

    @Test
    public void proxiesToPath_MQTT() throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        String deviceId = "Test";
        String method = "doSomething";
        final String path = "/sys/get_server_log";
        final DSMap payload = new DSMap().put("lines", 900);

        LocalDeviceNode recvDev =
                spy(new LocalDeviceNode(recvHub, deviceId, IotHubClientProtocol.MQTT));
        recvDev.registerDeviceIdentity();
        recvDev.setupClient();

        DirectMethodNode methodNode = new DirectMethodNode(method, path);

        when(recvDev.getDirectMethod(method)).thenReturn(methodNode);
        DSRequesterSession session = mock(DSRequesterSession.class);
        RootNode m = new RootNode();
        m.onConnected(session);


        RemoteDeviceNode sendDev = new RemoteDeviceNode(sendHub, deviceId);
        DSInfo actionInfo = mock(DSInfo.class);
        when(actionInfo.getAction()).thenReturn(null);
        DSMap parameters = new DSMap().put("Method_Name", method).put("Response_Timeout", 30)
                .put("Connect_Timeout", 5).put("Payload", payload.toString());
        try {
            sendDev.invokeDirectMethod(actionInfo, parameters);
        } catch (DSRequestException e) {
        }

        class IsMatchingRequest extends ArgumentMatcher<OutboundRequest> {
            public boolean matches(Object o) {
                if (o instanceof OutboundInvokeRequest) {
                    OutboundInvokeRequest req = (OutboundInvokeRequest) o;
                    return req.getPath().equals(path) && req.getParameters().equals(payload);
                }
                return false;
            }
        }

        verify(session).sendRequest(argThat(new IsMatchingRequest()));
    }
}
