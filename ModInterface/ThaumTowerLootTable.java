package Reika.LootTweaks.ModInterface;

import java.io.File;
import java.io.IOException;

import Reika.DragonAPI.ModList;

@Deprecated
public class ThaumTowerLootTable extends ReflectiveLootTable {

	private static final String THAUM_TOWER_KEY = "thaumVillageTower";

	protected ThaumTowerLootTable(File f) throws IOException {
		super(ModList.THAUMCRAFT, "thaumcraft.common.lib.world.ComponentWizardTower", "towerChestContents", THAUM_TOWER_KEY, f);
	}

}
