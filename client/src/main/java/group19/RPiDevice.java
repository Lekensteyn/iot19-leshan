package group19;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RPiDevice {

	private static InputStream loadResource(String resourceName) {
		InputStream input = RPiSensorDevice.class.getResourceAsStream(resourceName);
		if (input == null) {
			resourceName = "/resources" + resourceName;
			input = RPiSensorDevice.class.getResourceAsStream(resourceName);
		}
		return input;
	}

	/**
	 * Reads a Python script and writes it to the standard input pipe of a
	 * Python interpreter process.
	 * 
	 * @param os
	 *            Output stream that refers to stdin of the Python process.
	 * @throws IOException
	 */
	protected void readAndWriteScript(OutputStream os, String filename) throws IOException {
		InputStream input = loadResource(filename);
		if (input == null) {
			throw new IOException("Cannot load resource " + filename);
		}
		byte[] buffer = new byte[4096];
		int n;
		while ((n = input.read(buffer)) > 0) {
			os.write(buffer, 0, n);
		}
		os.flush();
		os.close();
	}

}
