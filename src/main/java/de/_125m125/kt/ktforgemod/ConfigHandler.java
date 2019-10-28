package de._125m125.kt.ktforgemod;

import java.io.File;

import de._125m125.kt.ktforgemod.encryption.EncryptedData;
import de._125m125.kt.ktforgemod.encryption.EncryptedUser;
import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {
	public static Configuration config;
	private static String file = "config/ktforgemod.cfg";

	public synchronized static void load() {
		if (config == null) {
			config = new Configuration(new File(file));
			config.load();
		}
	}

	public synchronized static void save() {
		System.out.println("want to save config... " + config);
		if (config != null) {
			config.save();
			System.out.println("saved config");
		}
	}

	public synchronized static void writeUser(final String uid, final String tid, final EncryptedData encrTkn) {
		load();
		System.out.println("sdfhklö adfgadgjklsdjkljkldgjkdjkltfgdgjopjösrgjklööäösdgöäöädföäföä");
		System.out.println(config.getCategory("user"));
		System.out.println(config.getCategory("user").get("uid"));
		config.get("user", "uid", "").set(uid);
		config.get("user", "tid", "").set(tid);
		config.get("user", "tkn", new String[0])
				.set(new String[] { encrTkn.getCyphertext(), encrTkn.getIv(), encrTkn.getSalt() });
		save();
	}

	public synchronized static EncryptedUser getEncryptedUser() {
		load();
		if (!config.getCategory("user").containsKey("uid"))
			return null;
		final String uid = config.getCategory("user").get("uid").getString();
		final String tid = config.get("user", "tid", "").getString();
		final String[] tkn = config.getCategory("user").get("tkn").getStringList();
		return new EncryptedUser(uid, tid, tkn == null ? null : new EncryptedData(tkn[0], tkn[1], tkn[2]));
	}

	public static void writeUser(final EncryptedUser user) {
		writeUser(user.getUid(), user.getTid(), user.getTkn());
	}
}