/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.LootTweaks;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.util.WeightedRandomFishable;
import net.minecraft.world.gen.structure.StructureNetherBridgePieces;
import net.minecraftforge.common.ChestGenHooks;
import Reika.DragonAPI.Instantiable.IO.CustomRecipeList;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;


public class LootTable {

	private static Field chestTable;
	private static Field chestContents;
	private static Field chestCountMin;
	private static Field chestCountMax;

	public final String key;
	private final String referenceFile;

	static final CustomRecipeList parser = new CustomRecipeList(LootTweaks.instance, "");

	protected final ArrayList<LootChange> changes = new ArrayList();

	private static final String NETHER_FORTRESS_KEY = "netherFortress";
	private static final String FISHING_KEY = "fishing";

	private static final HashMap<String, Default> defaultCache = new HashMap();
	public static HashMap<String, LootTable> tables = new HashMap();

	protected LootTable(String s, File ref) {
		key = s;
		referenceFile = ref.getAbsolutePath();
		tables.put(key, this);
	}

	public static LootTable construct(String s, File f) {
		if (s.equals(NETHER_FORTRESS_KEY))
			return new NetherFortressLootTable(f);
		else if (s.equals(FISHING_KEY))
			return new NetherFortressLootTable(f);
		else
			return new LootTable(s, f);
	}

	private static Default constructDefault(String s) throws Exception {
		if (s.equals(NETHER_FORTRESS_KEY)) {
			return new NetherFortressDefault();
		}
		else if (s.equals(FISHING_KEY)) {
			return new NetherFortressDefault();
		}
		else {
			ChestGenHooks cgh = ((Map<String, ChestGenHooks>)chestTable.get(null)).get(s);
			return new Default(cgh);
		}
	}

	public static Collection<String> getValidTables() throws Exception {
		Collection<String> c = new ArrayList(((Map<String, ChestGenHooks>)chestTable.get(null)).keySet());
		c.add(NETHER_FORTRESS_KEY);
		c.add(FISHING_KEY);
		return c;
	}

	public static void cacheDefaults() throws Exception {
		for (String s : tables.keySet()) {
			try {
				defaultCache.put(s, constructDefault(s));
			}
			catch (Exception e) {
				throw new RuntimeException("Error caching loot table '"+s+"'.", e);
			}
		}
	}

	public static void restoreDefaults() throws Exception {
		for (String s : defaultCache.keySet()) {
			try {
				Default def = defaultCache.get(s);
				ChestGenHooks cgh = ((Map<String, ChestGenHooks>)chestTable.get(null)).get(s);
				def.restore(cgh);
			}
			catch (Exception e) {
				throw new RuntimeException("Error restoring loot table '"+s+"'.", e);
			}
		}
	}

	public static void applyAll() throws Exception {
		for (LootTable lt : tables.values()) {
			lt.applyChanges();
		}
	}

	public static void reload() throws Exception {
		if (!LootTweaks.allowReload())
			throw new IllegalStateException("Dynamic reloading is not enabled!");
		restoreDefaults();
		for (LootTable lt : tables.values())
			lt.load(new File(lt.referenceFile));
		applyAll();
	}

	public final void load(File f) {
		parser.clear();
		parser.load(f);
		changes.clear();
		for (LuaBlock lb : parser.getEntries()) {
			changes.add(new LootChange(lb));
		}
		Collections.sort(changes);
		parser.clear();
	}

	protected void applyChanges() throws Exception {
		if (changes.isEmpty())
			return;
		try {
			ChestGenHooks cgh = ((Map<String, ChestGenHooks>)chestTable.get(null)).get(key);
			ArrayList<WeightedRandomChestContent> li = (ArrayList<WeightedRandomChestContent>)chestContents.get(cgh);
			for (LootChange c : changes) {
				try {
					c.apply(cgh, li, chestCountMin, chestCountMax);
				}
				catch (Exception e) {
					throw new RuntimeException("Could not apply loot change '"+c.id+"'", e);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Error modifying loot table '"+key+"'.", e);
		}
	}

	public static class NetherFortressLootTable extends LootTable {

		protected NetherFortressLootTable(File f) {
			super(NETHER_FORTRESS_KEY, f);
		}

		@Override
		protected void applyChanges() throws Exception {
			ArrayList<WeightedRandomChestContent> li = ReikaJavaLibrary.makeListFromArray(StructureNetherBridgePieces.Piece.field_111019_a);
			for (LootChange c : changes) {
				c.apply(null, li, chestCountMin, chestCountMax);
			}
			StructureNetherBridgePieces.Piece.field_111019_a = li.toArray(new WeightedRandomChestContent[li.size()]);
		}

	}

	public static class FishingLootTable extends LootTable {

		protected FishingLootTable(File f) {
			super(FISHING_KEY, f);
		}

		@Override
		protected void applyChanges() throws Exception {
			ArrayList<WeightedRandomChestContent> li = unpackFishLoot(EntityFishHook.field_146041_e);
			for (LootChange c : changes) {
				c.apply(null, li, chestCountMin, chestCountMax);
			}
			EntityFishHook.field_146041_e = packFishLoot(li);
		}

	}

	private static class Default {

		protected final ArrayList<WeightedRandomChestContent> items;
		protected final int countMin;
		protected final int countMax;

		private Default(ChestGenHooks cgh) throws Exception {
			items = new ArrayList();
			for (WeightedRandomChestContent c : (ArrayList<WeightedRandomChestContent>)chestContents.get(cgh)) {
				items.add(copy(c));
			}
			countMin = chestCountMin.getInt(cgh);
			countMax = chestCountMax.getInt(cgh);
		}

		private Default(ArrayList<WeightedRandomChestContent> li, int min, int max) throws Exception {
			items = li;
			countMax = max;
			countMin = min;
		}

		protected void restore(ChestGenHooks cgh) throws Exception {
			chestContents.set(cgh, new ArrayList(items));
			chestCountMin.setInt(cgh, countMin);
			chestCountMax.setInt(cgh, countMax);
		}

	}

	private static class NetherFortressDefault extends Default {

		private NetherFortressDefault() throws Exception {
			super(ReikaJavaLibrary.makeListFromArray(StructureNetherBridgePieces.Piece.field_111019_a), 0, 0);
		}

		@Override
		protected void restore(ChestGenHooks cgh) throws Exception {
			StructureNetherBridgePieces.Piece.field_111019_a = items.toArray(new WeightedRandomChestContent[items.size()]);
		}

	}

	private static class FishingDefault extends Default {

		private FishingDefault() throws Exception {
			super(unpackFishLoot(EntityFishHook.field_146041_e), 0, 0);
		}

		@Override
		protected void restore(ChestGenHooks cgh) throws Exception {
			EntityFishHook.field_146041_e = packFishLoot(items);
		}

	}

	private static List<WeightedRandomFishable> packFishLoot(ArrayList<WeightedRandomChestContent> items) {
		ArrayList<WeightedRandomFishable> li = new ArrayList();
		for (WeightedRandomChestContent w : items) {
			li.add(packFishLoot(w));
		}
		return li;
	}

	private static ArrayList<WeightedRandomChestContent> unpackFishLoot(List<WeightedRandomFishable> items) {
		ArrayList<WeightedRandomChestContent> li = new ArrayList();
		for (WeightedRandomFishable w : items) {
			li.add(unpackFishLoot(w));
		}
		return li;
	}

	private static WeightedRandomFishable packFishLoot(WeightedRandomChestContent w) {
		WeightedRandomFishable f = new WeightedRandomFishable(w.theItemId.copy(), w.itemWeight);
		packFishLootNBT(f, w.theItemId);
		return f;
	}

	private static WeightedRandomChestContent unpackFishLoot(WeightedRandomFishable f) {
		WeightedRandomChestContent w = new WeightedRandomChestContent(f.field_150711_b, 1, 1, f.itemWeight);
		if (f.field_150712_c > 0 || f.field_150710_d) {
			unpackFishLootNBT(w.theItemId, f.field_150712_c, f.field_150710_d);
		}
		return w;
	}

	private static WeightedRandomChestContent copy(WeightedRandomChestContent c) {
		return new WeightedRandomChestContent(c.theItemId.copy(), c.theMinimumChanceToGenerateItem, c.theMaximumChanceToGenerateItem, c.itemWeight);
	}

	static void packFishLootNBT(WeightedRandomFishable f, ItemStack item) {
		if (item.stackTagCompound != null) {
			float dmg = item.stackTagCompound.getCompoundTag("lootdata").getFloat("damage");
			boolean ench = item.stackTagCompound.getCompoundTag("lootdata").getBoolean("enchant");
			item.stackTagCompound.removeTag("lootdata");
			if (dmg > 0)
				f.func_150709_a(dmg);
			if (ench)
				f.func_150707_a();
		}
	}

	static void unpackFishLootNBT(ItemStack item, float dmg, boolean ench) {
		if (item.stackTagCompound == null)
			item.stackTagCompound = new NBTTagCompound();
		NBTTagCompound tag = new NBTTagCompound();
		tag.setFloat("damage", dmg);
		tag.setBoolean("enchant", ench);
		item.stackTagCompound.setTag("lootdata", tag);
	}

	static {
		try {
			chestTable = ChestGenHooks.class.getDeclaredField("chestInfo");
			chestTable.setAccessible(true);

			chestContents = ChestGenHooks.class.getDeclaredField("contents");
			chestContents.setAccessible(true);

			chestCountMin = ChestGenHooks.class.getDeclaredField("countMin");
			chestCountMin.setAccessible(true);

			chestCountMax = ChestGenHooks.class.getDeclaredField("countMax");
			chestCountMax.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}