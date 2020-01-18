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

- _MainNode_ - The root node of the link, has an action to add an IoT Hub Device to the view.
  - _LocalDeviceNode_ - Represents a local device.
    - _Desired Properties_ - Contains the desired properties of this device's device twin.
    - _Reported Properties_ - Contains the reported properties of this device's device twin
    - _Methods_ - Serves as the container for DirectMethod nodes.
      - _DirectMethodNode_ - Represents a direct method of its local device, which can be invoked by IoT Hub.

## Node Guide

The following section provides detailed descriptions of each node in the link as well as
descriptions of actions, values and child nodes.


### MainNode

This is the root node of the link.

**Actions**
- Add Device by Connection String - Add a child _LocalDeviceNode_ to represent and simulate the IoT Hub device with the given connection string.

**Child Nodes**
 - any _LocalDeviceNodes_ that have been added.

### LocalDeviceNode

This node represents a specific local device registered in an Azure IoT Hub.

**Actions**
- Refresh - Re-establish the connection between this device and the IoT Hub.
- Edit - Change the protocol used to communicate with the Iot Hub, or the connection string of the device.
- Send D2C Message - Send a device-to-cloud message to the IoT Hub this device is registered in.
- Upload File - Upload a file to the Azure storage container associated with the IoT Hub.
- Reported Properties/Add Reported Property - Creates a reported property value, and sends it to the IoT Hub to update this device's twin in the IoT Hub.

**Values**
- STATUS - Status of this device's connection to the IoT Hub.
- Cloud-to-Device Messages - A list of cloud-to-device messages that this device has received from the IoT Hub.

**Child Nodes**
 - D2C Rules - Holds _D2CRuleNodes_
 - Methods - Holds _DirectMethodNodes_
 - Desired Properties - Holds the desired properties of this device's device twin, retrieved from the IoT Hub.
 - Reported Properties - Holds this device's reported properties and the action that creates them.

### D2C Rules

Holds _D2CRuleNodes_ associated with its parent _LocalDeviceNode_.

**Actions**
- Add Rule - Create a _D2CRuleNode_ to configure automatic sending of device-to-cloud messages, providing a DSA path to watch and a format for the messages.

**Child Nodes**
 - any _D2CRuleNodes_ that have been added.

### D2CRuleNode

Defines a subscription to a DSA path, which will send its updates to IoT Hub as device-to-cloud messages

**Parameters (for the `Add Rule` and `Edit` actions)**
- `Subscribe Path` - The DSA path to subscribe to.
- `Properties` - A map of properties to be sent with each D2C message created by this rule. If you want to use the value, timestamp, or status of an update in the properties, use the placeholders `%VALUE%`, `%TIMESTAMP%` and `%STATUS%`.
  - e.g. `{"node_name":"kWh", "timestamp":"%TIMESTAMP%"}`
- `Body` - The message body.  Once again, use %VALUE%, %TIMESTAMP% and %STATUS% as placeholders.
  - e.g. `The new value is %VALUE%`
- Minimum Refresh Rate: Optional, ensures that at least this many seconds elapse between updates. This means that the DSLink will suppress updates that are too close together. (Leave this parameter as 0 to not use this feature.)
- Maximum Refresh Rate: Optional, ensures that an update gets sent every this many seconds. This means that if the DSA value updates too infrequently, the DSLink will send duplicate updates. (Leave this parameter as 0 to not use this feature.)

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


Apache Commons Lang 3.0

This software contains unmodified binary redistributions of 
[commons-lang3](https://commons.apache.org/proper/commons-lang/), 
which is licensed and available under the Apache License 2.0. An original copy of the license agreement 
can be found at https://git-wip-us.apache.org/repos/asf?p=commons-lang.git;a=blob;f=LICENSE.txt;h=d645695673349e3947e8e5ae42332d0ac3164cd7;hb=HEAD

## Changelog

### Version 1.1.0

- Service-Client functionality (previously under the REMOTE node, e.g. sending cloud-to-device messages) has been removed from the DSLink. The DSLink is now exclusively for acting as one or more IoT Hub devices.
  - What was previously the LOCAL node is now the root node of the DSLink. 
  - The ability to create new IoT Hub devices has been removed, as this used the service client to register the device identity with IoT Hub. Devices may now only be added by their connection strings (which can be found/generated on the [Azure Portal](https://portal.azure.com).
  - `nodes.zip` files from the previous version of the DSLink (1.0.13) will no longer work, but can be converted with a python script (see below).
  - paths to the DSLink (in dataflows, etc.) will need to be updated. E.g. `/downstream/iothub/main/exampleHub/Local/exampleDevice/...` should become `/downstream/iothub/main/exampleDevice/...`
- The device node now shows more detailed and correct information about the status of the connection to IoT Hub.
- Upon disconnect, the DSLink will try to re-establish the connection.

#### Python 2 script iothub_convert.py

Use [iothub_convert.py](https://github.com/iot-dsa-v2/dslink-java-v2-iothub/blob/develop/iothub_convert.py) to convert old nodes to the new nodes.

Assuming **oldnodes.zip** is your old nodes file. This will convert **oldnodes.zip** into a nodes file that can be used by the version 1.1.0 of the IoT Hub DSLink, and save the result in **newnodes.zip**.

Usage:

```bash
python iothub_convert.py oldnodes.zip newnodes.zip
```
