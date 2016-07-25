package org.sdsai.jaxos.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
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

import org.apache.commons.configuration.Configuration;
import org.sdsai.jaxos.net.BaseMessage;

public class CipherUtil {
	final String cipherName;
	final String secret;

	public CipherUtil(final Configuration configuration) {
		this.cipherName = configuration.getString("jaxos.cipher", "AES/CBC/PKCS5Padding");
		this.secret = configuration.getString("jaxos.secret", "Don't tell.");
	}

	public ByteBuffer decrypt(final ByteBuffer buffer) throws IOException
	{
		try {
			final Cipher cipher = buildCipher(Cipher.DECRYPT_MODE);
			return ByteBuffer.wrap(cipher.doFinal(buffer.array(), 0, buffer.position()));
		}
		catch (final Exception e){
			throw new IOException("Cannot decrypt message.", e);
		}
	}

	public ByteBuffer encrypt(final ByteBuffer buffer) throws IOException
	{
		try {
			final Cipher cipher = buildCipher(Cipher.ENCRYPT_MODE);
			return ByteBuffer.wrap(cipher.doFinal(buffer.array(), 0, buffer.position()));
		}
		catch (final Exception e){
			throw new IOException("Cannot decrypt message.", e);
		}
	}

	public byte[] decrypt(final byte[] data) throws
	NoSuchPaddingException,
	NoSuchAlgorithmException,
	InvalidKeySpecException,
	InvalidKeyException,
	BadPaddingException,
	IllegalBlockSizeException,
	InvalidParameterSpecException,
	InvalidAlgorithmParameterException
	{
		final Cipher cipher = buildCipher(Cipher.DECRYPT_MODE);
		return cipher.doFinal(data);
	}

	public byte[] encrypt(final byte[] data) throws
	NoSuchPaddingException,
	NoSuchAlgorithmException,
	InvalidKeySpecException,
	InvalidKeyException,
	BadPaddingException,
	IllegalBlockSizeException,
	InvalidParameterSpecException,
	InvalidAlgorithmParameterException
	{
		final Cipher cipher = buildCipher(Cipher.ENCRYPT_MODE);
		return cipher.doFinal(data);
	}

	public Cipher buildCipher(final int mode) throws
	NoSuchPaddingException,
	NoSuchAlgorithmException,
	InvalidKeySpecException,
	InvalidKeyException,
	InvalidParameterSpecException,
	InvalidAlgorithmParameterException
	{
		final Cipher cipher = Cipher.getInstance(cipherName);
		final int length = 128; //Cipher.getMaxAllowedKeyLength(cipherName);

		final Key key = generateKey(secret, length);

		final AlgorithmParameters params = cipher.getParameters();

		final byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
		for (int i = 0; i < iv.length; ++i) {
			iv[i] = (byte)i;
		}

		cipher.init(mode, key, new IvParameterSpec(iv));

		return cipher;
	}

	private Key generateKey(final String phrase, final int length) 
			throws
			NoSuchPaddingException,
			NoSuchAlgorithmException,
			InvalidKeySpecException,
			InvalidKeyException
	{
		// Generate a key.
		final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		final KeySpec keySpec = new PBEKeySpec(secret.toCharArray(), "just some salt".getBytes(), 65535, length);
		final SecretKey tmpKey = secretKeyFactory.generateSecret(keySpec);
		final SecretKey aesKey = new SecretKeySpec(tmpKey.getEncoded(), "AES");

		return aesKey;
	}


}
