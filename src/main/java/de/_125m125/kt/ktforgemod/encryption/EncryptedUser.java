package de._125m125.kt.ktforgemod.encryption;

public class EncryptedUser {
	final String uid;
	final String tid;
	final EncryptedData tkn;

	public EncryptedUser(final String uid, final String tid, final EncryptedData tkn) {
		super();
		this.uid = uid;
		this.tid = tid;
		this.tkn = tkn;
	}

	public String getUid() {
		return uid;
	}

	public String getTid() {
		return tid;
	}

	public EncryptedData getTkn() {
		return tkn;
	}
}
