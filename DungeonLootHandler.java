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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import Reika.DragonAPI.Libraries.ReikaEnchantmentHelper;
import Reika.DragonAPI.Libraries.Java.ReikaGLHelper.BlendMode;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.Libraries.Rendering.ReikaGuiAPI;
import Reika.LootTweaks.API.LootViewer;
import Reika.LootTweaks.API.LootViewer.LootItem;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class DungeonLootHandler extends TemplateRecipeHandler {

	private final Comparator<CachedRecipe> displaySorter = new Comparator<CachedRecipe>() {

		@Override
		public int compare(CachedRecipe o1, CachedRecipe o2) {
			if (o1 instanceof LootTableEntry && o2 instanceof LootTableEntry) {
				LootTableEntry l1 = (LootTableEntry)o1;
				LootTableEntry l2 = (LootTableEntry)o2;
				int idx1 = l1.chestType.indexOf('-');
				int idx2 = l2.chestType.indexOf('-');
				if ((idx2 == -1) != (idx1 == -1)) {
					return idx1 == -1 ? -1 : 1;
				}
				int ret = l1.chestType.compareTo(l2.chestType);
				return ret*1000000+Integer.compare(l2.item.getWeight(), l1.item.getWeight());//ReikaItemHelper.comparator.compare(l1.item.getItem(), l2.item.getItem());
			}
			else if (o1 instanceof LootTableEntry) {
				return Integer.MAX_VALUE;
			}
			else if (o2 instanceof LootTableEntry) {
				return Integer.MIN_VALUE;
			}
			else
				return 0;
		}

	};

	public class LootTableEntry extends CachedRecipe {

		private final String chestType;
		private final LootItem item;

		public LootTableEntry(String chest, LootItem is) {
			item = is;
			chestType = chest;
		}

		@Override
		public PositionedStack getResult() {
			return new PositionedStack(item.getItem(), 5, 15);
		}

		@Override
		public PositionedStack getIngredient()
		{
			return null;
		}
	}

	@Override
	public String getRecipeName() {
		return "Dungeon Loot";
	}

	@Override
	public String getGuiTexture() {
		return "unknown.png";
	}

	@Override
	public void drawBackground(int recipe)
	{
		GL11.glColor4f(1, 1, 1, 1);
		//ReikaTextureHelper.bindTexture(RotaryCraft.class, this.getGuiTexture());
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		ReikaGuiAPI.instance.drawTexturedModalRectWithDepth(0, 1, 5, 11, 166, 70, ReikaGuiAPI.NEI_DEPTH);
	}

	@Override
	public void drawForeground(int recipe)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_LIGHTING);
		//ReikaTextureHelper.bindTexture(RotaryCraft.class, this.getGuiTexture());
		this.drawExtras(recipe);
	}

	@Override
	public void loadTransferRects() {
		transferRects.add(new RecipeTransferRect(new Rectangle(0, 3, 165, 52), "drloot"));
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if (outputId != null && outputId.equals("drloot")) {
			try {
				Collection<String> c = LootTable.getValidTables();
				for (String s : c) {
					LootViewer lw = LootTable.getLootViewer(s);
					for (LootItem li : lw.getLoot()) {
						arecipes.add(new LootTableEntry(lw.location, li));
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		Collections.sort(arecipes, displaySorter);
		super.loadCraftingRecipes(outputId, results);
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		super.loadUsageRecipes(inputId, ingredients);
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		super.loadCraftingRecipes(result);
		arecipes.addAll(this.getEntriesForItem(result));
		Collections.sort(arecipes, displaySorter);
	}

	private Collection<LootTableEntry> getEntriesForItem(ItemStack is) {
		ArrayList<LootTableEntry> li = new ArrayList();
		try {
			Collection<String> c = LootTable.getValidTables();
			for (String s : c) {
				LootViewer lw = LootTable.getLootViewer(s);
				for (LootItem lti : lw.getLoot()) {
					if (ReikaItemHelper.matchStacks(lti.getItem(), is))
						li.add(new LootTableEntry(s, lti));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return li;
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		super.loadUsageRecipes(ingredient);
	}

	@Override
	public Class<? extends GuiContainer> getGuiClass() {
		return null;
	}

	@Override
	public int recipiesPerPage() {
		return 1;
	}

	@Override
	public void drawExtras(int recipe)
	{
		CachedRecipe r = arecipes.get(recipe);
		if (r instanceof LootTableEntry) {
			BlendMode.DEFAULT.apply();
			ReikaGuiAPI api = ReikaGuiAPI.instance;
			api.drawLine(0, 3, 165, 3, 0xff707070);
			api.drawLine(0, 15, 165, 15, 0xffaaaaaa);
			FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
			LootTableEntry e = (LootTableEntry)r;
			LootItem wc = e.item;
			api.drawCenteredStringNoShadow(fr, "Location: '"+e.chestType+"'", 82, 5, 0x000000);
			String n = wc.getItem().getDisplayName();
			int dy = 0;
			if (wc.getItem().getItem() == Items.enchanted_book) {
				HashMap<Enchantment, Integer> map = ReikaEnchantmentHelper.getEnchantments(wc.getItem());
				for (Enchantment en : map.keySet()) {
					fr.drawString(en.getTranslatedName(map.get(en)), 34, 31+dy, 0x505050);
					dy += 11;
				}
			}
			else if (wc.getItem().isItemEnchanted()) {
				n = n+", Enchanted";
				HashMap<Enchantment, Integer> map = ReikaEnchantmentHelper.getEnchantments(wc.getItem());
				for (Enchantment en : map.keySet()) {
					fr.drawString(en.getTranslatedName(map.get(en)), 34, 31+dy, 0x505050);
					dy += 11;
				}
			}
			fr.drawString(n, 26, 20, 0x000000);
			List<String> li = new ArrayList();
			wc.getItem().getItem().addInformation(wc.getItem(), Minecraft.getMinecraft().thePlayer, li, true);
			for (String s : li) {
				fr.drawString(s, 34, 31+dy, 0x505050);
				dy += 11;
			}
			int[] sizes = wc.getStackSizeRange();
			String counts = sizes.length == 1 ? sizes[0]+"x" : sizes[0]+"x - "+sizes[1]+"x";
			fr.drawString("Stack Size: "+counts, 26, 31+dy, 0x000000);
			fr.drawString("Weight: "+wc.getWeight(), 26, 42+dy, 0x000000);
			String s = String.format("%.4f", wc.netChance);
			while (s.charAt(s.length()-1) == '0')
				s = s.substring(0, s.length()-1);
			if (s.endsWith("."))
				s = s+"0";
			fr.drawString("Net Chance: "+s+"%", 26, 53+dy, 0x000000);
			api.drawLine(0, 63+dy, 165, 63+dy, 0xff707070);
		}
	}
}
