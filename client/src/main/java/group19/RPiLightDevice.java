package group19;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes light change requests to a child process.
 * 
 * The child process is a Python script named {@code light.py} which must be an
 * executable file located in the current working directory. It should read a
 * command line by line. Each line contains a command followed by a space and
 * its arguments. Examples:
 * <ul>
 * <li>Set the color to yellow (RGB #ffff00): {@code color 255 255 0}
 * <li>Enable low light mode: {@code lowlight true}
 * <li>Disable low light mode: {@code lowlight false}
 * </ul>
 */
public class RPiLightDevice implements LightProvider {

	private final static Logger LOG = LoggerFactory.getLogger(RPiLightDevice.class);

	private Process proc;
	private OutputStream childOutputStream;

	/**
	 * Tries to start the child process.
	 * 
	 * @throws IOException
	 *             If an error occurred during startup.
	 */
	public void start() throws IOException {
		try {
			proc = Runtime.getRuntime().exec("./light.py");
			childOutputStream = proc.getOutputStream();
		} catch (IOException e) {
			if (proc != null) {
				proc.destroy();
			}
			throw e;
		}
	}

	public void destroy() {
		proc.destroy();
	}

	@Override
	public void setColor(int r, int g, int b) {
		writeChild(String.format("color %d %d %d", r, g, b));
	}

	@Override
	public void setLowLightMode(boolean isLowLight) {
		writeChild("lowlight " + isLowLight);
	}

	private void writeChild(String line) {
		if (childOutputStream != null) {
			byte[] data = (line + "\n").getBytes();
			try {
				childOutputStream.write(data);
			} catch (IOException e) {
				LOG.warn("Unable to write light command to child process", e);
			}
		} else {
			LOG.warn("Unable to write light command, child is unavailable.");
		}
	}
}
