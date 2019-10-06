package Reika.LootTweaks.ModInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import Reika.DragonAPI.ModList;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Java.ReikaReflectionHelper;

public abstract class ReflectiveLootTable extends ModLootTable {

	private Class referenceClass;
	private Object referenceObject;
	private Field referenceField;

	protected ReflectiveLootTable(ModList mod, String c, String f, String s, File ref) throws IOException {
		super(mod, s, ref);
		if (mod.isLoaded()) {
			try {
				referenceClass = Class.forName(c);
				referenceField = referenceClass.getDeclaredField(f);
				referenceField.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(referenceField, referenceField.getModifiers() & ~Modifier.FINAL);
				try {
					Field fi = referenceClass.getDeclaredField("instance");
					fi.setAccessible(true);
					referenceObject = fi.get(null);
				}
				catch (NoSuchFieldException e) { //may be static
					referenceObject = null;
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	protected final ArrayList<WeightedRandomChestContent> getLoot() {
		try {
			return ReikaJavaLibrary.makeListFromArray((WeightedRandomChestContent[])referenceField.get(referenceObject));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected final void setLoot(ArrayList<WeightedRandomChestContent> li) {
		try {
			ReikaReflectionHelper.setFinalField(referenceField, referenceObject, li.toArray(new WeightedRandomChestContent[li.size()]));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Default createDefault() throws Exception {
		return new ReflectiveDefault();
	}

	@Override
	public final WeightedRandomChestContent[] getItems() {
		ArrayList<WeightedRandomChestContent> li = this.getLoot();
		return li.toArray(new WeightedRandomChestContent[li.size()]);
	}

	private class ReflectiveDefault extends Default {

		private ReflectiveDefault() throws Exception {
			super(ReflectiveLootTable.this.getLoot(), 0, 0);
		}

		@Override
		protected void restore(ChestGenHooks cgh) throws Exception {
			ReflectiveLootTable.this.setLoot(items);
		}

	}

}
