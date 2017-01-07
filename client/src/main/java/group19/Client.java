package group19;

import static org.eclipse.leshan.LwM2mId.FIRMWARE;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

	private static final Logger LOG = LoggerFactory.getLogger(Client.class);

	public final static int LIGHT_PROFILE_ID = 10250;
	public final static int SENSOR_PROFILE_ID = 10350;

	public static void main(String[] args) {
		createClient("light");
	}

	private static void loadSpec(List<ObjectModel> models, String resourceName) {
		InputStream input = ObjectLoader.class.getResourceAsStream(resourceName);
		if (input != null) {
			models.addAll(ObjectLoader.loadJsonStream(input));
		} else {
			LOG.error("Unable to load resource: " + resourceName);
		}
	}

	public static void createClient(String endpoint) {
		// Load LWM2M specifications (include default OMA classes for for Firmware profile)
		List<ObjectModel> models = new ArrayList<>();
		loadSpec(models, "/resources/oma-objects-spec.json");
		loadSpec(models, "/resources/light-object-spec.json");
		LwM2mModel model = new LwM2mModel(models);
		ObjectsInitializer initializer = new ObjectsInitializer(model);

		// register Objects by their ID
		initializer.setClassForObject(LIGHT_PROFILE_ID, LightDevice.class);

		// creates the Object Instances
		List<LwM2mObjectEnabler> enablers = initializer.create(LIGHT_PROFILE_ID, FIRMWARE);

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
