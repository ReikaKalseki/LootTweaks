package Reika.LootTweaks.ModInterface;

import java.io.File;

import Reika.DragonAPI.ModList;

public class ThaumTowerLootTable extends ReflectiveLootTable {

	private static final String THAUM_TOWER_KEY = "thaumVillageTower";

	protected ThaumTowerLootTable(File f) {
		super(ModList.THAUMCRAFT, "thaumcraft.common.lib.world.ComponentWizardTower", "towerChestContents", THAUM_TOWER_KEY, f);
	}

}
