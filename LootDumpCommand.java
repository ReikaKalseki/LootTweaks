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

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import Reika.DragonAPI.Command.DragonCommandBase;


public class LootDumpCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		File f = new File(LootTweaks.getDataFolder(), "Dump");
		LootTable.dumpTables(f);
		this.sendChatToSender(ics, EnumChatFormatting.GREEN+"Loot tables dumped to "+f.getAbsolutePath());
	}

	@Override
	public String getCommandString() {
		return "dumploot";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
