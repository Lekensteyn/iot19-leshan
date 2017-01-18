package group19;

import java.io.IOException;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorDevice extends BaseInstanceEnabler implements SensorChangeListener, MqttClientUser {

	private final static Logger LOG = LoggerFactory.getLogger(SensorDevice.class);

	private String sensorId;
	private String deviceType = "Sensor Device";
	private SensorState sensorState = SensorState.FREE;
	private String userId = "";
	private long groupNo;
	private double locationX;
	private double locationY;
	private String roomId = "";

	private RPiSensorDevice realDevice;
	private MqttAsyncClient mqttClient;

	public SensorDevice(String sensorId) {
		this.sensorId = sensorId;
		realDevice = new RPiSensorDevice(this);
		try {
			realDevice.start();
		} catch (IOException e) {
			LOG.error("Failed to start sensor watcher, state will not be updated!", e);
			realDevice = null;
		}

		if (realDevice != null) {
			new Thread(realDevice).start();
			// yep, nasty, there is probably a better place to add this.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					realDevice.destroy();
				}
			});
		}
	}

	@Override
	public ReadResponse read(int resourceid) {
		switch (resourceid) {
		case 0:
			return ReadResponse.success(resourceid, sensorId);
		case 1:
			return ReadResponse.success(resourceid, deviceType);
		case 2:
			return ReadResponse.success(resourceid, sensorState.name());
		case 3:
			return ReadResponse.success(resourceid, userId);
		case 4:
			return ReadResponse.success(resourceid, groupNo);
		case 5:
			return ReadResponse.success(resourceid, locationX);
		case 6:
			return ReadResponse.success(resourceid, locationY);
		case 7:
			return ReadResponse.success(resourceid, roomId);

		default:
			return super.read(resourceid);
		}
	}

	@Override
	public WriteResponse write(int resourceid, LwM2mResource value) {
		switch (resourceid) {
		case 2:
			try {
				sensorState = SensorState.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 3:
			userId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 4:
			groupNo = (long) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 5:
			locationX = (double) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 6:
			locationY = (double) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 7:
			try {
				setRoomId((String) value.getValue());
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

	public void setRoomId(String roomId) {
		if (roomId.contains("/")) {
			throw new IllegalArgumentException("Invalid argument");
		}
		if (!this.roomId.equals(roomId)) {
			this.roomId = roomId;
			fireResourcesChange(7);
			publishSensorState();
		}
	}

	@Override
	public void sensorChanged(boolean inUse) {
		SensorState sensorState = inUse ? SensorState.OCCUPIED : SensorState.FREE;
		if (this.sensorState != sensorState) {
			this.sensorState = sensorState;
			fireResourcesChange(2);
			publishSensorState();
		}
	}

	public void setMqttClient(MqttAsyncClient mqttClient) {
		this.mqttClient = mqttClient;
		publishSensorState();
	}

	private void publishSensorState() {
		if (mqttClient == null) {
			return;
		}
		if (roomId.isEmpty()) {
			LOG.warn("Not publishing sensor state, room is unknown");
			return;
		}
		if (sensorId.isEmpty()) {
			LOG.warn("Not publishing sensor state, sensorId is unknown");
			return;
		}
		String topic = String.format("TUE/%s/Sensor/%s/State", roomId, sensorId);
		byte[] payload = sensorState.name().getBytes();
		try {
			mqttClient.publish(topic, payload, 0, false);
			LOG.info("Published to MQTT topic {}: {}", topic, sensorState.name());
		} catch (MqttException e) {
			LOG.warn("Failed to publish sensor state", e);
		}
	}

	// represents the state of the sensor device
	enum SensorState {
		OCCUPIED, FREE
	}

}