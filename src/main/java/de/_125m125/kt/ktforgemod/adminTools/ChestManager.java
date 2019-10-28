package de._125m125.kt.ktforgemod.adminTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.google.common.eventbus.Subscribe;

import de._125m125.kt.ktapi.core.entities.ItemName;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktforgemod.KadcontradeMod;
import de._125m125.kt.ktforgemod.LoginState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiContainerEvent.DrawForeground;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChestManager {
	public enum TransferMode {
		SPECIAL(ChestManager::isSpecialItemStack), MASS(ChestManager::isCurrentMassItem),
		OTHER(ChestManager::isNormalItemStack),;

		private final BiPredicate<ChestManager, ItemStack> filter;

		private TransferMode(final BiPredicate<ChestManager, ItemStack> filter) {
			this.filter = filter;
		}

		public boolean shouldCollect(final ChestManager manger, final ItemStack stack) {
			return filter.test(manger, stack);
		}
	}

	private volatile boolean enabled = false;

	private boolean newChest = false;
	private final List<BlockPos> checkedLocations = new ArrayList<>();
	private final Map<String, Integer> normalCount = new HashMap<>();

	private final KadcontradeMod mod;

	private final TransferMode mode = TransferMode.SPECIAL;

	private final Map<String, Integer> expectedInventory = new HashMap<>();

	private Set<String> validIds;
	private String currentMassItem;
	private final boolean indexing = true;

	public ChestManager(final KadcontradeMod mod) {
		this.mod = mod;
		mod.eventBus.register(this);
	}

	@Subscribe
	public void onLoginStateChange(final LoginState state) {
		if (state == LoginState.SUCCESS) {
			getKnownItemIds();
		}
	}

	@SubscribeEvent
	public void onGuiOpen(final GuiOpenEvent guiOpenEvent) {
		if (enabled && guiOpenEvent.getGui() instanceof GuiChest) {
			newChest = true;
		}
	}

	public void reset() {
		checkedLocations.clear();
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}

	public void toggleEnable() {
		checkedLocations.clear();
		normalCount.clear();
		expectedInventory.clear();
		enabled = !enabled;
	}

	@SubscribeEvent
	public synchronized void onContainerDrawForeground(final DrawForeground drawForegroundEvent) {
		if (!enabled)
			return;
		final GuiContainer guiContainer = drawForegroundEvent.getGuiContainer();
		if (newChest && guiContainer instanceof GuiChest) {
			final RayTraceResult rayTrace = Minecraft.getMinecraft().player.rayTrace(100, 1f);
			if (rayTrace.typeOfHit != RayTraceResult.Type.BLOCK) {
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Konnte Kiste nicht finden... Bitte verscuhe es erneut!"));
				newChest = false;
				return;
			}
			final WorldClient world = Minecraft.getMinecraft().world;
			BlockPos blockPos = rayTrace.getBlockPos();
			Block block = world.getBlockState(blockPos).getBlock();
			if (block instanceof BlockStandingSign) {
				blockPos = blockPos.down();
				block = world.getBlockState(blockPos).getBlock();
			} else if (block instanceof BlockWallSign) {
				blockPos = blockPos
						.offset(world.getBlockState(blockPos).getValue(BlockHorizontal.FACING).getOpposite());
				block = world.getBlockState(blockPos).getBlock();
			}
			if (!(block instanceof BlockChest)) {
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Konnte Kiste nicht finden... Bitte versuche es erneut!"));
				newChest = false;
				return;
			}

			if (!validateInventoryWithNotification(guiContainer)) {
				newChest = false;
				return;
			}

			if (!checkedLocations.isEmpty()) {
				final BlockPos expected = checkedLocations.get(0);
				final int expectedX = expected.getX() - (expected.getX() % 16);
				final int expectedZ = expected.getZ() - (expected.getZ() % 16);
				if (blockPos.getX() < expectedX || blockPos.getX() >= expectedX + 16 || blockPos.getZ() < expectedZ
						|| blockPos.getZ() >= expectedZ + 16) {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Outside of chunk..."));
					unload();
					newChest = false;
					return;
				}
			}

			boolean index = indexing;
			final BlockPos finalBlockPos = blockPos;
			if (index && checkedLocations.stream().filter(l -> l.getY() == finalBlockPos.getY())
					.anyMatch(l -> l.distanceSq(finalBlockPos) <= 1)) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Kiste bereits indexiert..."));
				index = false;
			}

			boolean empty = true;
			final NonNullList<ItemStack> inventory = guiContainer.inventorySlots.getInventory();
			for (int i = 0; i < inventory.size() - 36; i++) {
				final ItemStack itemStack = inventory.get(i);
				if (itemStack.isEmpty()) {
					continue;
				}
				empty = false;
//				if (!isSpecialItemStack(itemStack)) {
//					final String id = getKadonId(itemStack);
//					thisInventory.put(id, Integer.sum(thisInventory.getOrDefault(id, 0), itemStack.getCount()));
//				}
				final String kadonId = getKadonId(itemStack);
				final int count = itemStack.getCount();
				if (mode.shouldCollect(this, itemStack)) {
					if (!inventoryFull(inventory)) {
//						guiContainer.inventorySlots.slotClick(i, 0, ClickType.QUICK_MOVE,
//								Minecraft.getMinecraft().player);
						Minecraft.getMinecraft().playerController.windowClick(guiContainer.inventorySlots.windowId, i,
								0, ClickType.QUICK_MOVE, Minecraft.getMinecraft().player);
						Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Taking from slot " + i));
						expectedInventory.put(kadonId, expectedInventory.getOrDefault(kadonId, 0) + count);
					}
				} else {
					if (index && isNormalItemStack(itemStack)) {
						normalCount.put(kadonId, normalCount.getOrDefault(kadonId, 0) + count);
					}
				}
			}
			if (empty) {
				newChest = true;
				return;
			}
			checkedLocations.add(blockPos);
			if (index) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Kiste erfolgreich indexiert..."));
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString("ItemCounts: " + normalCount));
			}
			newChest = false;
		}
	}

	private boolean inventoryFull(final NonNullList<ItemStack> inventory) {
		return !inventory.get(inventory.size() - 10).isEmpty();
	}

	private boolean validateInventoryWithNotification(final GuiContainer guiContainer) {
		final Map<String, Integer> validateInventory = validateInventory(guiContainer.inventorySlots.getInventory());
		if (!validateInventory.isEmpty()) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Inventar entspricht nicht den Erwartungen!"));
			final Map<Boolean, String> collect = validateInventory.entrySet().stream()
					.map(e -> e.getValue() + "*" + e.getKey())
					.collect(Collectors.groupingBy(s -> s.startsWith("-"), Collectors.joining(", ")));
			if (collect.containsKey(true))
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Fehlende Items: " + collect.get(true)));
			if (collect.containsKey(false))
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Überflüssige Items: " + collect.get(false)));
		}
		return validateInventory.isEmpty();
	}

	private Map<String, Integer> validateInventory(final NonNullList<ItemStack> inventory) {
		final Map<String, Integer> actual = new HashMap<>();
		for (int i = inventory.size() - 27; i < inventory.size(); i++) {
			final ItemStack itemStack = inventory.get(i);
			if (itemStack.isEmpty())
				continue;
			final String kadonId = getKadonId(itemStack);
			actual.put(kadonId, actual.getOrDefault(kadonId, 0) + itemStack.getCount());
		}
		System.out.println("is: " + actual);
		System.out.println("expected: " + expectedInventory);
		for (final Entry<String, Integer> current : expectedInventory.entrySet()) {
			if (actual.get(current.getKey()) == current.getValue())
				actual.remove(current.getKey());
			else
				actual.put(current.getKey(), actual.getOrDefault(current.getKey(), 0) - current.getValue());
		}
		return actual;
	}

	private void unload() {
		// TODO Auto-generated method stub

	}

	public boolean isSpecialItemStack(final ItemStack stack) {
		String expectedName = null;
		if (stack.isItemEnchanted()) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Enchanted Item: " + stack.getDisplayName()));
			return true;
		}
		if (stack.getItem() instanceof ItemSkull) {
			expectedName = getKadconSkullId(stack);
			if (expectedName == null) {
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Detected skull: " + stack.getDisplayName()));
			}
		} else {
			if (stack.getTagCompound() != null && !stack.getTagCompound().hasNoTags()) {
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Detected NBTed item: " + stack.getDisplayName()));
				return true;
			}
		}
		if (stack.hasDisplayName() && (expectedName == null || !expectedName.equals(stack.getDisplayName()))) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Detected renamed item: " + stack.getDisplayName()));
			return true;
		}
		if (stack.isItemDamaged()) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Detected damaged item: " + stack.getDisplayName()));
			return true;
		}
		if (stack.getRepairCost() != 0) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Detected repair cost on item: " + stack.getDisplayName()));
			return true;
		}
		if (!getKnownItemIds().contains(getKadonId(stack))) {
			Minecraft.getMinecraft().player
					.sendMessage(new TextComponentString("Detected unknown item: " + stack.getDisplayName()));
			return true;
		}
		return false;
	}

	public boolean isNormalItemStack(final ItemStack stack) {
		return !isSpecialItemStack(stack);
	}

	public boolean isCurrentMassItem(final ItemStack stack) {
		return isNormalItemStack(stack) && getKadonId(stack).equals(currentMassItem);
	}

	public String getKadonId(final ItemStack stack) {
		if (stack.getItem() instanceof ItemSkull) {
			return getKadconSkullId(stack);
		}
		System.out.println(stack);
		final int id = Item.getIdFromItem(stack.getItem());
		final int damage = stack.getItemDamage();
		if (damage == 0) {
			return String.valueOf(id);
		}
		System.out.println(id + ":" + damage);
		return id + ":" + damage;
	}

	public String getKadconSkullId(final ItemStack stack) {
		if (stack.getItem() instanceof ItemSkull) {
			if (stack.getMetadata() == 3 && stack.hasTagCompound()) {
				String owner = null;
				if (stack.getTagCompound().hasKey("SkullOwner", 8)) {
					owner = stack.getTagCompound().getString("SkullOwner");
				}

				if (stack.getTagCompound().hasKey("SkullOwner", 10)) {
					final NBTTagCompound nbttagcompound = stack.getTagCompound().getCompoundTag("SkullOwner");

					if (nbttagcompound.hasKey("Name", 8)) {
						owner = nbttagcompound.getString("Name");
					}
				}
				if (owner != null && owner.startsWith("c_")) {
					return owner.substring(2);
				}
			}
		}
		return null;
	}

	private Set<String> getKnownItemIds() {
		if (validIds != null)
			return validIds;
		mod.getRequester().getItemNames().addCallback(Callback.successCallback((status, result) -> {
			validIds = result.stream().map(ItemName::getId).collect(Collectors.toSet());
		}));
		return validIds;
	}
}
