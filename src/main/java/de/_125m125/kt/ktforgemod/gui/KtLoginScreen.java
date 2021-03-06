package de._125m125.kt.ktforgemod.gui;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import de._125m125.kt.ktapi.core.users.TokenUser;
import de._125m125.kt.ktapi.core.users.TokenUserKey;
import de._125m125.kt.ktforgemod.KadcontradeMod;
import de._125m125.kt.ktforgemod.LoginState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class KtLoginScreen extends GuiScreen {
	private static final int NEW_LOGIN = 15;
	private static final int EXISTING_LOGIN = 16;
	private static final int P12_LOGIN = 17;
	private static final int UID_ID = 0;
	private static final int TID_ID = 1;
	private static final int TKN_ID = 2;
	private static final int PWD_ID = 3;
	private static final int P12_PW = 6;
	private static final int PWD_ID_2 = 8;

	private final Map<Integer, GuiLabel> labels = new HashMap<>();
	private final Map<Integer, GuiTextField> fields = new HashMap<>();

	private GuiLabel userStatusLabel;
	private final KadcontradeMod kt;

	public KtLoginScreen(final KadcontradeMod kt) {
		this.kt = kt;
	}

	@Override
	public void initGui() {
		super.initGui();
		kt.eventBus.register(this);
		allowUserInput = true;

		{
			addLabel(new GuiLabel(fontRenderer, 1000003, width / 8, 110, this.width * 3 / 8, 20, 0xFFFFFF),
					"Benutzer ID: ");
			fields.put(UID_ID, new GuiTextField(UID_ID, fontRenderer, this.width / 2, 110, this.width * 3 / 8, 20));
			fields.get(UID_ID).setMaxStringLength(7);
			if (!kt.p12Present() && !kt.hasEncryptedTkn())
				fields.get(UID_ID).setFocused(true);
			if (kt.getUser() != null)
				fields.get(UID_ID).setText(kt.getUser().getUserId());

			addLabel(new GuiLabel(fontRenderer, 1000004, width / 8, 130, this.width * 3 / 8, 20, 0xFFFFFF),
					"Token ID: ");
			fields.put(TID_ID, new GuiTextField(TID_ID, fontRenderer, this.width / 2, 130, this.width * 3 / 8, 20));
			fields.get(TID_ID).setMaxStringLength(7);
			if (kt.getUser() != null)
				fields.get(TID_ID).setText(((TokenUserKey) kt.getUser()).getTid());

			addLabel(new GuiLabel(fontRenderer, 1000005, width / 8, 150, this.width * 3 / 8, 20, 0xFFFFFF), "Token: ");
			fields.put(TKN_ID, new GuiTextField(TKN_ID, fontRenderer, this.width / 2, 150, this.width * 3 / 8, 20));
			fields.get(TKN_ID).setMaxStringLength(13);

			addLabel(new GuiLabel(fontRenderer, 1000006, width / 8, 170, this.width * 3 / 8, 20, 0xFFFFFF),
					"(Optional) KtMinecraftMod Passwort: ");
			fields.put(PWD_ID, new GuiTextField(TKN_ID, fontRenderer, this.width / 2, 170, this.width * 3 / 8, 20));
			fields.get(PWD_ID);

			addButton(new GuiButton(NEW_LOGIN, this.width / 8, 190, this.width * 3 / 4, 20, "Benutzer verwenden"));
		}
		if (kt.hasEncryptedTkn()) {
			addLabel(new GuiLabel(fontRenderer, 1000002, width / 8, 65, this.width * 3 / 8, 20, 0xFFFFFF),
					"KtMinecraftMod Passwort: ");
			fields.put(PWD_ID_2, new GuiTextField(PWD_ID_2, fontRenderer, this.width / 2, 65, this.width * 3 / 8, 20));
			if (!kt.p12Present())
				fields.get(PWD_ID_2).setFocused(true);

			addButton(new GuiButton(EXISTING_LOGIN, this.width / 8, 85, this.width * 3 / 4, 20, "Login"));
		}
		if (kt.p12Present()) {
			addLabel(new GuiLabel(fontRenderer, 1000001, width / 8, 20, this.width * 3 / 8, 20, 0xFFFFFF),
					"p12 Passwort: ");
			fields.put(P12_PW, new GuiTextField(P12_PW, fontRenderer, this.width / 2, 20, this.width * 3 / 8, 20));
			fields.get(P12_PW).setFocused(true);
			addButton(new GuiButton(P12_LOGIN, this.width / 8, 40, this.width * 3 / 4, 20, "Login"));
		}
		onLoginStateChange(kt.getLoginState());
	}

	private GuiLabel addLabel(final GuiLabel label, final String... text) {
		final GuiLabel put = labels.put(label.id, label);
		if (put != null)
			labelList.remove(put);
		labelList.add(label);
		for (final String s : text) {
			label.addLine(s);
		}
		return label;
	}

	private void setGuiLabelText(final String text) {
		if (userStatusLabel != null) {
			labelList.remove(userStatusLabel);
		}
		userStatusLabel = new GuiLabel(fontRenderer, 0, this.width / 2 + this.width / 8, 26, this.width / 4, 20,
				0xFFFFFF);
		userStatusLabel.addLine(text);
		labelList.add(userStatusLabel);
	}

	@Override
	protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
		fields.values().forEach(t -> t.mouseClicked(mouseX, mouseY, mouseButton));
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
		if (keyCode == 15) { // tab
			if (isShiftKeyDown()) {
				fields.values().stream().filter(GuiTextField::isFocused).findAny().ifPresent(f -> {
					final GuiTextField next = fields.get(f.getId() - 1);
					if (next != null) {
						f.setFocused(false);
						next.setFocused(true);
					}
				});
			} else {
				fields.values().stream().filter(GuiTextField::isFocused).findAny().ifPresent(f -> {
					final GuiTextField next = fields.get(f.getId() + 1);
					if (next != null) {
						f.setFocused(false);
						next.setFocused(true);
					}
				});
			}
		} else if (keyCode == 28) {// enter
			fields.values().stream().filter(GuiTextField::isFocused).findAny().ifPresent(f -> {
				switch (f.getId()) {
				case UID_ID:
				case TID_ID:
				case TKN_ID:
				case PWD_ID:
					buttonList.stream().filter(b -> b.id == NEW_LOGIN).findAny().ifPresent(t -> {
						try {
							actionPerformed(t);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					});
					break;
				case P12_PW:
					buttonList.stream().filter(b -> b.id == P12_LOGIN).findAny().ifPresent(t -> {
						try {
							actionPerformed(t);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					});
					break;
				case PWD_ID_2:
					buttonList.stream().filter(b -> b.id == EXISTING_LOGIN).findAny().ifPresent(t -> {
						try {
							actionPerformed(t);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					});
					break;
				}
			});
		} else {
			fields.values().stream().filter(GuiTextField::isFocused)
					.forEach(t -> t.textboxKeyTyped(typedChar, keyCode));
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		drawDefaultBackground();
		fields.values().forEach(GuiTextField::drawTextBox);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(final GuiButton button) throws IOException {
		if (button.id == P12_LOGIN) {
			try {
				kt.loadP12(fields.get(P12_PW).getText().toCharArray());
			} catch (GeneralSecurityException | IOException e) {
				e.printStackTrace();
				setGuiLabelText("Falsches Passwort oder ungültiges Zertifikat");
			}
		} else if (button.id == EXISTING_LOGIN) {
			try {
				kt.usePassword(fields.get(PWD_ID_2).getText());
			} catch (final IllegalArgumentException e) {
				setGuiLabelText("Falsches Passwort");
			}
		} else if (button.id == NEW_LOGIN) {
			kt.setUser(new TokenUser(fields.get(UID_ID).getText(), fields.get(TID_ID).getText(),
					fields.get(TKN_ID).getText()), fields.get(PWD_ID).getText());
		} else {
			super.actionPerformed(button);
		}
	}

	@Override
	public void onGuiClosed() {
		kt.eventBus.unregister(this);
		super.onGuiClosed();
	}

	@Subscribe
	public void onLoginStateChange(final LoginState state) {
		if (state != null) {
			switch (state) {
			case FAILURE:
				setGuiLabelText("Ungültiger Benutzer");
				break;
			case ILLEGAL_JRE:
				setGuiLabelText("Ungültige JRE");
				break;
			default:
				break;
			}
			Minecraft.getMinecraft().displayGuiScreen(null);
		}
	}

}
