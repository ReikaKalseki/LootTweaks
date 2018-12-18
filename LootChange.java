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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Libraries.ReikaNBTHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.LootTweaks.LootTier.TierItemEntry;


public class LootChange implements Comparable<LootChange> {

	private final ChangeType type;
	private final LuaBlock data;
	public final String id;

	public LootChange(LuaBlock lb) {
		data = lb;
		type = ChangeType.valueOf(lb.getString("operation").toUpperCase(Locale.ENGLISH));
		id = lb.getString("type");
	}

	@Override
	public String toString() {
		return type.toString()+" "+data.toString();
	}

	public void apply(ChestGenHooks cgh, ArrayList<WeightedRandomChestContent> li, Field countMin, Field countMax) throws Exception {
		type.apply(cgh, li, countMin, countMax, data);
	}

	@Override
	public int compareTo(LootChange o) {
		return type.compareTo(o.type);
	}

	public static enum ChangeType {
		CLEAR(false),
		REMOVE(true),
		ADD(true),
		CHANGEWEIGHT(true),
		CHANGEBOUNDS(true),
		CHANGENBT(true),
		CHANGECOUNTS(false),
		ADDTIER(false),
		BATCH(false);

		private final boolean hasItem;

		private ChangeType(boolean b) {
			hasItem = b;
		}

		private void apply(ChestGenHooks cgh, ArrayList<WeightedRandomChestContent> li, Field countMin, Field countMax, LuaBlock data) throws Exception {
			ItemStack item = hasItem ? this.parseItem(data) : null;
			boolean matchNBT = data.getBoolean("match_nbt");
			boolean stopAtFirst = data.getBoolean("stop_at_first");
			switch(this) {
				case ADD:
					if (cgh == null && data.hasChild("fishing_params")) {
						LuaBlock lb = data.getChild("fishingParams");
						LootTable.unpackFishLootNBT(item, (float)lb.getDouble("damage_factor"), lb.getBoolean("random_enchant"));
					}
					li.add(new WeightedRandomChestContent(item, data.getInt("min_size"), data.getInt("max_size"), data.getInt("weight")));
					break;
				case ADDTIER:
					String tier = data.getString("tier");
					LootTier lt = LootTier.getTierByName(tier);
					if (lt == null)
						lt = LootTier.getTier(data.getInt("tier"));
					if (lt == null)
						throw new IllegalArgumentException("No such loot tier '"+tier+"'");
					double weightFactor = data.getDouble("weight_factor");
					int weight = (int)(lt.weight*weightFactor);
					for (TierItemEntry is : lt.getItems()) {
						li.add(new WeightedRandomChestContent(is.getItem(), lt.minCount, lt.maxCount, Math.max(1, (int)(weight*is.relativeWeight))));
					}
					break;
				case REMOVE:
					Iterator<WeightedRandomChestContent> it = li.iterator();
					while (it.hasNext()) {
						WeightedRandomChestContent wc = it.next();
						if (this.matchStack(wc, item, matchNBT)) {
							it.remove();
							if (stopAtFirst)
								break;
						}
					}
					break;
				case CHANGEBOUNDS:
					for (WeightedRandomChestContent wc : li) {
						if (this.matchStack(wc, item, matchNBT)) {
							wc.theMinimumChanceToGenerateItem = data.getInt("min_size");
							wc.theMaximumChanceToGenerateItem = data.getInt("max_size");
							if (stopAtFirst)
								break;
						}
					}
					break;
				case CHANGEWEIGHT:
					for (WeightedRandomChestContent wc : li) {
						if (this.matchStack(wc, item, matchNBT)) {
							if (data.containsKey("relative_weight"))
								wc.itemWeight *= data.getDouble("relative_weight");
							else
								wc.itemWeight = data.getInt("weight");
							if (stopAtFirst)
								break;
						}
					}
					break;
				case CHANGENBT:
					for (WeightedRandomChestContent wc : li) {
						if (this.matchStack(wc, item, matchNBT)) {
							wc.theItemId.stackTagCompound = data.hasChild("nbt") ? ReikaNBTHelper.constructNBT(data.getChild("nbt")) : null;
							if (stopAtFirst)
								break;
						}
					}
					break;
				case CHANGECOUNTS:
					if (cgh == null)
						break;
					countMin.setInt(cgh, data.getInt("min_count"));
					countMax.setInt(cgh, data.getInt("max_count"));
					break;
				case CLEAR:
					li.clear();
					break;
				case BATCH:
					String s = data.getString("entry");
					BatchChange bc = BatchChange.getBatch(s);
					if (bc == null)
						throw new IllegalArgumentException("No such batch file '"+s+"'!");
					bc.apply(cgh, li, countMin, countMax, data);
					break;
			}
		}

		private boolean matchStack(WeightedRandomChestContent wc, ItemStack is, boolean matchNBT) {
			return ReikaItemHelper.matchStacks(is, wc.theItemId) && (!matchNBT || ItemStack.areItemStackTagsEqual(is, wc.theItemId));
		}

		private ItemStack parseItem(LuaBlock data) {
			return LootTable.parser.parseItemString(data.getString("item"), data.getChild("item_nbt"), false);
		}
	}

}
