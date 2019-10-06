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

import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;


public class BatchChange {

	public static File folder;

	private static final HashMap<String, BatchChange> batches = new HashMap();

	public final String name;
	private final ArrayList<LootChange> changes = new ArrayList();

	private BatchChange(String s) {
		name = s;
	}

	public static BatchChange getBatch(String id) {
		return batches.get(id);
	}

	public static void reload() throws Exception {
		batches.clear();
		loadBatchFiles();
	}

	public static void loadBatchFiles() throws Exception {
		for (File f : ReikaFileReader.getAllFilesInFolder(folder, ".batchchange")) {
			loadFile(f);
		}
	}

	private static void loadFile(File f) throws Exception {
		LootTable.parser.clear();
		LootTable.parser.load(f);
		if (LootTable.parser.getEntries().isEmpty())
			return;
		String s = f.getName();
		s = s.substring(0, s.indexOf('.'));
		BatchChange bc = new BatchChange(s);
		batches.put(s, bc);
		for (LuaBlock lb : LootTable.parser.getEntries()) {
			bc.changes.add(new LootChange(lb));
		}
		LootTweaks.logger.log("Parsed batch '"+bc.name+"' with "+bc.changes.size()+" changes: "+bc.changes);
		LootTable.parser.clear();
	}

	public Collection<LootChange> getChanges() {
		return Collections.unmodifiableCollection(changes);
	}

}
