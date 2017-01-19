package group19;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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

	private static byte[] readFile(String inputFile) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (FileInputStream is = new FileInputStream(inputFile)) {
			byte[] buffer = new byte[4096];
			int n;
			while ((n = is.read(buffer)) != -1) {
				bos.write(buffer, 0, n);
			}
		}
		return bos.toByteArray();
	}

	private static void printUsage(String errorMessage) {
		String usage = String.format("Usage: java -cp client.jar %s {genkey|sign|verify} [OPTIONS]",
				SignedData.class.getCanonicalName());
		if (errorMessage != null) {
			System.err.println(errorMessage);
			System.err.println(usage);
			System.exit(1);
		} else {
			System.out.println(usage);
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			printUsage("Missing command");
		}

		boolean success = false;
		switch (args[0]) {
		case "genkey":
			// "genkey": writes pubkey and privkey to stdout
			if (args.length != 3) {
				printUsage("Options for genkey: privKeyOutFile pubKeyOutFile");
			}
			String privKeyOutFile = args[1];
			String pubKeyOutFile = args[2];
			success = doGenKey(privKeyOutFile, pubKeyOutFile);
			break;
		case "sign":
			if (args.length != 4) {
				printUsage("Options for sign: privKeyFile inputFile outputFile");
			}
			String privKeyFile = args[1];
			String inputFile = args[2];
			String outputFile = args[3];
			success = doSign(privKeyFile, inputFile, outputFile);
			break;
		case "verify":
			if (args.length != 3) {
				printUsage("Options for verify: pubKeyFile inputFile");
			}
			String pubKeyFile = args[1];
			inputFile = args[2];
			success = doVerify(pubKeyFile, inputFile);
			break;
		case "help":
			printUsage(null);
			break;
		default:
			printUsage("Unrecognized command: " + args[0]);
		}
		System.exit(success ? 0 : 1);
	}

	private static boolean doGenKey(String privKeyOutFile, String pubKeyOutFile) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
			// Default is (SECG) "secp256r1" a.k.a "NIST P-256"/"prime256v1".
			// (Ed25519 is unfortunately not available.)
			kpg.initialize(new ECGenParameterSpec("secp256r1"));

			KeyPair kp = kpg.generateKeyPair();
			PrivateKey privKey = kp.getPrivate();
			PublicKey pubKey = kp.getPublic();
			String privKeyB64 = DatatypeConverter.printBase64Binary(privKey.getEncoded());
			String pubKeyB64 = DatatypeConverter.printBase64Binary(pubKey.getEncoded());

			try (PrintStream privOut = new PrintStream(privKeyOutFile)) {
				privOut.println(privKeyB64);
				System.out.println("Wrote private key to " + privKeyOutFile);
			} catch (FileNotFoundException e) {
				System.out.println("Unable to write private key: " + e);
				return false;
			}
			try (PrintStream pubOut = new PrintStream(pubKeyOutFile)) {
				pubOut.println(pubKeyB64);
				System.out.println("Wrote public key to " + pubKeyOutFile);
			} catch (FileNotFoundException e) {
				System.out.println("Unable to write public key: " + e);
				return false;
			}
			return true;
		} catch (GeneralSecurityException e) {
			System.err.println("Key generation failed: " + e);
			return false;
		}
	}

	private static boolean doSign(String privKeyFile, String inputFile, String outputFile) {
		PrivateKey privKey;
		try {
			byte[] privKeyData = loadBase64FromStream(new FileInputStream(privKeyFile));
			privKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privKeyData));
		} catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			System.err.println("Unable to load private key: " + e);
			return false;
		}

		byte[] inputData;
		try {
			inputData = readFile(inputFile);
		} catch (IOException e) {
			System.err.println("Unable to read input file: " + e);
			return false;
		}

		SignedData signedData = new SignedData(inputData);
		try {
			signedData.sign(privKey);
		} catch (SignatureException e) {
			System.err.println("Could not sign file: " + e);
			return false;
		}
		String signatureB64 = DatatypeConverter.printBase64Binary(signedData.getSignature());

		try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
			outputStream.write(inputData);
			outputStream.write('#');
			outputStream.write(signatureB64.getBytes());
			outputStream.write('\n');
		} catch (IOException e) {
			System.err.println("Could not save signed file: " + e);
			return false;
		}
		System.out.println("Wrote signed file to " + outputFile);
		return true;
	}

	private static boolean doVerify(String pubKeyFile, String inputFile) {
		PublicKey pubKey;
		try {
			byte[] pubKeyData = loadBase64FromStream(new FileInputStream(pubKeyFile));
			pubKey = loadPublicKey(pubKeyData);
		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Unable to load public key: " + e);
			return false;
		}

		byte[] inputData;
		try {
			inputData = readFile(inputFile);
		} catch (IOException e) {
			System.err.println("Unable to read input file: " + e);
			return false;
		}

		try {
			SignedData signedData = SignedData.load(inputData);
			signedData.verify(pubKey);
		} catch (SignatureException e) {
			System.err.println(e);
			return false;
		}

		System.out.println("Verified signature of " + inputFile);
		return true;
	}
}
