package de._125m125.kt.ktforgemod.encryption;

import java.io.Serializable;
import java.util.Base64;

public class EncryptedData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6855960888013873299L;

	private final String cyphertext;
	private final String iv;
	private final String salt;

	public EncryptedData(final byte[] cyphertext, final byte[] iv, final byte[] salt) {
		super();
		// we need strings for minecraft to write this data into the config file
		this.cyphertext = Base64.getEncoder().encodeToString(cyphertext);
		this.iv = Base64.getEncoder().encodeToString(iv);
		this.salt = Base64.getEncoder().encodeToString(salt);
	}

	public EncryptedData(final String cyphertext, final String iv, final String salt) {
		super();
		this.cyphertext = cyphertext;
		this.iv = iv;
		this.salt = salt;
	}

	public byte[] getCyphertextBytes() {
		return Base64.getDecoder().decode(this.cyphertext);
	}

	public byte[] getIvBytes() {
		return Base64.getDecoder().decode(this.iv);
	}

	public byte[] getSaltBytes() {
		return Base64.getDecoder().decode(this.salt);
	}

	public String getCyphertext() {
		return cyphertext;
	}

	public String getIv() {
		return iv;
	}

	public String getSalt() {
		return salt;
	}
}