package org.iot.dsa.iothub;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.ActionValues;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DirectMethodReturnTest {

    public static String DANIEL_CONNECTION_STRING =
            "HostName=DanielFreeHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU=";

    @Test
    public void returns404_MQTT() throws Exception {
        returns404(IotHubClientProtocol.MQTT);
    }

    // @Test
    // public void returns404_AMQPS() throws Exception{
    // returns404(IotHubClientProtocol.AMQPS);
    // }
    //
    // @Test
    // public void returns404_AMQPS_WS() throws Exception{
    // returns404(IotHubClientProtocol.AMQPS_WS);
    // }
    //
    // @Test
    // public void returns404_HTTPS() throws Exception{
    // returns404(IotHubClientProtocol.HTTPS);
    // }

    public void returns404(IotHubClientProtocol protocol) throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        String deviceId = "Test";
        String method = "nonexistant";
        String payload = "{\"a\":123, \"b\":\"abc\"}";

        LocalDeviceNode recvDev = spy(new LocalDeviceNode(recvHub, deviceId, protocol));
        recvDev.registerDeviceIdentity();
        recvDev.setupClient();
        when(recvDev.getDirectMethod(method)).thenReturn(null);

        RemoteDeviceNode sendDev = new RemoteDeviceNode(sendHub, deviceId);
        DSInfo actionInfo = mock(DSInfo.class);
        when(actionInfo.getAction()).thenReturn(null);
        DSMap parameters = new DSMap().put("Method Name", method).put("Response Timeout", 30)
                                      .put("Connect Timeout", 5).put("Payload", payload);
        ActionResult result = sendDev.invokeDirectMethod(actionInfo.getAction(), parameters);
        DSIValue code = ((ActionValues) result).getValue(0);
        assert (code.toElement().toInt() == 404);

    }

    @Test
    public void returns200_MQTT() throws Exception {
        returns200(IotHubClientProtocol.MQTT);
    }

    // @Test
    // public void returns200_AMQPS() throws Exception{
    // returns200(IotHubClientProtocol.AMQPS);
    // }
    //
    // @Test
    // public void returns200_AMQPS_WS() throws Exception{
    // returns200(IotHubClientProtocol.AMQPS_WS);
    // }
    //
    // @Test
    // public void returns200_HTTPS() throws Exception{
    // returns200(IotHubClientProtocol.HTTPS);
    // }

    public void returns200(IotHubClientProtocol protocol) throws Exception {
        IotHubNode sendHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        IotHubNode recvHub = new IotHubNode(DANIEL_CONNECTION_STRING);
        String deviceId = "Test";
        String method = "doNothing";
        String payload = "{\"a\":123, \"b\":\"abc\"}";

        LocalDeviceNode recvDev = spy(new LocalDeviceNode(recvHub, deviceId, protocol));
        recvDev.registerDeviceIdentity();
        recvDev.setupClient();

        DirectMethodNode methodNode = new DirectMethodNode(method, "");

        //when(recvDev.getDirectMethod(method)).thenReturn(methodNode);
        doReturn(methodNode).when(recvDev).getDirectMethod(method);

        RemoteDeviceNode sendDev = new RemoteDeviceNode(sendHub, deviceId);
        DSInfo actionInfo = mock(DSInfo.class);
        when(actionInfo.getAction()).thenReturn(null);
        DSMap parameters = new DSMap().put("Method Name", method).put("Response Timeout", 30)
                                      .put("Connect Timeout", 10).put("Payload", payload);
        ActionResult result = sendDev.invokeDirectMethod(actionInfo.getAction(), parameters);
        DSIValue code = ((ActionValues) result).getValue(0);
        assert (code.toElement().toInt() == 200);
    }
}
