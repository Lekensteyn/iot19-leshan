package group19;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import group19.SensorDevice.SensorState;

public class LightDevice extends BaseInstanceEnabler
		implements SmartLightEventListener, MqttClientUser, IMqttMessageListener {

	private final static Logger LOG = LoggerFactory.getLogger(LightDevice.class);

	private String lightId = "";
	private String deviceType = "Light Device";
	private LightState lightState = LightState.FREE;
	private UserType userType = UserType.USER3;
	private String userId = "";
	private RGBColor lightColor = new RGBColor(255, 255, 255);
	private boolean lowLight;
	private long groupNo;
	private double locationX;
	private double locationY;
	private String roomId = "";
	private BehaviorDeployment behaviorDeployment = BehaviorDeployment.Distributed;

	private LightProvider realDevice;
	private SmartLight smartLight;
	private String ownershipPriorityJson;
	private MqttAsyncClient mqttClient;
	private String mqttTopicFilter;

	public LightDevice(String lightId) {
		this.lightId = lightId;

		final RPiLightDevice realDevice = new RPiLightDevice();
		try {
			realDevice.start();
			this.realDevice = realDevice;
			// yep, nasty, there is probably a better place to add this.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					realDevice.destroy();
				}
			});
		} catch (IOException e) {
			LOG.error("Failed to start light device controller, state will not be updated!", e);
		}

		if (behaviorDeployment == BehaviorDeployment.Distributed) {
			startDistributedBehavior();
		}
	}

	@Override
	public ReadResponse read(int resourceid) {
		switch (resourceid) {
		case 0:
			return ReadResponse.success(resourceid, lightId);
		case 1:
			return ReadResponse.success(resourceid, deviceType);
		case 2:
			return ReadResponse.success(resourceid, lightState.name());
		case 3:
			return ReadResponse.success(resourceid, userType.name());
		case 4:
			return ReadResponse.success(resourceid, userId);
		case 5:
			return ReadResponse.success(resourceid, lightColor.toString());
		case 6:
			return ReadResponse.success(resourceid, lowLight);
		case 7:
			return ReadResponse.success(resourceid, groupNo);
		case 8:
			return ReadResponse.success(resourceid, locationX);
		case 9:
			return ReadResponse.success(resourceid, locationY);
		case 10:
			return ReadResponse.success(resourceid, roomId);
		case 11:
			return ReadResponse.success(resourceid, behaviorDeployment.name());

		default:
			return super.read(resourceid);
		}
	}

	@Override
	public WriteResponse write(int resourceid, LwM2mResource value) {
		switch (resourceid) {
		case 2: /* Light State */
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				return WriteResponse.badRequest("Resource cannot be written in distributed mode");
			}
			try {
				setLightState((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 3: /* User Type */
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				return WriteResponse.badRequest("Resource cannot be written in distributed mode");
			}
			try {
				setUserType((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 4: /* User Id */
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				smartLight.setUser3((String) value.getValue());
			} else {
				setUserId((String) value.getValue());
			}
			return WriteResponse.success();
		case 5: /* Light Color */
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				return WriteResponse.badRequest("Resource cannot be written in distributed mode");
			}
			try {
				setLightColor((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 6: /* Low Light mode */
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				return WriteResponse.badRequest("Resource cannot be written in distributed mode");
			}
			setLowLightMode((boolean) value.getValue());
			return WriteResponse.success();
		case 7:
			groupNo = (long) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 8:
			locationX = (double) value.getValue();
			if (smartLight != null) {
				smartLight.setLocation(locationX, locationY);
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 9:
			locationY = (double) value.getValue();
			if (smartLight != null) {
				smartLight.setLocation(locationX, locationY);
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 10: /* Room Id */
			try {
				setRoomId((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 11:
			try {
				behaviorDeployment = BehaviorDeployment.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			// Try to start distributed behavior as needed
			if (behaviorDeployment == BehaviorDeployment.Distributed) {
				startDistributedBehavior();
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 12:
			try {
				setOwnershipPriorityURL((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 13:
			try {
				setLightBehaviorURL((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		default:
			return super.write(resourceid, value);
		}
	}

	@Override
	public ExecuteResponse execute(int resourceid, String params) {
		switch (resourceid) {
		default:
			return super.execute(resourceid, params);
		}
	}

	private void startDistributedBehavior() {
		if (smartLight != null) {
			// smartLight.destroy();
			// smartLight = null;
			// Already running, do not restart.
			return;
		}
		try {
			smartLight = new SmartLight();
			smartLight.setListener(this);
			smartLight.start();
			smartLight.setLocation(locationX, locationY);
			if (ownershipPriorityJson != null) {
				smartLight.setOwnership(ownershipPriorityJson);
			}
		} catch (IOException e) {
			LOG.warn("Failed to start smart behavior", e);
		}
	}

	private void setLightState(String value) {
		lightState = LightState.valueOf(value);
		fireResourcesChange(2);
	}

	private void setUserType(String value) {
		userType = UserType.valueOf(value);
		fireResourcesChange(3);
	}

	private void setUserId(String value) {
		userId = value;
		fireResourcesChange(4);
	}

	private void setLightColor(String value) {
		lightColor = new RGBColor(value);
		if (realDevice != null) {
			realDevice.setColor(lightColor.r, lightColor.g, lightColor.b);
		}
		fireResourcesChange(5);
	}

	private void setLowLightMode(boolean value) {
		lowLight = value;
		if (realDevice != null) {
			realDevice.setLowLightMode(lowLight);
		}
		fireResourcesChange(6);
	}

	public void setRoomId(String roomId) {
		if (roomId.contains("/")) {
			throw new IllegalArgumentException("Invalid argument");
		}
		if (!this.roomId.equals(roomId)) {
			this.roomId = roomId;
			fireResourcesChange(10);
			subscribeToSensors();
		}
	}

	private void setOwnershipPriorityURL(String url) {
		LOG.info("Setting Ownership Priority URL to: " + url);
		new Thread(new Download(url) {

			@Override
			public void onError(IOException e) {
				LOG.warn("Failed to retrieve ownership priority file", e);
			}

			@Override
			public void onComplete(byte[] data) {
				String text;
				try {
					text = new String(data, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOG.warn("Failed to decode data", e);
					return;
				}
				// TODO validate JSON
				ownershipPriorityJson = text;
				if (smartLight != null) {
					smartLight.setOwnership(ownershipPriorityJson);
				}
			}
		}).start();
	}

	private void setLightBehaviorURL(String url) {
		LOG.info("Setting Light Behavior URL to: " + url);
		new Thread(new Download(url) {

			@Override
			public void onError(IOException e) {
				LOG.warn("Failed to retrieve Light Behavior file");
			}

			@Override
			public void onComplete(byte[] data) {
				byte[] program;
				try {
					SignedData signedData = SignedData.load(data);
					program = signedData.getVerifiedData();
				} catch (SignatureException e) {
					LOG.error("Failed to verify light behavior program", e);
					return;
				}
				LOG.info("Received valid program of {} bytes", program.length);
				// TODO use program
			}
		}).start();
	}

	public void childValueEmitted(String key, String value) {
		if (behaviorDeployment != BehaviorDeployment.Distributed) {
			LOG.info("Ignoring child because not distributed mode.");
			return;
		}
		switch (key) {
		case "color":
			setLightColor(value);
			break;
		case "lowlight":
			setLowLightMode(Boolean.valueOf(value));
			break;
		case "userid":
			setUserId(value);
			break;
		case "state":
			setLightState(value);
			break;
		case "usertype":
			setUserType(value);
			break;
		default:
			LOG.warn("Unhandled option from child: " + key);
		}
	}

	@Override
	public void setMqttClient(MqttAsyncClient client) {
		this.mqttClient = client;
		subscribeToSensors();
	}

	private void subscribeToSensors() {
		if (mqttClient == null) {
			return;
		}
		if (roomId.isEmpty()) {
			LOG.warn("Not subscribing to sensor state, room is unknown");
			return;
		}
		String topic = String.format("TUE/%s/Sensor/%s/State", roomId, "+");
		if (mqttTopicFilter != null && mqttTopicFilter.equals(topic)) {
			LOG.info("Topic did not change, skipping re-subscription");
			return;
		}
		try {
			// unsubscribe from old filter
			if (mqttTopicFilter != null) {
				mqttClient.unsubscribe(mqttTopicFilter);
			}
			mqttTopicFilter = topic;
			mqttClient.subscribe(topic, 0, this);
			LOG.info("Subscribed to MQTT topic " + topic);
		} catch (MqttException e) {
			LOG.warn("Failed to subscribe to MQTT topic", e);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String[] parts = topic.split("/");
		String roomId = parts[1];
		String sensorId = parts[3];
		String payload = new String(message.getPayload());
		// We subscribed only to the long topic, so assume sufficient parts.
		LOG.info("MQTT message received: roomId={} sensorId={} payload={}", roomId, sensorId, payload);

		boolean isOccupied = SensorState.valueOf(payload) == SensorState.OCCUPIED;
		if (smartLight != null) {
			smartLight.notifySensorOccupied(sensorId, isOccupied);
		}
	}

	// represents the state of the light device
	enum LightState {
		USED, FREE
	}

	// stating which lighting behavior deployment is used
	enum BehaviorDeployment {
		Broker, Distributed
	}

	enum UserType {
		USER1, USER2, USER3
	}
}
