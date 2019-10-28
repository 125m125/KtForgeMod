package de._125m125.kt.ktforgemod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLHandshakeException;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.lwjgl.input.Keyboard;

import com.google.common.eventbus.EventBus;

import de._125m125.kt.ktapi.core.KtCachingRequester;
import de._125m125.kt.ktapi.core.KtNotificationManager;
import de._125m125.kt.ktapi.core.SingleUserKtRequester;
import de._125m125.kt.ktapi.core.entities.Permissions;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.users.CertificateUser;
import de._125m125.kt.ktapi.core.users.KtUserStore;
import de._125m125.kt.ktapi.core.users.TokenUser;
import de._125m125.kt.ktapi.core.users.UserKey;
import de._125m125.kt.ktapi.requester.retrofit.KtRetrofitRequester;
import de._125m125.kt.ktapi.retrofit.KtRetrofit;
import de._125m125.kt.ktapi.smartcache.KtSmartCache;
import de._125m125.kt.ktapi.websocket.KtWebsocketManager;
import de._125m125.kt.ktapi.websocket.events.listeners.AutoReconnectionHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.KtWebsocketNotificationHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.OfflineMessageHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.SessionHandler;
import de._125m125.kt.ktapi.websocket.okhttp.KtOkHttpWebsocket;
import de._125m125.kt.ktforgemod.adminTools.ChestManager;
import de._125m125.kt.ktforgemod.encryption.EncryptedUser;
import de._125m125.kt.ktforgemod.encryption.EncryptionHelper;
import de._125m125.kt.ktforgemod.gui.KtLoginScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = KadcontradeMod.MODID, name = KadcontradeMod.NAME, version = KadcontradeMod.VERSION)
@SideOnly(Side.CLIENT)
public class KadcontradeMod {
	public static final String MODID = "ktforgemod";
	public static final String NAME = "Kadcontrade Mod";
	public static final String VERSION = "1.0";

	private static Logger logger;

	private EncryptedUser user;

	private String tkn;
	private LoginState state;

	private final KtUserStore userStore = new KtUserStore();
	private KtRetrofitRequester requester;
	private KtNotificationManager<?> notificationManager;
	private KtCachingRequester cachingRequester;
	private UserKey userKey;
	private SingleUserKtRequester suRequester;
	private Permissions permissions;

	private final EncryptionHelper encryptionHelper = new EncryptionHelper();

	public final EventBus eventBus = new EventBus("Kadcontrade");
	private File configPath;

	private static KeyBinding userKeyBinding = new KeyBinding("key.hud.user", Keyboard.KEY_K,
			"key.kadcontrade.category");
	private static KeyBinding adminKeyBinding = new KeyBinding("key.hud.admin", Keyboard.KEY_J,
			"key.kadcontrade.category");
	private ChestManager chestWatcher;

	@EventHandler
	public void preInit(final FMLPreInitializationEvent event) {
		logger = event.getModLog();
		configPath = new File("config");
	}

	@EventHandler
	public void init(final FMLInitializationEvent event) {
		// some example code
		MinecraftForge.EVENT_BUS.register(this);
		chestWatcher = new ChestManager(this);
		MinecraftForge.EVENT_BUS.register(chestWatcher);
		ClientRegistry.registerKeyBinding(userKeyBinding);
	}

	@SubscribeEvent
	public void onEvent(final KeyInputEvent event) {
		// DEBUG
//		System.out.println("Key Input Event");

		// check each enumerated key binding type for pressed and take appropriate
		// action
		if (userKeyBinding.isPressed()) {
			chestWatcher.toggleEnable();
			// DEBUG
//			System.out.println("Key binding =" + userKeyBinding.getKeyDescription());

			// do stuff for this key binding here
			// remember you may need to send packet to server
//			if (getLoginState() != LoginState.SUCCESS) {
//				Minecraft.getMinecraft().displayGuiScreen(new KtLoginScreen(this));
//			} else {
////				minecraft.displayGuiScreen(new KtOverviewScreen(this));
//			}

		}
		if (adminKeyBinding.isPressed()) {
			if (getLoginState() != LoginState.SUCCESS) {
				Minecraft.getMinecraft().displayGuiScreen(new KtLoginScreen(this));
			} else {
//				minecraft.displayGuiScreen(new KtAdminScreen(this));
			}
		}
	}

	public void usePassword(final String password) {
		try {
			final String tkn = encryptionHelper.aesDecrypt(user.getTkn(), password);
			if (tkn == null) {
				throw new IllegalArgumentException("wrong password");
			}
			setUser(new TokenUser(user.getUid(), user.getTid(), tkn), password);
		} catch (final RuntimeException e) {
			if (e.getCause() instanceof InvalidKeyException && "Illegal key size".equals(e.getCause().getMessage())) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
						"Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."));
				setLoginState(LoginState.ILLEGAL_JRE);
				e.printStackTrace();
			}
		}
	}

	public void loadP12(final char[] password) throws IOException, GeneralSecurityException {
		final File certificateFile = getCertificateCandidates()[0];
		try (InputStream keyInput = new FileInputStream(certificateFile)) {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keyInput, password);
			final Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				final Certificate certificate = keyStore.getCertificate(aliases.nextElement());
				if (certificate == null || !(certificate instanceof X509Certificate)) {
					continue;
				}
				final JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(
						(X509Certificate) certificate);
				final RDN[] rdn = jcaX509CertificateHolder.getSubject().getRDNs(BCStyle.UID);
				if (rdn.length != 1 || rdn[0].isMultiValued()) {
					continue;
				}
				final String uid = IETFUtils.valueToString(rdn[0].getFirst().getValue());
				setUser(new CertificateUser(uid, certificateFile, password));
				return;
			}
			throw new IOException("no certificate found");
		}
	}

	private File[] getCertificateCandidates() {
		// check if we can use bouncycastle...
		try {
			Class.forName("org.bouncycastle.cert.jcajce.JcaX509CertificateHolder");
		} catch (final ClassNotFoundException ex) {
			return new File[0];
		}
		return configPath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return "certificate.p12".equals(name);
			}
		});
	}

	private void setUser(final CertificateUser user) {
		this.user = new EncryptedUser(user.getUserId(), null, null);
//		LiteLoader.getInstance().writeConfig(this);
		closeRequesters();

		userStore.add(user);
		userKey = user.getKey();
		requester = KtRetrofit.createClientCertificateRequester("miecraftmod", userStore, user.getKey(), null);
		buildDecoratingRequesters(userKey, requester);
		checkPermissions();
	}

	public void setUser(final TokenUser user, final String password) {
		this.tkn = user.getToken();
		try {
			this.user = new EncryptedUser(user.getUserId(), user.getTokenId(),
					encryptionHelper.aesEncrypt(tkn, password));
			ConfigHandler.writeUser(this.user);
		} catch (final RuntimeException e) {
			if (e.getCause() instanceof InvalidKeyException && "Illegal key size".equals(e.getCause().getMessage())) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
						"Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."));
				setLoginState(LoginState.ILLEGAL_JRE);
				e.printStackTrace();
			}
			e.printStackTrace();
		}
		closeRequesters();

		userStore.add(user);
		userKey = user.getKey();
		requester = KtRetrofit.createDefaultRequester("miecraftmod", userStore, null);
		buildDecoratingRequesters(userKey, requester);
		checkPermissions();
	}

	private void buildDecoratingRequesters(final UserKey key, final KtRetrofitRequester requester) {
		final KtOkHttpWebsocket ws = new KtOkHttpWebsocket("wss://kt.125m125.de/api/websocket",
				requester.getOkHttpClient());
		notificationManager = new KtWebsocketNotificationHandler(userStore);
		KtWebsocketManager.builder(ws).addDefaultParsers().addListener(new OfflineMessageHandler())
				.addListener(new SessionHandler()).addListener(notificationManager)
				.addListener(new AutoReconnectionHandler()).buildAndOpen();
		cachingRequester = new KtSmartCache(requester, notificationManager);
		suRequester = new SingleUserKtRequester(key, requester); // TODO cachingRequester
	}

	private void checkPermissions() {
		suRequester.getPermissions().addCallback(new Callback<Permissions>() {

			@Override
			public void onSuccess(final int status, final Permissions result) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Kadcontrade verbunden."));
				permissions = result;
				setLoginState(LoginState.SUCCESS);
			}

			@Override
			public void onFailure(final int status, final String message, final String humanReadableMessage) {
				if (status == -1) {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
							"Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."));
					setLoginState(LoginState.ILLEGAL_JRE);
				} else {
					Minecraft.getMinecraft().player.sendMessage(
							new TextComponentString("Kadcontrade konnte keine gültigen Logindaten finden."));
					setLoginState(LoginState.FAILURE);
				}
			}

			@Override
			public void onError(final Throwable t) {
				if (t instanceof SSLHandshakeException) {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
							"Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."));
					setLoginState(LoginState.ILLEGAL_JRE);
					t.printStackTrace();
				} else {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
							"Bei der Verbindung mit Kadcontrade ist ein unbekannter Fehler aufgetreten."));
					setLoginState(LoginState.FAILURE);
					t.printStackTrace();
				}
			}
		});
	}

	private void closeRequesters() {
		if (suRequester != null)
			try {
				suRequester.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		if (cachingRequester != null)
			try {
				cachingRequester.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		if (notificationManager != null)
			notificationManager.disconnect();
		if (requester != null)
			requester.close();
	}

	public UserKey getUser() {
		return userKey;
	}

	public SingleUserKtRequester getRequester() {
		return suRequester;
	}

	public Permissions getCurrentPermissions() {
		if (state == LoginState.SUCCESS)
			return permissions;
		else
			return Permissions.NO_PERMISSIONS;
	}

	public LoginState getLoginState() {
		return state;
	}

	public void setLoginState(final LoginState state) {
		this.state = state;
		eventBus.post(state);
		if (state == LoginState.SUCCESS) {
			if (permissions.mayReadItems())
				notificationManager.subscribeToItems(
						m -> Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Dein Itemstand hat sich verändert.")),
						userKey, false);
			if (permissions.mayReadPayouts())
				notificationManager.subscribeToPayouts(
						m -> Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Eine Auszahlungen wurde bearbeitet.")),
						userKey, false);
			if (permissions.mayReadOrders())
				notificationManager.subscribeToTrades(
						m -> Minecraft.getMinecraft().player.sendMessage(
								new TextComponentString("Für eine Order wurde ein Gegenangebot gefunden.")),
						userKey, false);
		}
	}

	public KtNotificationManager<?> getNotificationManager() {
		return notificationManager;
	}

	public boolean hasEncryptedTkn() {
		user = ConfigHandler.getEncryptedUser();
		return user != null && user.getTkn() != null;
	}

	public boolean p12Present() {
		return getCertificateCandidates().length > 0;
	}

}
