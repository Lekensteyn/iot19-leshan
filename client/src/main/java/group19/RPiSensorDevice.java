package group19;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watch for sensor changes from a separate child process.
 * 
 * The child process is a Python script named {@code sensor.py} which must be an
 * executable file located in the current working directory. It should write one
 * character for the current sensor state ({@code F} for Free or {@code U} for
 * Used) to standard output.
 */
public class RPiSensorDevice implements Runnable {

	private final static Logger LOG = LoggerFactory.getLogger(RPiSensorDevice.class);

	private Process proc;
	private SensorChangeListener sensorChangeListener;

	private InputStream childInputStream;

	public RPiSensorDevice(SensorChangeListener sensorChangeListener) {
		this.sensorChangeListener = sensorChangeListener;
	}

	/**
	 * Tries to start the child process.
	 * 
	 * @throws IOException
	 *             If an error occurred during startup.
	 */
	public void start() throws IOException {
		try {
			proc = Runtime.getRuntime().exec("./sensor.py");
			childInputStream = proc.getInputStream();

			boolean state = readStateFromChild();
			sensorChangeListener.sensorChanged(state);
		} catch (IOException e) {
			if (proc != null) {
				proc.destroy();
			}
			throw e;
		}
	}

	private boolean readStateFromChild() throws IOException {
		int c;
		while ((c = childInputStream.read()) != -1) {
			switch (c) {
			case 'U': /* in Use */
				return true;
			case 'F': /* Free */
				return false;
			default:
				LOG.warn("Unrecognized input from face detection script: {}", (char) c);
			}
		}
		throw new EOFException("No more input from sensor process");
	}

	@Override
	public void run() {
		try {
			while (true) {
				boolean state = readStateFromChild();
				sensorChangeListener.sensorChanged(state);
			}
		} catch (IOException e) {
			LOG.error("Read error from face detection script", e);
		}
	}

	public void destroy() {
		proc.destroy();
	}
}
