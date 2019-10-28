package de._125m125.kt.ktforgemod.encryption;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
	private final SecureRandom SECURE_RANDOM = new SecureRandom();

	public EncryptedData aesEncrypt(final String s, final String password) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			final byte[] iv = new byte[cipher.getBlockSize()];
			final byte[] salt = new byte[8];

			SECURE_RANDOM.nextBytes(iv);
			SECURE_RANDOM.nextBytes(salt);
			cipher.init(Cipher.ENCRYPT_MODE, generateKey(password.toCharArray(), salt), new IvParameterSpec(iv));

			final byte[] encrypted = cipher.doFinal(s.getBytes("UTF-8"));
			return new EncryptedData(encrypted, iv, salt);
		} catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IOException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private Key generateKey(final char[] password, final byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		final KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
		final SecretKey tmp = factory.generateSecret(spec);
		return new SecretKeySpec(tmp.getEncoded(), "AES");
//		return new PBEKeySpec(password, salt, 65536, 256);
	}

	public String aesDecrypt(final EncryptedData encryptionResult, final String password) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

			cipher.init(Cipher.DECRYPT_MODE, generateKey(password.toCharArray(), encryptionResult.getSaltBytes()),
					new IvParameterSpec(encryptionResult.getIvBytes()));

			final byte[] decrypted = cipher.doFinal(encryptionResult.getCyphertextBytes());
			return new String(decrypted, "UTF-8");
		} catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IOException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
