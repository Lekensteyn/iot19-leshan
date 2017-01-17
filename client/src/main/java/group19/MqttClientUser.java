package group19;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

public interface MqttClientUser {
	/**
	 * Invoked when the MQTT broker connection is ready.
	 * 
	 * @param client
	 */
	public void setMqttClient(MqttAsyncClient client);
}
