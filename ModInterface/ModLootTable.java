package Reika.LootTweaks.ModInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import net.minecraft.util.WeightedRandomChestContent;

import Reika.DragonAPI.Interfaces.Registry.ModEntry;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.LootTweaks.LootChange;
import Reika.LootTweaks.LootTable;


public abstract class ModLootTable extends LootTable {

	public final ModEntry source;

	private static final HashMap<String, ModLootTableEntry> list = new HashMap();
	private static boolean wasInitialized = false;

	protected ModLootTable(ModEntry mod, String s, File ref) throws IOException {
		super(s, ref);
		source = mod;
		if (mod.isLoaded())
			list.put(s, new ModLootTableEntry(s, this.getClass(), mod));
	}

	public abstract Default createDefault() throws Exception;
	public abstract WeightedRandomChestContent[] getItems();

	@Override
	protected final void doApplyChanges(Field min, Field max) throws Exception {
		ArrayList<WeightedRandomChestContent> li = ReikaJavaLibrary.makeListFromArray(this.getItems());
		for (LootChange c : changes) {
			c.apply(null, li, min, max);
		}
		this.setLoot(li);
	}

	protected abstract void setLoot(ArrayList<WeightedRandomChestContent> li);

	public static class ModLootTableEntry {

		public final String key;
		public final ModEntry mod;
		private final Class refClass;

		ModLootTableEntry(String s, Class<? extends ModLootTable> c, ModEntry mod) {
			key = s;
			this.mod = mod;
			refClass = c;
		}

		protected ModLootTable constructLootTable(File f) throws Exception {
			Constructor<ModLootTable> ctr = refClass.getDeclaredConstructor(File.class);
			ctr.setAccessible(true);
			return ctr.newInstance(f);
		}

		@Override
		public final String toString() {
			return key;
		}

	}

	public static Set<String> getModTables() {
		return Collections.unmodifiableSet(list.keySet());
	}

	public static boolean isModTable(String s) {
		return list.containsKey(s);
	}

	public static LootTable construct(String s, File f) throws IOException {
		try {
			return list.get(s).constructLootTable(f);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException("Could not create handler for loot '"+s+"'!", e);
		}
	}

	public static void initializeModTables() {
		if (wasInitialized)
			return;
		wasInitialized = true;
		try {
			for (Class c : ReikaJavaLibrary.getAllClassesFromPackage("Reika.LootTweaks.ModInterface", null, false, false)) {
				try {
					Method init = c.getDeclaredMethod("getInit");
					Collection<ModLootTableEntry> li = (Collection<ModLootTableEntry>)init.invoke(null);
					for (ModLootTableEntry e : li) {
						if (e.mod.isLoaded()) {
							list.put(e.key, e);
						}
					}
					continue;
				}
				catch (NoSuchMethodException e) {

				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}

				if (ModLootTable.class.isAssignableFrom(c)) {
					if ((c.getModifiers() & Modifier.ABSTRACT) != 0 || c.isAnnotationPresent(Deprecated.class))
						continue;
					try {
						Constructor ctr = c.getDeclaredConstructor(File.class);
						ctr.setAccessible(true);
						ctr.newInstance(new Object[]{null});
					}
					catch (Exception e) {
						throw new RuntimeException("Could not initialize handler for loot '"+c+"'!", e);
					}
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Could not find mod loot handlers!", e);
		}
	}

}
