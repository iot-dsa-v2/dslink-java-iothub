package org.iot.dsa.iothub;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

public class DPSDeviceNodeByX509 extends DPSDeviceNode {
    private String cert;
    
    public DPSDeviceNodeByX509() {
        super();
    }
    
    public DPSDeviceNodeByX509(String deviceId, IotHubClientProtocol protocol, String scopeId, String endpoint, String regId, String cert) {
        super(deviceId, protocol, scopeId, endpoint, regId);
        this.cert = cert;
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        if (cert == null || cert.isEmpty()) {
            DSIObject c = get("Certificate");
            cert = c instanceof DSString ? c.toString() : null;
        }
    }
    
    @Override
    protected void storeConfigs() {
        super.storeConfigs();
        if (cert != null) {
            put("Certificate", DSString.valueOf(cert)).setReadOnly(true);
        }
    }
    
    @Override
    protected DSAction makeEditAction() {
        DSAction act = super.makeEditAction();
        act.addDefaultParameter("Certificate", cert != null ? DSString.valueOf(cert) : DSString.EMPTY, "Either a path to the certificate PEM file, or the PEM text");
        return act;
    }
    
    @Override
    protected void editSpecifics(DSMap parameters) {
        super.editSpecifics(parameters);
        cert = parameters.getString("Certificate");
    }
    
    private X509Certificate getX509() {
        if (cert == null) {
            return null;
        }
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            if (cert.startsWith("-----BEGIN CERTIFICATE-----")) {
                return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(cert.getBytes()));
            } else {
                return (X509Certificate) certFactory.generateCertificate(new FileInputStream(cert));
            }
        } catch (CertificateException | FileNotFoundException e) {
            warn("", e);
            return null;
        }
    }

    @Override
    protected DeviceClient newClient() throws IllegalArgumentException, URISyntaxException {
        info("Device ID = " + deviceId);              // these log messages aren't necessary, just to show the available parameters
        info("Protocol = " + protocol.toString());
        info("Scope ID = " + scopeId);
        info("Provisioning Service Endpoint = " + endpoint);
        info("Registration ID = " + regId);
        
        X509Certificate x509 = getX509();
        
        // TODO create a new DeviceClient instance using the parameters listed above
        return null;
    }

}
