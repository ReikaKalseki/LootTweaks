package Reika.LootTweaks.ModInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import Reika.DragonAPI.ModList;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.LootTweaks.ModInterface.ModLootTable.ModLootTableEntry;

public class TwilightLootTables {

	private static Class coreClass;

	private static final String[] rarities = {
			"useless",
			"common",
			"uncommon",
			"rare",
			"ultrarare",
	};

	private static final String[] locations = {
			"hill1",
			"hill2",
			"hill3",
			"hedgemaze",
			"labyrinth_room",
			"labyrinth_deadend",
			"tower_room",
			"tower_library",
			"basement",
			"labyrinth_vault",
			"darktower_cache",
			"darktower_key",
			"darktower_boss",
			"tree_cache",
			"stronghold_cache",
			"stronghold_room",
			"stronghold_boss",
			"aurora_cache",
			"aurora_room",
			"aurora_boss",
			"troll_garden",
			"troll_vault",
	};

	private static Object getTreasureEntry(String s) throws Exception {
		Class c = getOrCreateCoreClass();
		Field f = c.getDeclaredField(s);
		f.setAccessible(true);
		return f.get(null);
	}

	private static Object getTreasureTable(Object entry, String table) throws Exception {
		Class c = getOrCreateCoreClass();
		Field f = c.getDeclaredField(table);
		f.setAccessible(true);
		return f.get(entry);
	}

	private static Class getOrCreateCoreClass() throws Exception {
		if (coreClass == null)
			coreClass = Class.forName("twilightforest.TFTreasure");
		return coreClass;
	}

	private static Collection<WeightedRandomChestContent> getItems(Object table) throws Exception {
		Collection<WeightedRandomChestContent> c = new ArrayList();
		Field f = table.getClass().getDeclaredField("list");
		f.setAccessible(true);
		ArrayList li = (ArrayList)f.get(table);
		for (Object o : li) {
			c.add(convertLootItem(o));
		}
		return c;
	}

	public static void setItems(Object table, Collection<WeightedRandomChestContent> li) throws Exception {
		Field f = table.getClass().getDeclaredField("list");
		f.setAccessible(true);
		ArrayList c = (ArrayList)f.get(table);
		c.clear();
		for (WeightedRandomChestContent wc : li) {
			c.add(deConvertLootItem(wc));
		}
	}

	private static WeightedRandomChestContent convertLootItem(Object o) throws Exception {
		Field item = o.getClass().getDeclaredField("itemStack");
		item.setAccessible(true);
		Field wt = o.getClass().getDeclaredField("rarity");
		wt.setAccessible(true);
		ItemStack is = (ItemStack)item.get(o);
		return new WeightedRandomChestContent(ReikaItemHelper.getSizedItemStack(is, 1), 1, is.stackSize, wt.getInt(o));
	}

	private static Object deConvertLootItem(WeightedRandomChestContent wc) throws Exception {
		Class c = Class.forName("twilightforest.TFTreasureItem");
		Constructor ctr = c.getConstructor(ItemStack.class, int.class);
		ItemStack is = ReikaItemHelper.getSizedItemStack(wc.theItemId, wc.theMaximumChanceToGenerateItem);
		return ctr.newInstance(is, wc.itemWeight);
	}

	private static class TFEntry extends ModLootTableEntry {

		private final String location;
		private final String rarity;

		TFEntry(String s, String r) {
			super(s+"_"+r, TFTable.class, ModList.TWILIGHT);
			location = s;
			rarity = r;
		}

		@Override
		protected ModLootTable constructLootTable(File f) throws Exception {
			return new TFTable(location, rarity, new File(new File(f.getParentFile(), "TwilightForest"), key+".tweaks"));
		}

	}

	private static final class TFTable extends ModLootTable {

		private final String location;
		private final String rarity;

		protected TFTable(String s, String r, File f) throws IOException {
			super(ModList.TWILIGHT, s+"_"+r, f);
			location = s;
			rarity = r;
		}

		@Override
		public Default createDefault() throws Exception {
			return new TFDefault();
		}

		@Override
		public WeightedRandomChestContent[] getItems() {
			try {
				Collection<WeightedRandomChestContent> c = TwilightLootTables.getItems(this.getTreasureData());
				return c.toArray(new WeightedRandomChestContent[c.size()]);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Object getTreasureData() throws Exception {
			return TwilightLootTables.getTreasureTable(TwilightLootTables.getTreasureEntry(location), rarity);
		}

		@Override
		protected void setLoot(ArrayList<WeightedRandomChestContent> li) {
			try {
				TwilightLootTables.setItems(this.getTreasureData(), li);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private class TFDefault extends Default {

			private TFDefault() throws Exception {
				super(ReikaJavaLibrary.makeListFromArray(TFTable.this.getItems()), 0, 0);
			}

			@Override
			protected void restore(ChestGenHooks cgh) throws Exception {
				TFTable.this.setLoot(items);
			}

		}

	}

	static Collection<ModLootTableEntry> getInit() {
		ArrayList<ModLootTableEntry> li = new ArrayList();
		for (String s : locations) {
			for (String r : rarities) {
				li.add(new TFEntry(s, r));
			}
		}
		return li;
	}

}
