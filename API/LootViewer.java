package Reika.LootTweaks.API;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;


public class LootViewer {

	private final ArrayList<LootItem> data = new ArrayList();
	public final String location;

	public LootViewer(String s, WeightedRandomChestContent[] li) {
		this(s, ReikaJavaLibrary.makeListFromArray(li));
	}

	public LootViewer(String s, ArrayList<WeightedRandomChestContent> li) {
		for (WeightedRandomChestContent wr : li) {
			data.add(new LootItem(wr, this.calculateChance(wr, li)));
		}
		location = s;
	}

	private float calculateChance(WeightedRandomChestContent wr, ArrayList<WeightedRandomChestContent> li) {
		float sum = 0;
		for (WeightedRandomChestContent wc : li) {
			sum += wc.itemWeight;
		}
		return wr.itemWeight/sum*100;
	}

	public Collection<LootItem> getLoot() {
		return Collections.unmodifiableCollection(data);
	}

	public static class LootItem {

		private final WeightedRandomChestContent item;

		/** Taking all the other items in the table into account. 0-100. */
		public final float netChance;

		private LootItem(WeightedRandomChestContent wr, float f) {
			item = wr;
			netChance = f;
		}

		public ItemStack getItem() {
			return item.theItemId.copy();
		}

		public int[] getStackSizeRange() {
			if (item.theMinimumChanceToGenerateItem == item.theMaximumChanceToGenerateItem) {
				return new int[]{item.theMinimumChanceToGenerateItem};
			}
			else {
				return new int[]{item.theMinimumChanceToGenerateItem, item.theMaximumChanceToGenerateItem};
			}
		}

		public int getWeight() {
			return item.itemWeight;
		}

	}

}
