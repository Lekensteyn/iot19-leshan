package group19;

import static org.eclipse.leshan.LwM2mId.FIRMWARE;

import java.util.List;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;

public class Client {

	public final static int LIGHT_PROFILE_ID = 10250;
	public final static int SENSOR_PROFILE_ID = 10350;

	public static void main(String[] args) {
		createClient("light");
	}

	public static void createClient(String endpoint) {

		// register Objects by their ID
		ObjectsInitializer initializer = new ObjectsInitializer();
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
