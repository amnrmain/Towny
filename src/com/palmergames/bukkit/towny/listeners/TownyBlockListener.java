package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.flagwar.TownyWar;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

public class TownyBlockListener implements Listener {

	private final Towny plugin;

	public TownyBlockListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getBlock();

		//Get build permissions (updates cache if none exist)
		boolean bDestroy = PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.DESTROY);
		
		// Allow destroy if we are permitted
		if (bDestroy)
			return;

		/*
		 * Fetch the players cache
		 */
		PlayerCache cache = plugin.getCache(player);
		
		
		/*
		 * Allows War Event to piggy back off of Flag War editable materials, while accounting for neutral nations.
		 */
		boolean playerNeutral = false;
		if (TownyAPI.getInstance().isWarTime()) {
			try {
				Resident resident = TownyUniverse.getInstance().getDatabase().getResident(player.getName());
				if (resident.isJailed())
					playerNeutral = true;				
				if (resident.hasTown())
					if (!War.isWarringTown(resident.getTown())) {						
						playerNeutral = true;
					}
			} catch (NotRegisteredException e) {
			}
			
		}	
		
		/*
		 * Allow destroy in a WarZone (FlagWar) if it's an editable material.
		 * Event War piggy backing on flag war's EditableMaterialInWarZone 
		 */
		try {
			if (cache.getStatus() == TownBlockStatus.WARZONE || (TownyAPI.getInstance().isWarTime() && cache.getStatus() == TownBlockStatus.ENEMY && !playerNeutral && War.isWarringTown(cache.getLastTownBlock().getTownBlock().getTown()))) {
				if (!TownyWarConfig.isEditableMaterialInWarZone(block.getType())) {				
					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_warzone_cannot_edit_material"), "destroy", block.getType().toString().toLowerCase()));
				}
				return;
			}
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		event.setCancelled(true);
		

		/* 
		 * display any error recorded for this plot
		 */
		if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
			TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			TownyWorld world = townyUniverse.getDatabase().getWorld(block.getWorld().getName());
			worldCoord = new WorldCoord(world.getName(), Coord.parseCoord(block));

			//Get build permissions (updates if none exist)
			boolean bBuild = PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.BUILD);

			// Allow build if we are permitted
			if (bBuild)
				return;
			
			/*
			 * Fetch the players cache
			 */
			PlayerCache cache = plugin.getCache(player);
			TownBlockStatus status = cache.getStatus();

			/*
			 * Allows War Event to piggy back off of Flag War editable materials, while accounting for neutral nations.
			 */
			boolean playerNeutral = false;
			if (TownyAPI.getInstance().isWarTime()) {
				try {
					Resident resident = townyUniverse.getDatabase().getResident(player.getName());
					if (resident.isJailed())
						playerNeutral = true;	
					if (resident.hasTown())
						if (!War.isWarringTown(resident.getTown()))
							playerNeutral = true;
				} catch (NotRegisteredException e) {
				}
				
			}	
			
			/*
			 * Flag war
			 */
			if (((status == TownBlockStatus.ENEMY) && TownyWarConfig.isAllowingAttacks()) && (event.getBlock().getType() == TownyWarConfig.getFlagBaseMaterial())) {

				try {
					if (TownyWar.callAttackCellEvent(plugin, player, block, worldCoord))
						return;
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

				event.setBuild(false);
				event.setCancelled(true);

			// Event War piggy backing on flag war's EditableMaterialInWarZone 
			} else if (status == TownBlockStatus.WARZONE || (TownyAPI.getInstance().isWarTime() && cache.getStatus() == TownBlockStatus.ENEMY && !playerNeutral && War.isWarringTown(cache.getLastTownBlock().getTownBlock().getTown()))) {
				if (!TownyWarConfig.isEditableMaterialInWarZone(block.getType())) {
					event.setBuild(false);
					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_warzone_cannot_edit_material"), "build", block.getType().toString().toLowerCase()));
				}
				return;
			} else {
				event.setBuild(false);
				event.setCancelled(true);
			}

			/* 
			 * display any error recorded for this plot
			 */
			if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
				TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
		}

	}

	// prevent blocks igniting if within a protected town area when fire spread is set to off.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (onBurn(event.getBlock()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockIgnite(BlockIgniteEvent event) {

		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (onBurn(event.getBlock()))
			event.setCancelled(true);

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		List<Block> blocks = event.getBlocks();
		if (testBlockMove(event.getBlock(), event.getDirection(), true))
			event.setCancelled(true);

		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection(), false))
					event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (testBlockMove(event.getBlock(), event.getDirection(), false))
			event.setCancelled(true);
		
		List<Block> blocks = event.getBlocks();

		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection(), false))
					event.setCancelled(true);
			}
		}
	}

	/**
	 * testBlockMove
	 * 
	 * @param block - block that is being moved, or if pistonBlock is true the piston itself
	 * @param direction - direction the blocks are going
	 * @param pistonBlock - test is slightly different when the piston block itself is being checked.	 * 
	 */
	private boolean testBlockMove(Block block, BlockFace direction, boolean pistonBlock) {

		Block blockTo = null;
		if (!pistonBlock)
			blockTo = block.getRelative(direction);
		else {
			blockTo = block.getRelative(direction.getOppositeFace());
		}
		Location loc = block.getLocation();
		Location locTo = blockTo.getLocation();
		Coord coord = Coord.parseCoord(loc);
		Coord coordTo = Coord.parseCoord(locTo);

		TownyWorld townyWorld = null;
		TownBlock currentTownBlock = null, destinationTownBlock = null;

		try {
			townyWorld = TownyUniverse.getInstance().getDatabase().getWorld(loc.getWorld().getName());
			currentTownBlock = townyWorld.getTownBlock(coord);
		} catch (NotRegisteredException e) {
		}

		try {
			destinationTownBlock = townyWorld.getTownBlock(coordTo);
		} catch (NotRegisteredException e1) {
		}

		if (currentTownBlock != destinationTownBlock) {
			
			// Cancel if either is not null, but other is (wild to town).
			if (((currentTownBlock == null) && (destinationTownBlock != null)) || ((currentTownBlock != null) && (destinationTownBlock == null))) {
				return true;
			}

			// If both blocks are owned by the town.
			if (!currentTownBlock.hasResident() && !destinationTownBlock.hasResident()) {
				return false;
			}

			try {
				if ((!currentTownBlock.hasResident() && destinationTownBlock.hasResident()) || (currentTownBlock.hasResident() && !destinationTownBlock.hasResident()) || (currentTownBlock.getResident() != destinationTownBlock.getResident())

				|| (currentTownBlock.getPlotPrice() != -1) || (destinationTownBlock.getPlotPrice() != -1)) {
					return true;
				}
			} catch (NotRegisteredException e) {
				// Failed to fetch a resident
				return true;
			}
		}

		return false;
	}

	private boolean onBurn(Block block) {

		Location loc = block.getLocation();
		Coord coord = Coord.parseCoord(loc);
		TownyWorld townyWorld;

		try {
			townyWorld = TownyUniverse.getInstance().getDatabase().getWorld(loc.getWorld().getName());

			if (!townyWorld.isUsingTowny())
				return false;
			
			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(loc);
			
		
			// Give the wilderness a pass on portal ignition, like we do in towns when fire is disabled.
			if ((block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) && ((townBlock == null && !townyWorld.isForceFire() && !townyWorld.isFire()))) {
				TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within " + coord.toString() + ".");
				return true;
			}

			try {

				//TownBlock townBlock = townyWorld.getTownBlock(coord);
				
				boolean inWarringTown = false;
				if (TownyAPI.getInstance().isWarTime()) {
					if (townyWorld.hasTownBlock(coord))
						if (War.isWarringTown(townBlock.getTown()))
							inWarringTown = true;
				}
				/*
				 * Event War piggybacking off of Flag War's fire control setting.
				 */
				if (townyWorld.isWarZone(coord) || TownyAPI.getInstance().isWarTime() && inWarringTown) {
					if (TownyWarConfig.isAllowingFireInWarZone()) {
						return false;
					} else {
						TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within " + coord.toString() + ".");
						return true;
					}
				}
				if (townBlock != null)
					// Give a pass to Obsidian for portal lighting and Netherrack for fire decoration.
					if (((block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) || (block.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK)) && ((!townBlock.getTown().isFire() && !townyWorld.isForceFire() && !townBlock.getPermissions().fire) || (TownyAPI.getInstance().isWarTime() && TownySettings.isAllowWarBlockGriefing() && !townBlock.getTown().hasNation()))) {
						TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within " + coord.toString() + ".");
						return true;
					}
			} catch (TownyException x) {
				// Not a town so check the world setting for fire
				if (!townyWorld.isFire()) {
					TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within " + coord.toString() + ".");
					return true;
				}
			}

		} catch (NotRegisteredException e) {
			// Failed to fetch the world
		}

		return false;
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreateExplosion(BlockExplodeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		TownyWorld townyWorld;
		List<Block> blocks = event.blockList();
		int count = 0;
		
		try {
			townyWorld = TownyUniverse.getInstance().getDatabase().getWorld(event.getBlock().getLocation().getWorld().getName());
		} catch (NotRegisteredException e) {
			e.printStackTrace();
			return;
		}
		for (Block block : blocks) {
			count++;
			
			if (!locationCanExplode(townyWorld, block.getLocation())) {
				event.setCancelled(true);
				return;
			}
			
			if (TownyAPI.getInstance().isWilderness(block.getLocation())) {
				if (townyWorld.isUsingTowny()) {
					if (townyWorld.isExpl()) {
						if (townyWorld.isUsingPlotManagementWildRevert()) {
							//TownyMessaging.sendDebugMsg("onCreateExplosion: Testing block: " + entity.getType().getEntityClass().getSimpleName().toLowerCase() + " @ " + coord.toString() + ".");
							if ((!TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(block.getLocation()))) && (block.getType() != Material.TNT)) {
								ProtectionRegenTask task = new ProtectionRegenTask(plugin, block);
								task.setTaskId(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ((TownySettings.getPlotManagementWildRegenDelay() + count) * 20)));
								TownyRegenAPI.addProtectionRegenTask(task);
								event.setYield((float) 0.0);
								block.getDrops().clear();
							}
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Test if this location has explosions enabled.
	 * 
	 * @param world
	 * @param target
	 * @return true if allowed.
	 */
	public boolean locationCanExplode(TownyWorld world, Location target) {

		Coord coord = Coord.parseCoord(target);

		if (world.isWarZone(coord) && !TownyWarConfig.isAllowingExplosionsInWarZone()) {
			return false;
		}
		TownBlock townBlock = null;
		boolean isNeutral = false;
		try {
			townBlock = world.getTownBlock(coord);
			if (townBlock.hasTown())
				if (!War.isWarZone(townBlock.getWorldCoord()))
					isNeutral = true;
		} catch (NotRegisteredException e1) {
			if (TownyAPI.getInstance().isWilderness(target.getBlock().getLocation())) {
				isNeutral = !world.isExpl();
				if (!world.isExpl() && !TownyAPI.getInstance().isWarTime())
					return false;				
				if (world.isExpl() && !TownyAPI.getInstance().isWarTime())
					return true;	
			}
		}
		
		try {			
			if (world.isUsingTowny() && !world.isForceExpl()) {
				if (TownyAPI.getInstance().isWarTime() && TownyWarConfig.explosionsBreakBlocksInWarZone() && !isNeutral){
					return true;				
				}
				if ((!townBlock.getPermissions().explosion) || (TownyAPI.getInstance().isWarTime() && TownyWarConfig.isAllowingExplosionsInWarZone() && !townBlock.getTown().hasNation() && !townBlock.getTown().isBANG()))
					return false;
			}
		} catch (NotRegisteredException e) {
			return world.isExpl();
		}
		return true;
	}

}