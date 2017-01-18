package group19;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Communicates with a child process to handle distributed light behavior.
 * 
 * See {@code smartlight.py} for the interface documentation.
 */
public class SmartLight extends RPiDevice {

	private final static Logger LOG = LoggerFactory.getLogger(SmartLight.class);

	private Process proc;
	private InputStream childStdout;
	private OutputStream childStdin;
	private final Path tempDirectory;
	private final File pyFile;
	private SmartLightEventListener listener;

	/**
	 * Default adaptive light behavior implementation.
	 * 
	 * @throws IOException
	 */
	public SmartLight() throws IOException {
		try {
			tempDirectory = Files.createTempDirectory("iotLightBehavior");
			pyFile = new File(tempDirectory.toFile(), "smartlight.py");
			try (OutputStream pyFileOut = new FileOutputStream(pyFile)) {
				readAndWriteScript(pyFileOut, "/smartlight.py");
			}
			pyFile.setExecutable(true);
		} catch (IOException e) {
			deleteStuff();
			throw e;
		}

		// Cleanup temporary files on exit
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				deleteStuff();
			}
		});
	}

	public void setListener(SmartLightEventListener listener) {
		this.listener = listener;
	}

	/**
	 * Tries to start the child process.
	 * 
	 * @throws IOException
	 *             If an error occurred during startup.
	 */
	public void start() throws IOException {
		try {
			proc = Runtime.getRuntime().exec(pyFile.getAbsolutePath());
			forwardErrors(proc.getErrorStream(), LOG);
			childStdout = proc.getInputStream();
			childStdin = proc.getOutputStream();
			watchStdout();
		} catch (IOException e) {
			if (proc != null) {
				proc.destroy();
			}
			deleteStuff();
			throw e;
		}
	}

	private void deleteStuff() {
		pyFile.delete();
		tempDirectory.toFile().delete();
	}

	public void destroy() {
		proc.destroy();
		deleteStuff();
	}

	private void writeChild(String line) {
		if (line.contains("\n")) {
			// should not happen, but if untrusted input is passed...
			throw new IllegalArgumentException("Malformed input");
		}
		if (childStdin != null) {
			byte[] data = (line + "\n").getBytes();
			try {
				LOG.debug("Writing line: " + line);
				childStdin.write(data);
				childStdin.flush();
			} catch (IOException e) {
				LOG.warn("Unable to write light command to child process", e);
			}
		} else {
			LOG.warn("Unable to write light command, child is unavailable.");
		}
	}

	private void watchStdout() {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(childStdout));
		new Thread(new Runnable() {

			@Override
			public void run() {
				String line;
				try {
					while ((line = reader.readLine()) != null) {
						String[] args = line.split(" ", 3);
						if (args.length != 3 || !args[0].equals("set")) {
							LOG.warn("Unexpected input from child: " + line);
							continue;
						}
						if (listener != null) {
							try {
								listener.childValueEmitted(args[1], args[2].trim());
							} catch (IllegalArgumentException e) {
								LOG.warn("Invalid option received from child", e);
							}
						}
					}
				} catch (IOException e) {
					LOG.warn("Failed to read from child stdout", e);
				}
			}
		}).start();
	}

	public void setLocation(double x, double y) {
		writeChild(String.format(Locale.ENGLISH, "location %f %f", x, y));
	}

	public void setOwnership(String ownershipJson) {
		writeChild("ownership " + ownershipJson.replace("\r", "").replace("\n", ""));
	}

	public void notifySensorOccupied(String sensor_id, boolean is_occupied) {
		if (sensor_id.contains(" ")) {
			throw new IllegalArgumentException("Bad Sensor ID");
		}
		writeChild(String.format("sensor_occupied %s %s", sensor_id, is_occupied));
	}

	public void setUser3(String user_id) {
		writeChild("user3 " + user_id);
	}
}
