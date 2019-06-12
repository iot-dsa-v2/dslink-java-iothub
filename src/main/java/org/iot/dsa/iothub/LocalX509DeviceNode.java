package org.iot.dsa.iothub;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.iot.dsa.node.DSFlexEnum;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

public class LocalX509DeviceNode extends AbstractLocalDeviceNode {
    private String uri;
    private boolean groupEnrollment = false;
    private String cert;
    
    public LocalX509DeviceNode() {
        super();
    }
    
    public LocalX509DeviceNode(String deviceId, IotHubClientProtocol protocol, String uri, boolean groupEnrollment, String cert) {
        super(deviceId, protocol);
        this.uri = uri;
        this.groupEnrollment = groupEnrollment;
        this.cert = cert;
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        DSIObject attestation = get("Key Attestation Type");
        if (attestation instanceof DSString) {
            groupEnrollment = "Group Enrollment".equals(attestation.toString());
        }
        if (uri == null || uri.isEmpty()) {
            DSIObject u = get("IoT Hub URI");
            uri = u instanceof DSString ? u.toString() : null;
        }
        if (cert == null || cert.isEmpty()) {
            DSIObject c = get("Certificate");
            cert = c instanceof DSString ? c.toString() : null;
        }
    }
    
    @Override
    protected void storeConfigs() {
        super.storeConfigs();
        put("Key Attestation Type", DSString.valueOf(groupEnrollment ? "Group Enrollment" : "Individual Enrollment"));
        if (uri != null) {
            put("IoT Hub URI", DSString.valueOf(uri)).setReadOnly(true);
        }
        if (cert != null) {
            put("Certificate", DSString.valueOf(cert)).setReadOnly(true);
        }
    }
    
    @Override
    protected DSAction makeEditAction() {
        DSAction act = super.makeEditAction();
        DSList enrollTypes = new DSList().add("Individual Enrollment").add("Group Enrollment");
        act.addDefaultParameter("Key Attestation Type", 
                DSFlexEnum.valueOf(groupEnrollment ? "Group Enrollment" : "Individual Enrollment", enrollTypes), null);
        act.addDefaultParameter("IoT Hub URI", uri != null ? DSString.valueOf(uri) : DSString.EMPTY, null);
        act.addDefaultParameter("Certificate", cert != null ? DSString.valueOf(cert) : DSString.EMPTY, "Either a path to the certificate PEM file, or the PEM text");
        return act;
    }
    
    @Override
    protected void editSpecifics(DSMap parameters) {
        groupEnrollment = "Group Enrollment".equals(parameters.getString("Key Attestation Type"));
        uri = parameters.getString("IoT Hub URI");
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
        info("IoT Hub URI = " + uri);
        info("Key Attestation Type = " + (groupEnrollment ? "Group Enrollment" : "Individual Enrollment"));
        info("Either certificate text or path to cert = " + cert);
        
        X509Certificate x509 = getX509();
        
        // TODO create a new DeviceClient instance using the parameters listed above
        return null;
    }
}
