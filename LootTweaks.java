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
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.Auxiliary.Trackers.CommandableUpdateChecker;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Base.DragonAPIMod.LoadProfiler.LoadPhase;
import Reika.DragonAPI.Exception.RegistrationException;
import Reika.DragonAPI.Instantiable.Event.ConfigReloadEvent;
import Reika.DragonAPI.Instantiable.Event.Client.SinglePlayerLogoutEvent;
import Reika.DragonAPI.Instantiable.IO.ModLogger;
import Reika.DragonAPI.Instantiable.IO.SimpleConfig;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


@Mod( modid = "LootTweaks", name="LootTweaks", version = "v@MAJOR_VERSION@@MINOR_VERSION@", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI")
public class LootTweaks extends DragonAPIMod {

	@Instance("LootTweaks")
	public static LootTweaks instance = new LootTweaks();

	public static ModLogger logger;

	private static final SimpleConfig config = new SimpleConfig(instance);

	private static File parentFolder;

	public static File getDataFolder() {
		return parentFolder;
	}

	@Override
	@EventHandler
	public void preload(FMLPreInitializationEvent evt) {
		this.startTiming(LoadPhase.PRELOAD);
		this.verifyInstallation();

		logger = new ModLogger(instance, false);
		if (DragonOptions.FILELOG.getState())
			logger.setOutput("**_Loading_Log.log");

		config.loadSubfolderedConfigFile(evt);
		config.loadDataFromFile(evt);
		config.finishReading();

		FMLCommonHandler.instance().bus().register(this);
		this.basicSetup(evt);
		this.finishTiming();
	}

	private void createFiles() throws Exception {
		parentFolder = new File(config.getConfigFolder(), "LootTweaksChanges");
		if (!parentFolder.exists())
			parentFolder.mkdirs();

		Collection<String> c = LootTable.getValidTables();
		for (String s : c) {
			File f = new File(parentFolder, s+".tweaks");
			if (!f.exists()) {
				f.createNewFile();
			}
			LootTweaks.logger.log("Constructing loot table "+s+" from file "+f.getAbsolutePath());
			LootTable lt = LootTable.construct(s, f);
			LootTweaks.logger.log("Constructed loot table: "+lt);
			lt.load(f);
		}

		LootTier.folder = new File(parentFolder, "Tiers");
		if (!LootTier.folder.exists())
			LootTier.folder.mkdir();

		BatchChange.folder = new File(parentFolder, "Batch");
		if (!BatchChange.folder.exists())
			BatchChange.folder.mkdir();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);

		if (!ReikaObfuscationHelper.isDeObfEnvironment() && DragonAPICore.isOnActualServer()) {
			//PlayerHandler.instance.registerTracker(new LootSyncer());
		}

		this.finishTiming();
	}

	@Override
	@EventHandler
	public void postload(FMLPostInitializationEvent evt) {
		this.startTiming(LoadPhase.POSTLOAD);

		try {
			this.createFiles();
		}
		catch (IOException e) {
			throw new RuntimeException("Error creating loot files!", e);
		}
		catch (Exception e) {
			throw new RegistrationException(this, "Error loading loot tables!", e);
		}

		this.finishTiming();
	}

	@EventHandler
	public void applyData(FMLServerAboutToStartEvent evt) {
		this.applyLootChanges();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void applyDataForClient(ClientConnectedToServerEvent evt) { //fired on client when logging into a server
		if (!evt.isLocal)
			this.applyLootChanges();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void undoServerChanges(ClientDisconnectionFromServerEvent evt) { //fired on client when logging out of a server
		if (!evt.manager.isLocalChannel())
			this.undoLootChanges();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void undoServerChanges(SinglePlayerLogoutEvent evt) {
		this.undoLootChanges();
	}

	private void applyLootChanges() {
		try {
			//if (allowReload()) {
			LootTable.cacheDefaults();
			//}
			LootTier.loadTierFiles();
			BatchChange.loadBatchFiles();
			LootTable.applyAll();
		}
		catch (Exception e) {
			throw new RuntimeException("Errored while applying loot changes.", e);
		}
	}

	private void undoLootChanges() {
		//if (allowReload()) {
		try {
			LootTable.restoreDefaults();
		}
		catch (Exception e) {
			throw new RuntimeException("Errored while reverting loot changes.", e);
		}
		//}
	}

	@EventHandler
	public void applyData(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new LootReloadCommand());
		evt.registerServerCommand(new LootDumpCommand());
	}

	public static boolean allowReload() {
		return config.getBoolean("Control Setup", "Allow Dynamic Reload", true);
	}

	@SubscribeEvent
	public void reloadConfig(ConfigReloadEvent d) {

	}

	@Override
	public String getDisplayName() {
		return "LootTweaks";
	}

	@Override
	public String getModAuthorName() {
		return "Reika";
	}

	@Override
	public URL getDocumentationSite() {
		return DragonAPICore.getReikaForumPage();
	}

	@Override
	public String getWiki() {
		return null;
	}

	@Override
	public String getUpdateCheckURL() {
		return CommandableUpdateChecker.reikaURL;
	}

	@Override
	public ModLogger getModLogger() {
		return logger;
	}

	@Override
	public File getConfigFolder() {
		return config.getConfigFolder();
	}

}
