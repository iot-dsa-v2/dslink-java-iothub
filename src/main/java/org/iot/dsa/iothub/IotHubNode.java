package org.iot.dsa.iothub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.iot.dsa.DSRuntime;
import org.iot.dsa.dslink.DSRequestException;
import org.iot.dsa.iothub.node.RemovableNode;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSJavaEnum;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.ActionSpec;
import org.iot.dsa.node.action.ActionSpec.ResultType;
import org.iot.dsa.node.action.ActionTable;
import org.iot.dsa.node.action.DSAction;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.service.FileUploadNotification;
import com.microsoft.azure.sdk.iot.service.FileUploadNotificationReceiver;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceMethod;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.microsoft.azure.servicebus.ServiceBusException;

public class IotHubNode extends RemovableNode {
	private String connectionString;
	
	private DSNode localNode;
	private DSNode remoteNode;

	private DeviceMethod methodClient;
	private DeviceTwin twinClient;
	
	public IotHubNode() {
		//
	}

	public IotHubNode(String connectionString) {
		this.connectionString = connectionString;
	}
	
	public String getConnectionString() {
		return connectionString;
	}
	
	public DeviceMethod getMethodClient() {
		if (methodClient == null) {
			createMethodClient();
		}
		return methodClient;
	}
	
	public DeviceTwin getTwinClient() {
		if (twinClient == null) {
			createTwinClient();
		}
		return twinClient;
	}
	
	@Override
	protected void declareDefaults() {
		super.declareDefaults();
		declareDefault("Local", new LocalNode());
		declareDefault("Remote", new RemoteNode());
		declareDefault("Read_Messages", makeReadMessagesAction());
		declareDefault("Get_File_Upload_Notifications", makeReadFileNotificationsAction());
	}
	
	public static class LocalNode extends DSNode {
		public LocalNode() {
			super();
		}
		@Override
		protected void declareDefaults() {
			super.declareDefaults();
			declareDefault("Create_Local_Device", makeCreateDeviceAction());
		}
	}
	
	public static class RemoteNode extends DSNode {
		public RemoteNode() {
			super();
		}
		@Override
		protected void declareDefaults() {
			super.declareDefaults();
			declareDefault("Add_Remote_Device", makeAddDeviceAction());
		}
	}

	@Override
	protected void onStarted() {
		if (connectionString == null) {
			DSIObject cs = get("Connection_String");
			connectionString = cs instanceof DSString ? ((DSString) cs).toString() : "";
		}
	}
	
	@Override
	protected void onStable() {
		localNode = getNode("Local");
		remoteNode = getNode("Remote");
		init();
	}
	
	private void init() {
		put("Connection_String", DSString.valueOf(connectionString)).setReadOnly(true);
		createMethodClient();
		createTwinClient();
		put("Edit", makeEditAction()).setTransient(true);
	}
	
	private void createMethodClient() {
		try {
			methodClient = DeviceMethod.createFromConnectionString(connectionString);
		} catch (IOException e) {
			warn("Error creating method client: " + e);
		}
	}
	
	private void createTwinClient() {
		try {
			twinClient = DeviceTwin.createFromConnectionString(connectionString);
		} catch (IOException e) {
			warn("Error creating twin client: " + e);
		}
	}
	
	private DSAction makeEditAction() {
		DSAction act = new DSAction() {
			@Override
			 public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
				((IotHubNode) info.getParent()).edit(invocation.getParameters());
				return null;
			}
		};
		act.addDefaultParameter("Connection_String", DSString.valueOf(connectionString), null);
		return act;
	}

	private static DSAction makeCreateDeviceAction() {
		DSAction act = new DSAction() {
			@Override
			 public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
				((IotHubNode) info.getParent().getParent()).createDevice(invocation.getParameters());
				return null;
			}
		};
		act.addParameter("Device_ID", DSValueType.STRING, null);
		act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubClientProtocol.MQTT), null);
		return act;
	}

	private DSAction makeReadMessagesAction() {
		DSAction act = new DSAction() {
			@Override
			 public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
				return ((IotHubNode) info.getParent()).readMessages(info, invocation);
			}
		};
		act.addDefaultParameter("EventHub_Compatible_Name", DSString.valueOf("iothub-ehub-danielfree-172452-a48c3b34bf"), null);
		act.addDefaultParameter("EventHub_Compatible_Endpoint", DSString.valueOf("Endpoint=sb://ihsuprodbyres053dednamespace.servicebus.windows.net/;SharedAccessKeyName=iothubowner;SharedAccessKey=mBIqQQgZsYgvJ/la4G7KkHZMBzTX4pk3HvF2aabB/LU="), null);
		act.addParameter("Partition_ID", DSValueType.STRING, null).setPlaceHolder("0");
		act.addParameter("Start Time", DSValueType.STRING, "Optional - defaults to 'now'").setPlaceHolder("Optional");
		act.setResultType(ResultType.STREAM_TABLE);
		return act;
	}

	private static DSAction makeAddDeviceAction() {
		DSAction act = new DSAction() {
			@Override
			 public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
				((IotHubNode) info.getParent().getParent()).addDevice(invocation.getParameters());
				return null;
			}
		};
		act.addParameter("Device_ID", DSValueType.STRING, null);
		return act;
	}
	
	private static DSAction makeReadFileNotificationsAction() {
		DSAction act = new DSAction() {
			@Override
			 public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
				return ((IotHubNode) info.getParent()).readFileNotifications(info, invocation);
			}
		};
		act.addParameter("Protocol", DSJavaEnum.valueOf(IotHubServiceClientProtocol.AMQPS), null);
		act.setResultType(ResultType.STREAM_TABLE);
		return act;
	}
	
	
	private void edit(DSMap parameters) {
		connectionString = parameters.getString("Connection_String");
		init();
	}
	
	private void addDevice(DSMap parameters) {
		String id = parameters.getString("Device_ID");
		remoteNode.add(id, new RemoteDeviceNode(this, id));
	}
	
	private void createDevice(DSMap parameters) {
		String id = parameters.getString("Device_ID");
		String protocolStr = parameters.getString("Protocol");
		IotHubClientProtocol protocol = IotHubClientProtocol.valueOf(protocolStr);
		localNode.add(id, new LocalDeviceNode(this, id, protocol));
	}
	
	private ActionResult readMessages(DSInfo actionInfo, ActionInvocation invocation) {
		final DSAction action = actionInfo.getAction();
		DSMap parameters = invocation.getParameters();
		String name = parameters.getString("EventHub_Compatible_Name");
		String endpt = parameters.getString("EventHub_Compatible_Endpoint");
		String connStr = endpt + ";EntityPath=" + name;
		String partitionId = parameters.getString("Partition_ID");
		String startStr = parameters.getString("Start Time");
		Instant start = null;
		if (startStr != null) {
			try {
				start = Instant.parse(startStr);
			} catch (DateTimeParseException e) {
				startStr = startStr.trim();
				String suffix = "T00:00:00.00Z";
				int len = startStr.length();
				if (len >= 10 && len < 23) {
					startStr += suffix.substring(len - 10);
					try {
						start = Instant.parse(startStr);
					} catch (DateTimeParseException e1) {
					}
				}
			}
		}
		
		
		EventHubClient client = null;
		try {
			client = EventHubClient.createFromConnectionStringSync(connStr);
			receiveMessages(client, partitionId, start, invocation);
		} catch(Exception e) {
			warn("Failed to create receiver: " + e.getMessage());
			throw new  DSRequestException(e.getMessage());
		}
		
		return new ActionTable() {
			private List<DSMap> cols;
			
			
			@Override
			public Iterator<DSList> getRows() {
				return new ArrayList<DSList>().iterator();
			}
			
			@Override
			public Iterator<DSMap> getColumns() {
				if (cols == null) {
					cols = new ArrayList<DSMap>();
					cols.add(Util.makeColumn("Offset", DSValueType.STRING));
					cols.add(Util.makeColumn("Sequence_Number", DSValueType.NUMBER));
					cols.add(Util.makeColumn("Enqueued_Time", DSValueType.STRING));
					cols.add(Util.makeColumn("Device_ID", DSValueType.STRING));
					cols.add(Util.makeColumn("Message_Payload", DSValueType.STRING));
					cols.add(Util.makeColumn("Parameters", DSValueType.MAP));
				}
				return cols.iterator();
			}

			@Override
			public ActionSpec getAction() {
				return action;
			}

			@Override
			public void onClose() {
			}
		};
	}

	private void receiveMessages(final EventHubClient client, final String partitionId,
			final Instant start, final ActionInvocation invocation) throws ServiceBusException {
		client.createReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, partitionId, start != null ? start : Instant.now())
				.thenAccept(new Consumer<PartitionReceiver>() {
					public void accept(PartitionReceiver receiver) {
						try {
							while (invocation.isOpen()) {
								Iterable<EventData> receivedEvents = receiver.receive(100).get();
								if (receivedEvents != null) {
									for (EventData receivedEvent : receivedEvents) {
										String offset = receivedEvent.getSystemProperties().getOffset();
										long seqNo = receivedEvent.getSystemProperties().getSequenceNumber();
										Instant enqTime = receivedEvent.getSystemProperties().getEnqueuedTime();
										Object deviceId = receivedEvent.getSystemProperties().get("iothub-connection-device-id");
										String payload = new String(receivedEvent.getBytes(), Charset.defaultCharset());
										DSList row = new DSList().add(offset).add(seqNo).add(enqTime.toString());
										row.add(deviceId != null ? deviceId.toString() : null);
										row.add(payload);
										DSMap properties = row.addMap();
										for(Entry<String, Object> entry: receivedEvent.getProperties().entrySet()) {
											properties.put(entry.getKey(), entry.getValue().toString());
										}
										invocation.send(row);
									}
								}
							}
						} catch (Exception e) {
							warn("Failed to receive messages: " + e.getMessage());
							invocation.close(new DSRequestException(e.getMessage()));
						} finally {
							try {
								client.closeSync();
							} catch (ServiceBusException e) {
								warn("Failed to close Client: " + e.getMessage());
							}
						}
					}
				});
	}
	
	private ActionResult readFileNotifications(DSInfo actionInfo, final ActionInvocation invocation) {
		final DSAction action = actionInfo.getAction();
		DSMap parameters = invocation.getParameters();
		String protocolStr = parameters.getString("Protocol");
		IotHubServiceClientProtocol protocol = protocolStr.endsWith("WS") ? IotHubServiceClientProtocol.AMQPS_WS : IotHubServiceClientProtocol.AMQPS;
		final ServiceClient serviceClient;
		final FileUploadNotificationReceiver fileUploadNotificationReceiver;
		try {
			serviceClient = ServiceClient.createFromConnectionString(connectionString, protocol);
			serviceClient.open();
			fileUploadNotificationReceiver = serviceClient.getFileUploadNotificationReceiver();
			fileUploadNotificationReceiver.open();
		} catch(Exception e) {
			warn("Failed to create receiver: " + e.getMessage());
			throw new  DSRequestException(e.getMessage());
		}
		
		if (fileUploadNotificationReceiver != null) {
			DSRuntime.run(new Runnable() {
				@Override
				public void run() {
					receiveFileNotifications(fileUploadNotificationReceiver, invocation);
				}
			});
		}
		return new ActionTable() {
			private List<DSMap> cols;
			
			@Override
			public void onClose() {
				if (fileUploadNotificationReceiver != null) {
					try {
						fileUploadNotificationReceiver.close();
					} catch (IOException e) {
					}
				}
				if (serviceClient != null) {
					try {
						serviceClient.close();
					} catch (IOException e) {
					}
				}
			}
			
			@Override
			public ActionSpec getAction() {
				return action;
			}
			
			@Override
			public Iterator<DSList> getRows() {
				return new ArrayList<DSList>().iterator();
			}
			
			@Override
			public Iterator<DSMap> getColumns() {
				if (cols == null) {
					cols = new ArrayList<DSMap>();
					cols.add(Util.makeColumn("Enqueued_Time", DSValueType.STRING));
					cols.add(Util.makeColumn("Device_ID", DSValueType.STRING));
					cols.add(Util.makeColumn("Blob_URI", DSValueType.STRING));
					cols.add(Util.makeColumn("Blob_Name", DSValueType.STRING));
					cols.add(Util.makeColumn("Last_Updated", DSValueType.STRING));
					cols.add(Util.makeColumn("Blob_Size(Bytes)", DSValueType.NUMBER));
				}
				return cols.iterator();
			}
		};
	}
	
	private void receiveFileNotifications(FileUploadNotificationReceiver receiver, ActionInvocation invocation) {
		try {
			while (invocation.isOpen()) {
				System.out.println("Recieve file upload notifications...");
				FileUploadNotification notification = receiver.receive();
				if (notification != null) {
					String enqTime = notification.getEnqueuedTimeUtcDate().toInstant().toString();
					String devId = notification.getDeviceId();
					String blobUri = notification.getBlobUri();
					String blobName = notification.getBlobName();
					String lastUpdate = notification.getLastUpdatedTimeDate().toInstant().toString();
					Long blobBytes = notification.getBlobSizeInBytes();
					DSList row = new DSList().add(enqTime).add(devId).add(blobUri).add(blobName).add(lastUpdate).add(blobBytes);
					invocation.send(row);
				}
			}
		} catch (Exception e) {
			warn(e);
			invocation.close(e);
		}
	}
	
	
}
