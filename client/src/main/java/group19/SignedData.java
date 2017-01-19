package group19;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

/**
 * Handles cryptographically signed data. The signed blob contains the actual
 * data followed by the signature. This signature consists of a {@code #}
 * followed by the base64 encoding of the DER-encoded signature and a newline.
 */
public class SignedData {
	private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
	private final byte[] data;
	private byte[] signature;
	private PublicKey defaultPublicKey;

	/**
	 * Creates signed data.
	 */
	public SignedData(byte[] data, byte[] signature) {
		this.data = data;
		this.signature = signature;
	}

	/**
	 * Creates unsigned data (to be signed later).
	 */
	public SignedData(byte[] data) {
		this.data = data;
	}

	/**
	 * Tries to load a blob of signed data. The caller is responsible for
	 * checking the signature.
	 * 
	 * @param blob
	 *            A byte array containing the data and its signature.
	 * @return A SignedData object.
	 * @throws SignatureException
	 *             If the signature cannot be found.
	 */
	public static SignedData load(byte[] blob) throws SignatureException {
		int dataEnd = -1;

		// find '#', that marks the length of the data.
		for (int i = blob.length - 1; i >= 0; i--) {
			if (blob[i] == '#') {
				dataEnd = i;
				break;
			}
		}

		if (dataEnd == -1) {
			throw new SignatureException("Signature not found");
		}

		// signature comes after '#'
		int sigPos = dataEnd + 1;
		String sigBase64 = new String(blob, sigPos, blob.length - sigPos).trim();
		byte[] data = Arrays.copyOf(blob, dataEnd);
		byte[] signature = DatatypeConverter.parseBase64Binary(sigBase64);
		SignedData signedData = new SignedData(data, signature);
		return signedData;
	}

	/**
	 * Check whether the signed data is indeed valid.
	 * 
	 * @throws SignatureException
	 *             If the signature is invalid.
	 */
	public void verify(PublicKey pubKey) throws SignatureException {
		Signature sig;
		try {
			sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			sig.initVerify(pubKey);
			sig.update(data);
			if (!sig.verify(signature)) {
				throw new SignatureException("Signature verification failed");
			}
		} catch (SignatureException e) {
			throw e;
		} catch (NoSuchAlgorithmException | KeyException e) {
			throw new SignatureException(e);
		}
	}

	/**
	 * Creates a signature for the stored data.
	 * 
	 * @throws SignatureException
	 *             If the signature could not be created.
	 */
	public void sign(PrivateKey privKey) throws SignatureException {
		Signature sig;
		try {
			sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			// WARNING: this assumes that the system random number generator is
			// secure. If not, the private key can be leaked!
			sig.initSign(privKey);
			sig.update(data);
			signature = sig.sign();
		} catch (SignatureException e) {
			throw e;
		} catch (NoSuchAlgorithmException | KeyException e) {
			throw new SignatureException(e);
		}
	}

	/**
	 * Try to get the signed data, first checking if it is valid using the
	 * default public key (stored in {@linkplain /resources/public.key}).
	 * 
	 * @return The data for which a signature is available.
	 * @throws SignatureException
	 *             If the signature cannot be verified.
	 */
	public byte[] getVerifiedData() throws SignatureException {
		if (defaultPublicKey == null) {
			byte[] defaultPublicKeyData;
			InputStream pubKeyFile = loadResource("/public.key");
			if (pubKeyFile == null) {
				throw new SignatureException("Unable to load public key file");
			}

			try {
				defaultPublicKeyData = loadBase64FromStream(pubKeyFile);
				defaultPublicKey = loadPublicKey(defaultPublicKeyData);
			} catch (GeneralSecurityException | IOException e) {
				throw new SignatureException("Unable to load public key", e);
			}
		}
		verify(defaultPublicKey);
		return data;
	}

	private byte[] getSignature() {
		if (signature == null) {
			throw new IllegalStateException("Data is not yet signed!");
		}
		return signature;
	}

	private static InputStream loadResource(String resourceName) {
		InputStream input = SignedData.class.getResourceAsStream(resourceName);
		if (input == null) {
			resourceName = "/resources" + resourceName;
			input = SignedData.class.getResourceAsStream(resourceName);
		}
		return input;
	}

	private static PublicKey loadPublicKey(byte[] pubKeyData) throws GeneralSecurityException {
		try {
			return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pubKeyData));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw e;
		}
	}

	private static byte[] loadBase64FromStream(InputStream keyFile) throws IOException {
		try (Scanner scanner = new Scanner(keyFile)) {
			String keyB64 = scanner.nextLine();
			return DatatypeConverter.parseBase64Binary(keyB64);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid keyfile contents", e);
		}
	}
}
