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

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import Reika.DragonAPI.Command.DragonCommandBase;


public class LootReloadCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		if (LootTweaks.allowReload()) {
			try {
				LootTier.reload();
				BatchChange.reload();
				LootTable.reload();
				this.sendChatToSender(ics, EnumChatFormatting.GREEN+"Loot tables successfully reloaded.");
			}
			catch (Exception e) {
				this.sendChatToSender(ics, EnumChatFormatting.RED+"Error reloading loot tables!");
				e.printStackTrace();
			}
		}
		else {
			this.sendChatToSender(ics, EnumChatFormatting.RED+"You cannot reload the loot tables; dynamic reloading is not enabled!");
		}
	}

	@Override
	public String getCommandString() {
		return "reloadloot";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
