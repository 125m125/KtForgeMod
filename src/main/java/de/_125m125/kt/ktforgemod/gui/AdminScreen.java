package de._125m125.kt.ktforgemod.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de._125m125.kt.ktforgemod.KadcontradeMod;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class AdminScreen extends GuiScreen {
	private static final int USERNAME_ID = 1000;

	private static List<String> knownUsers;
	private static String lastInspectedUser;

	private final Map<Integer, GuiLabel> labels = new HashMap<>();
	private final Map<Integer, GuiTextField> fields = new HashMap<>();

	public AdminScreen(final KadcontradeMod mod) {

	}

	@Override
	public void initGui() {
		super.initGui();

		addLabel(new GuiLabel(fontRenderer, 1000003, width / 8, 110, this.width * 3 / 8, 20, 0xFFFFFF),
				"Benutzer ID: ");
		fields.put(USERNAME_ID,
				new GuiTextField(USERNAME_ID, fontRenderer, this.width / 2, 110, this.width * 3 / 8, 20));
		fields.get(USERNAME_ID).setMaxStringLength(20);
		fields.get(USERNAME_ID).setFocused(true);
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

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		drawDefaultBackground();
		fields.values().forEach(GuiTextField::drawTextBox);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
