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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import net.minecraft.item.ItemStack;

import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;


public class LootTier implements Comparable<LootTier> {

	public static File folder;

	public final String name;
	public final int index;
	public final int weight;
	public final int minCount;
	public final int maxCount;

	private final ArrayList<TierItemEntry> items = new ArrayList();

	private static final HashMap<Integer, LootTier> tiers = new HashMap();
	private static final HashMap<String, LootTier> tierNames = new HashMap();

	public LootTier(String n, int idx, int min, int max, int wt) {
		name = n;
		index = idx;

		minCount = min;
		maxCount = max;
		weight = wt;

		if (tiers.containsKey(tiers))
			throw new IllegalArgumentException("Loot tier "+index+"already declared!");
		tiers.put(index, this);
		tierNames.put(name, this);
	}

	public LootTier addItem(ItemStack is, float relativeWeight) {
		items.add(new TierItemEntry(is, relativeWeight));
		return this;
	}

	@Override
	public int compareTo(LootTier lt) {
		return Integer.compare(index, lt.index);
	}

	public Collection<TierItemEntry> getItems() {
		return Collections.unmodifiableCollection(items);
	}

	public static LootTier getTier(int tier) {
		return tiers.get(tier);
	}

	public static LootTier getTierByName(String n) {
		return tierNames.get(n);
	}

	public static void reload() throws Exception {
		tiers.clear();
		loadTierFiles();
	}

	public static void loadTierFiles() throws Exception {
		for (File f : ReikaFileReader.getAllFilesInFolder(folder, ".loottier")) {
			loadFile(f);
		}
	}

	private static void loadFile(File f) throws Exception {
		LootTable.parser.clear();
		LootTable.parser.load(f);
		if (LootTable.parser.getEntries().isEmpty())
			return;
		if (LootTable.parser.getEntries().size() != 1)
			throw new IllegalArgumentException("Invalid tier file '"+f.getName()+"': Only one tier definition per file!");
		LuaBlock lb = LootTable.parser.getEntries().iterator().next();
		String s = f.getName();
		s = s.substring(0, s.indexOf('.'));
		LootTier lt = new LootTier(s, lb.getInt("tier"), lb.getInt("min_size"), lb.getInt("max_size"), lb.getInt("weight"));
		LuaBlock items = lb.getChild("items");
		if (!items.getDataValues().isEmpty()) {
			for (String item : items.getDataValues()) {
				if (item.startsWith("//"))
					continue;
				for (ItemStack is : LootTable.parser.parseItemCollection(ReikaJavaLibrary.makeListFrom(item), false))
					lt.addItem(is, 1);
			}
		}
		else {
			for (LuaBlock item : items.getChildren()) {
				ItemStack is = LootTable.parser.parseItemString(item.getString("item"), item.getChild("nbt"), false);
				float w = item.containsKey("relative_weight") ? (float)item.getDouble("relative_weight") : 1;
				lt.addItem(is, w);
			}
		}
		LootTable.parser.clear();
	}

	public static class TierItemEntry {

		private final ItemStack item;
		public final float relativeWeight;

		private TierItemEntry(ItemStack is, float w) {
			item = is.copy();
			relativeWeight = w;
		}

		public ItemStack getItem() {
			return item.copy();
		}

		@Override
		public String toString() {
			return item.toString()+" W="+relativeWeight;
		}

	}

}
