package group19;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDiscovery {

	private final static Logger LOG = LoggerFactory.getLogger(ServiceDiscovery.class);

	public static List<String> findAddresses() {
		List<String> addresses = new ArrayList<>();
		String command = "avahi-browse -ptr _coap._udp";
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				LOG.debug("avahi-browse: " + line.trim());
				String[] parts = line.split(";");
				if (parts.length <= 8) {
					// Skip unknown lines
					continue;
				}
				if (parts[0] != "=") {
					// Only process the "resolved addresses" part
					continue;
				}

				String address = parts[7];
				addresses.add(address);
			}
		} catch (IOException e) {
			LOG.error("Failed to execute discovery", e);
		} finally {
			if (proc != null) {
				proc.destroy();
			}
		}
		return addresses;
	}
}
