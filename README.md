# dslink-java-v2-iothub

* Java - version 1.8 and up.
* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## Overview

This is a link for interacting with Microsoft Azure IoT Hub.

If you are not familiar with DSA and links, an overview can be found at
[here](http://iot-dsa.org/get-started/how-dsa-works).

This link was built using the DSLink Java SDK which can be found
[here](https://github.com/iot-dsa-v2/sdk-dslink-java-v2).


## Link Architecture

This section outlines the hierarchy of nodes defined by this link.

- _MainNode_ - The root node of the link, has an action to add an IoT Hub to the view.
  - _IotHubNode_ - A node representing a specific IoT Hub.
    - _Local_ - Serves as the container for Local Device nodes.
      - _LocalDeviceNode_ - Represents a local device.
        - _Desired Properties_ - Contains the desired properties of this device's device twin.
        - _Reported Properties_ - Contains the reported properties of this device's device twin
        - _Methods_ - Serves as the container for DirectMethod nodes.
          - _DirectMethodNode_ - Represents a direct method of its local device, which can be invoked by IoT Hub.
    - _Remote_ - Serves as the container for Remote Device nodes.
      - _RemoteDeviceNode_ - Represents a specific remote device.
        - _Desired Properties_ - Contains the desired properties of this device's device twin.
        - _Reported Properties_ - Contains the reported properties of this device's device twin.
        - _Tags_ - Contains the tags of this device's device twin.


## Node Guide

The following section provides detailed descriptions of each node in the link as well as
descriptions of actions, values and child nodes.


### MainNode

This is the root node of the link.

**Actions**
- Add IoT Hub - Connect to an IoT Hub and add a child _IotHubNode_ to represent it. 
  - `Connection String` - The connection string of the IoT Hub, [found under _Shared Access Policies_ in the Azure Portal.]( https://raw.githubusercontent.com/iot-dsa-v2/dslink-java-v2-iothub/develop/docs/connstring1.png)  

**Child Nodes**
 - any _IotHubNodes_ that have been added.

### IotHubNode

This node represents a specific Azure IoT Hub.

**Actions**
- Remove - Remove this node.
- Edit - Edit the connection string and try to connect again.
- Read Messages - Read device-to-cloud messages from this IoT Hub's EventHub-compatible endpoint.
  - EventHub Compatible Name - [Found under _Endpoints_ in the Azure Portal.](https://raw.githubusercontent.com/iot-dsa-v2/dslink-java-v2-iothub/develop/docs/eventhubname.png)
  - EventHub Compatible Endpoint - [Found under _Endpoints_ in the Azure Portal.](https://raw.githubusercontent.com/iot-dsa-v2/dslink-java-v2-iothub/develop/docs/eventhubendpt.png)
  - Partition ID - The partition of the endpoint to read from.
  - Start Time - The time from which to start reading messages, defaults to the time of invocation.
- Get File Upload Notifications - Read any file upload notifications that this IoT Hub receives. These get sent to the IoT Hub whenever one of its devices uploads a file to Azure blob storage.

**Child Nodes**
- Local - Holds _LocalDeviceNodes_.
- Remote - Holds _RemoteDeviceNodes_.

### Local

Holds _LocalDeviceNodes_ associated with its parent _IotHubNode_.

**Actions**
- Create Local Device - Register a new device with the IoT Hub and add a child _LocalDeviceNode_ to represent and simulate it.

**Child Nodes**
 - any _LocalDeviceNodes_ that have been added.

### LocalDeviceNode

This node represents a specific local device registered in an Azure IoT Hub.

**Actions**
- Remove - Remove this node.
- Refresh - Re-establish the connection between this device and the IoT Hub.
- Edit - Change the protocol used to communicate with the Iot Hub.
- Send D2C Message - Send a device-to-cloud message to the IoT Hub this device is registered in.
- Upload File - Upload a file to the Azure storage container associated with the IoT Hub.

- Reported Properties/Add Reported Property - Creates a reported property value, and sends it to the IoT Hub to update this device's twin in the IoT Hub.

**Values**
- STATUS - Status of this device's connection to the IoT Hub.
- Cloud-to-Device Messages - A list of cloud-to-device messages that this device has received from the IoT Hub.

**Child Nodes**
 - Methods - Holds _DirectMethodNodes_
 - Desired Properties - Holds the desired properties of this device's device twin, retrieved from the IoT Hub.
 - Reported Properties - Holds this device's reported properties and the action that creates them.

### Methods

Holds _DirectMethodNodes_ associated with its parent _LocalDeviceNode_.

**Actions**
- Add Direct Method - Create a _DirectMethodNode_, optionally providing a DSA path that it should forward invocations to.

**Child Nodes**
 - any _DirectMethodNodes_ that have been added.

### DirectMethodNode

This node represents a direct method of a local device. The IoT Hub that the device is registered in can invoke this method. Whenever this happens, details of the invocation will be stored by this node. If this node has an associated path to a DSA action, then this will also cause that action to be invoked.

**Actions**
- Remove - Remove this node.

**Values**
- Invocations - A list of attempts by the IoT Hub to invoke this direct method, with time stamps and invocation parameters.

### Remote

Holds _RemoteDeviceNodes_ associated with its parent _IotHubNode_.

**Actions**
- Add Remote Device - Select one of the devices registered in this IoT Hub and add a child _RemoteDeviceNode_ to represent it.

**Child Nodes**
 - any _RemoteDeviceNodes_ that have been added.

### RemoteDeviceNode

This node represents a specific IoT Hub Device.

**Actions**
- Remove - Remove this node.
- Refresh - Re-retrieve the device twin from the IoT Hub and update Tags and Desired/Reported Properties.
- Invoke Direct Method - Invoke a direct method of this device.
- Send C2D Message - Send a cloud-to-device message to this device.

- Desired Properties/Add Desired Property - Creates a desired property value, and sends it to the IoT Hub to update this device's twin in the IoT Hub.
- Tags/Add Tag - Creates a tag value, and sends it to the IoT Hub to update this device's twin in the IoT Hub.

**Child Nodes**
 - Desired Properties - Holds the desired properties of this device's device twin, retrieved from the IoT Hub. Also holds the action for creating additional desired properties.
 - Reported Properties - Holds the reported properties of this device's device twin, retrieved from the IoT Hub.
 - Tags - Holds the tags of this device's device twin, retrieved from the IoT Hub. Also holds the action for creating additional tags.


## Acknowledgements

SDK-DSLINK-JAVA

This software contains unmodified binary redistributions of 
[sdk-dslink-java-v2](https://github.com/iot-dsa-v2/sdk-dslink-java-v2), which is licensed 
and available under the Apache License 2.0. An original copy of the license agreement can be found 
at https://github.com/iot-dsa-v2/sdk-dslink-java-v2/blob/master/LICENSE


Microsoft Azure IoT SDKs for Java

This software contains unmodified binary redistributions of 
[azure-iot-sdk-java](https://github.com/Azure/azure-iot-sdk-java), 
which is licensed and available under the MIT License. An original copy of the license agreement 
can be found at https://github.com/Azure/azure-iot-sdk-java/blob/master/LICENSE


Microsoft Azure Event Hubs Client for Java

This software contains unmodified binary redistributions of 
[azure-event-hubs-java](https://github.com/Azure/azure-event-hubs-java), 
which is licensed and available under the MIT License. An original copy of the license agreement 
can be found at https://github.com/Azure/azure-event-hubs-java/blob/dev/LICENSE


Apache Commons Lang 3.0

This software contains unmodified binary redistributions of 
[commons-lang3](https://commons.apache.org/proper/commons-lang/), 
which is licensed and available under the Apache License 2.0. An original copy of the license agreement 
can be found at https://git-wip-us.apache.org/repos/asf?p=commons-lang.git;a=blob;f=LICENSE.txt;h=d645695673349e3947e8e5ae42332d0ac3164cd7;hb=HEAD

## History

* Version 1.0.2
  - Dependency update.
* Version 1.0.0
  - Hello World

