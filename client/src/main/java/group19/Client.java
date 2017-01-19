package group19;

import static org.eclipse.leshan.client.object.Security.noSec;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.leshan.LwM2mId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

	private final static Logger LOG = LoggerFactory.getLogger(Client.class);
	private final static String USAGE = "java -jar client.jar [OPTIONS]";

	public final static int LIGHT_PROFILE_ID = 10250;
	public final static int SENSOR_PROFILE_ID = 10350;

	// TODO move this to some other configuration class?
	private final static int GROUP_NO = 19;

	public static void main(String[] args) {
		// Define options for command line tools
		Options options = new Options();
		options.addOption("h", "help", false, "Display help information.");
		options.addOption("u", "server", true, "Set the LWM2M or Bootstrap server URL.\nDefault: localhost:5683.");
		options.addOption("m", "mqtt", true, "Set the MQTT broker address.\nDefault the LWM2M broker host.");
		options.addOption("t", "type", true, "Set the device type (light or sensor).\nDefault: light.");
		options.addOption("d", "discover", false,
				"Discover the LWM2M server through mDNS-SD (ignores the --server parameter).");
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);

		// Parse arguments
		CommandLine cl = null;
		try {
			cl = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
			formatter.printHelp(USAGE, options);
			return;
		}

		// Print help
		if (cl.hasOption("help")) {
			formatter.printHelp(USAGE, options);
			return;
		}

		// Get server URI
		String serverURI = "coap://localhost:5683";
		if (cl.hasOption("u")) {
			serverURI = "coap://" + cl.getOptionValue("u");
		}

		// set device type
		boolean isSensor = false;
		if (cl.hasOption("t")) {
			switch (cl.getOptionValue("t")) {
			case "light":
				break;
			case "sensor":
				isSensor = true;
				break;
			default:
				System.err.println("Invalid value for option \"--type\".");
				return;
			}
		}

		boolean discover = cl.hasOption("d");
		if (discover) {
			List<String> addresses = ServiceDiscovery.findAddresses();
			if (addresses.size() == 0) {
				System.err.println("Cannot discover a LWM2M server in the network");
				return;
			}
			if (addresses.size() > 1) {
				LOG.warn("Found multiple LWM2M servers, will use the first one.");
			}
			String host = addresses.get(0);
			if (host.contains(":")) {
				host = "[" + host + "]";
			}
			serverURI = "coap://" + host + ":5683";
			LOG.info("Using discovered LWM2M server: " + serverURI);
		}

		String endpoint = String.format("%s-Device-%d-1", isSensor ? "Sensor" : "Light", GROUP_NO);
		URI coapServerURI, mqttServerURI;
		try {
			coapServerURI = new URI(serverURI);
		} catch (URISyntaxException e) {
			LOG.error("Unable to parse LWM2M server " + serverURI, e);
			return;
		}

		// Default MQTT broker address to the LWM2M server.
		String mqttAddr = coapServerURI.getHost();
		if (cl.hasOption("m")) {
			mqttAddr = cl.getOptionValue("m");
		}
		try {
			mqttServerURI = new URI("tcp://" + mqttAddr);
		} catch (URISyntaxException e) {
			LOG.error("Unable to parse MQTT broker address " + mqttAddr, e);
			return;
		}

		createClient(endpoint, coapServerURI, mqttServerURI, isSensor);
	}

	private static void loadSpec(List<ObjectModel> models, String resourceName) {
		InputStream input = ObjectLoader.class.getResourceAsStream(resourceName);
		if (input == null) {
			resourceName = "/resources" + resourceName;
			input = ObjectLoader.class.getResourceAsStream(resourceName);
		}
		if (input != null) {
			LOG.info("Loading specification from " + resourceName);
			models.addAll(ObjectLoader.loadJsonStream(input));
		} else {
			LOG.error("Unable to load resource: " + resourceName);
		}
	}

	public static void createClient(String endpoint, URI coapServerURI, URI mqttServerURI, boolean isSensor) {
		// Load LWM2M specs (include default OMA objects for Firmware profile)
		List<ObjectModel> models = new ArrayList<>();
		loadSpec(models, "/oma-objects-spec.json");
		loadSpec(models, "/light-object-spec.json");
		loadSpec(models, "/sensor-object-spec.json");
		LwM2mModel model = new LwM2mModel(models);
		ObjectsInitializer initializer = new ObjectsInitializer(model);

		// Register mandatory objects (See Appendix E.1, E.2, E.4 of OMA TS)
		initializer.setInstancesForObject(LwM2mId.SECURITY, noSec(coapServerURI.toString(), 123));
		initializer.setInstancesForObject(LwM2mId.SERVER, new Server(123, 30, BindingMode.U, false));
		// Note: Device is mandatory (including things like reboot), but this is
		// not fully implemented by Leshan.
		initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Group 19", "RPi", "12345", "U"));

		// register other Objects by their ID
		final MqttClientUser mqttClientUser;
		if (isSensor) {
			SensorDevice sensorDevice = new SensorDevice(endpoint);
			initializer.setInstancesForObject(SENSOR_PROFILE_ID, sensorDevice);
			mqttClientUser = sensorDevice;
		} else {
			LightDevice lightDevice = new LightDevice(endpoint);
			initializer.setInstancesForObject(LIGHT_PROFILE_ID, lightDevice);
			mqttClientUser = lightDevice;
		}

		// creates the Object Instances
		List<LwM2mObjectEnabler> enablers = initializer.create(LwM2mId.SECURITY, LwM2mId.SERVER, LwM2mId.DEVICE);
		if (isSensor) {
			enablers.add(initializer.create(SENSOR_PROFILE_ID));
		} else {
			enablers.add(initializer.create(LIGHT_PROFILE_ID));
		}

		// Create client
		LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
		// NOTE: null and zero will use default settings
		// builder.setLocalAddress(localAddress, localPort);
		builder.setObjects(enablers);
		final LeshanClient client = builder.build();
		client.start();

		// De-register on shutdown and stop client.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// send de-registration request before destroy
				client.destroy(true);
			}
		});

		// Register MQTT client to update SmartLight or publish sensor data.
		// Use MemoryPersistence since we do not care about reliability across
		// client restarts.
		try {
			final MqttAsyncClient mqttClient = new MqttAsyncClient(mqttServerURI.toString(), endpoint,
					new MemoryPersistence());
			LOG.info("Trying to connect to MQTT broker: " + mqttServerURI);
			mqttClient.connect(null, new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken token) {
					LOG.info("Connected to MQTT broker");
					mqttClientUser.setMqttClient(mqttClient);
				}

				@Override
				public void onFailure(IMqttToken token, Throwable e) {
					LOG.error("Cannot connect to MQTT broker", e);
				}
			});
		} catch (MqttException e) {
			LOG.warn("MQTT client failed", e);
		}
	}
}
