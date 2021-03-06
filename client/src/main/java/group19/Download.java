package group19;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Download the contents of a remote file.
 */
public abstract class Download implements Runnable {
	private URL url;

	public Download(URL url) {
		setUrl(url);
	}

	public Download(String url) {
		try {
			setUrl(new URL(url));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private final void setUrl(URL url) {
		this.url = url;
		if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
			throw new IllegalArgumentException("Expected HTTP URL.");
		}
	}

	private byte[] doDownload() throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		InputStream is = httpConn.getInputStream();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int n;
		while ((n = is.read(buffer)) != -1) {
			os.write(buffer, 0, n);
		}
		return os.toByteArray();
	}

	@Override
	public void run() {
		byte[] data;
		try {
			data = doDownload();
		} catch (IOException e) {
			onError(e);
			return;
		}
		onComplete(data);
	}

	/**
	 * Invoked with the response data when the download completes.
	 */
	public abstract void onComplete(byte[] data);

	/**
	 * Invoked when the request fails.
	 */
	public abstract void onError(IOException e);
}
