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
import org.eclipse.leshan.LwM2mId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

	private static final Logger LOG = LoggerFactory.getLogger(Client.class);

	public final static int LIGHT_PROFILE_ID = 10250;
	public final static int SENSOR_PROFILE_ID = 10350;

	public static void main(String[] args) {
		String serverURI = "coap://localhost:5683";
		createClient("light", serverURI);
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

	public static void createClient(String endpoint, String serverURI) {
		// Load LWM2M specs (include default OMA objects for Firmware profile)
		List<ObjectModel> models = new ArrayList<>();
		loadSpec(models, "/oma-objects-spec.json");
		loadSpec(models, "/light-object-spec.json");
		LwM2mModel model = new LwM2mModel(models);
		ObjectsInitializer initializer = new ObjectsInitializer(model);

		// Register mandatory objects (See Appendix E.1, E.2, E.4 of OMA TS)
		initializer.setInstancesForObject(LwM2mId.SECURITY, noSec(serverURI, 123));
		initializer.setInstancesForObject(LwM2mId.SERVER, new Server(123, 30, BindingMode.U, false));
		// Note: Device is mandatory (including things like reboot), but this is
		// not fully implemented by Leshan.
		initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Group 19", "RPi", "12345", "U"));

		// register other Objects by their ID
		initializer.setClassForObject(LIGHT_PROFILE_ID, LightDevice.class);

		// creates the Object Instances
		List<LwM2mObjectEnabler> enablers = initializer.create(LwM2mId.SECURITY, LwM2mId.SERVER, LwM2mId.DEVICE,
				LIGHT_PROFILE_ID, LwM2mId.FIRMWARE);

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
