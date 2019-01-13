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

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

public class NEILootConfig implements IConfigureNEI {

	private static final DungeonLootHandler loot = new DungeonLootHandler();

	@Override
	public void loadConfig() {
		LootTweaks.logger.log("Loading NEI Compatibility!");
		API.registerRecipeHandler(loot);
	}

	@Override
	public String getName() {
		return "LootTweaks NEI Handlers";
	}

	@Override
	public String getVersion() {
		return "-";
	}

}
