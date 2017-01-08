package group19;

import static org.eclipse.leshan.client.object.Security.noSec;

import java.io.InputStream;
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
		options.addOption("t", "type", true, "Set the device type (light or sensor).\nDefault: light.");
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

		String endpoint = String.format("%s-Device-%d-1", isSensor ? "Sensor" : "Light", GROUP_NO);
		createClient(endpoint, serverURI, isSensor);
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

	public static void createClient(String endpoint, String serverURI, boolean isSensor) {
		// Load LWM2M specs (include default OMA objects for Firmware profile)
		List<ObjectModel> models = new ArrayList<>();
		loadSpec(models, "/oma-objects-spec.json");
		loadSpec(models, "/light-object-spec.json");
		loadSpec(models, "/sensor-object-spec.json");
		LwM2mModel model = new LwM2mModel(models);
		ObjectsInitializer initializer = new ObjectsInitializer(model);

		// Register mandatory objects (See Appendix E.1, E.2, E.4 of OMA TS)
		initializer.setInstancesForObject(LwM2mId.SECURITY, noSec(serverURI, 123));
		initializer.setInstancesForObject(LwM2mId.SERVER, new Server(123, 30, BindingMode.U, false));
		// Note: Device is mandatory (including things like reboot), but this is
		// not fully implemented by Leshan.
		initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Group 19", "RPi", "12345", "U"));

		// register other Objects by their ID
		if (isSensor) {
			SensorDevice dev = new SensorDevice(endpoint);
			initializer.setInstancesForObject(SENSOR_PROFILE_ID, dev);
		} else {
			LightDevice dev = new LightDevice(endpoint);
			initializer.setInstancesForObject(LIGHT_PROFILE_ID, dev);
			// TODO somehow multiple instances are not working, only ownership
			// is available
			initializer.setInstancesForObject(LwM2mId.FIRMWARE, dev.getLightFirmareUpdate());
			initializer.setInstancesForObject(LwM2mId.FIRMWARE, dev.getOwnershipFirmwareUpdate());
		}

		// creates the Object Instances
		List<LwM2mObjectEnabler> enablers = initializer.create(LwM2mId.SECURITY, LwM2mId.SERVER, LwM2mId.DEVICE);
		if (isSensor) {
			enablers.add(initializer.create(SENSOR_PROFILE_ID));
		} else {
			enablers.addAll(initializer.create(LIGHT_PROFILE_ID, LwM2mId.FIRMWARE));
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
	}
}
