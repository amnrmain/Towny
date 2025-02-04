package com.palmergames.bukkit.towny.command;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.google.common.collect.ListMultimap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.ConfirmationType;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownSpawnLevel;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.naming.InvalidNameException;
import java.io.InvalidObjectException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Send a list of all town help commands to player Command: /town
 */

public class TownCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> output = new ArrayList<>();

	static {
		output.add(ChatTools.formatTitle("/town"));
		output.add(ChatTools.formatCommand("", "/town", "", TownySettings.getLangString("town_help_1")));
		output.add(ChatTools.formatCommand("", "/town", "[town]", TownySettings.getLangString("town_help_3")));
		output.add(ChatTools.formatCommand("", "/town", "new [name]", TownySettings.getLangString("town_help_11")));
		output.add(ChatTools.formatCommand("", "/town", "here", TownySettings.getLangString("town_help_4")));
		output.add(ChatTools.formatCommand("", "/town", "list", ""));
		output.add(ChatTools.formatCommand("", "/town", "online", TownySettings.getLangString("town_help_10")));
		output.add(ChatTools.formatCommand("", "/town", "leave", ""));
		output.add(ChatTools.formatCommand("", "/town", "reslist", ""));
		output.add(ChatTools.formatCommand("", "/town", "ranklist", ""));
		output.add(ChatTools.formatCommand("", "/town", "outlawlist", ""));
		output.add(ChatTools.formatCommand("", "/town", "plots", ""));
		output.add(ChatTools.formatCommand("", "/town", "outlaw add/remove [name]", ""));
		output.add(ChatTools.formatCommand("", "/town", "say", "[message]"));
		output.add(ChatTools.formatCommand("", "/town", "spawn", TownySettings.getLangString("town_help_5")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/town", "deposit [$]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/town", "rank add/remove [resident] [rank]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "mayor ?", TownySettings.getLangString("town_help_8")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "new [town] " + TownySettings.getLangString("town_help_2"), TownySettings.getLangString("town_help_7")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "delete [town]", ""));
	}

	public TownCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			parseTownCommand(player, args);
		} else
			try {
				parseTownCommandForConsole(sender, args);
			} catch (TownyException ignored) {
			}

		return true;
	}

	@SuppressWarnings("static-access")
	private void parseTownCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
					
				for (String line : output)
					sender.sendMessage(line);
				
		} else if (split[0].equalsIgnoreCase("list")) {

			listTowns(sender, split);

		} else {
			try {
				final Town town = TownyUniverse.getInstance().getDatabase().getTown(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(town)));
			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		}

	}

	@SuppressWarnings("static-access")
	private void parseTownCommand(final Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {

			if (split.length == 0) {
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
					try {
						Resident resident = townyUniverse.getDatabase().getResident(player.getName());
						Town town = resident.getTown();

						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
					} catch (NotRegisteredException x) {
						try {
							throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player,e.getMessage()); // Exceptions written from this runnable, are not reached by the catch at the end.
						}
					}
				});
			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

				for (String line : output)
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("here")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				showTownStatusHere(player);

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listTowns(player, split);

			} else if (split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 1) {
					throw new TownyException(TownySettings.getLangString("msg_specify_name"));
				} else if (split.length >= 2) {
					String[] newSplit = StringMgmt.remFirstArg(split);
					String townName = String.join("_", newSplit);					
					newTown(player, townName, player.getName(), false);			
				}

			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				townLeave(player);

			} else if (split[0].equalsIgnoreCase("withdraw")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				
				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Town tbTown = tb.getTown(); 
					Town pTown = townyUniverse.getDatabase().getResident(player.getName()).getTown();
					if (tbTown != pTown)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));						
				}
				
				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_your_town"));					
					Coord coord = Coord.parseCoord(plugin.getCache(player).getLastLocation());
					Town town = townyUniverse.getDatabase().getWorld(player.getLocation().getWorld().getName()).getTownBlock(coord).getTown();
					if (!townyUniverse.getDatabase().getResident(player.getName()).getTown().equals(town))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_your_town"));
				}

				if (split.length == 2)
					try {
						townWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town withdraw"));

			} else if (split[0].equalsIgnoreCase("deposit")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				
				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					}
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Town tbTown = tb.getTown(); 
					Town pTown = townyUniverse.getDatabase().getResident(player.getName()).getTown();
					if (tbTown != pTown)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
				}
				
				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_your_town"));
					}
					Coord coord = Coord.parseCoord(plugin.getCache(player).getLastLocation());
					Town town = townyUniverse.getDatabase().getWorld(player.getLocation().getWorld().getName()).getTownBlock(coord).getTown();
					if (!townyUniverse.getDatabase().getResident(player.getName()).getTown().equals(town))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_your_town"));
				}

				if (split.length == 2)
					try {
						townDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town deposit"));
			} else if (split[0].equalsIgnoreCase("plots")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				Town town = null;
				try {


					if (split.length == 1) {
						town = townyUniverse.getDatabase().getResident(player.getName()).getTown();
					} else {
						town = townyUniverse.getDatabase().getTown(split[1]);
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));
					return;
				}

				townPlots(player, town);

			} else {
				String[] newSplit = StringMgmt.remFirstArg(split);

				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * perm tests performed in method.
					 */
					townRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("set")) {

					/*
					 * perm test performed in method.
					 */
					townSet(player, newSplit);

				} else if (split[0].equalsIgnoreCase("buy")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townBuy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					townToggle(player, newSplit);

				} else if (split[0].equalsIgnoreCase("mayor")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townMayor(player, newSplit);

				} else if (split[0].equalsIgnoreCase("spawn")) {

					/*
					 * town spawn handles it's own perms.
					 */
					townSpawn(player, newSplit, false);

				} else if (split[0].equalsIgnoreCase("outpost")) {
					if (split.length >= 2) {
						if (split[1].equalsIgnoreCase("list")) {
							if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTPOST_LIST.getNode())){
								throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
							}
							Resident resident = townyUniverse.getDatabase().getResident(player.getName());
							if (resident.hasTown()){
								Town town = resident.getTown();
								List<Location> outposts = town.getAllOutpostSpawns();
								int page = 1;
								int total = (int) Math.ceil(((double) outposts.size()) / ((double) 10));
								if (split.length == 3){
									try {
										page = Integer.parseInt(split[2]);
										if (page < 0) {
											TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative"));
											return;
										} else if (page == 0) {
											TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
											return;
										}
									} catch (NumberFormatException e) {
										TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
										return;
									}
								}
								if (page > total) {
									TownyMessaging.sendErrorMsg(player, TownySettings.getListNotEnoughPagesMsg(total));
									return;
								}
								int iMax = page * 10;
								if ((page * 10) > outposts.size()) {
									iMax = outposts.size();
								}
								@SuppressWarnings({ "unchecked", "rawtypes" })
								List<String> outputs = new ArrayList();
								for (int i = (page - 1) * 10; i < iMax; i++) {
									Location outpost = outposts.get(i);
									String output;
									TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
									if (!tb.getName().equalsIgnoreCase("")) {
										output = Colors.Gold + (i + 1) + Colors.Gray + " - " + Colors.LightGreen  + tb.getName() +  Colors.Gray + " - " + Colors.LightBlue + outpost.getWorld().getName() +  Colors.Gray + " - " + Colors.LightBlue + "(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")";
									} else {
										output = Colors.Gold + (i + 1) + Colors.Gray + " - " + Colors.LightBlue + outpost.getWorld().getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")";
									}
									outputs.add(output);
								}
								player.sendMessage(
										ChatTools.formatList(
												TownySettings.getLangString("outpost_plu"),
												Colors.Gold + "#" + Colors.Gray + " - " + Colors.LightGreen + "(Plot Name)" + Colors.Gray + " - " + Colors.LightBlue + "(Outpost World)"+ Colors.Gray + " - " + Colors.LightBlue + "(Outpost Location)",
												outputs,
												TownySettings.getListPageMsg(page, total)
										));

							} else {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_must_belong_town"));
							}
						} else {
							townSpawn(player, newSplit, true);
						}
					} else {
						townSpawn(player, newSplit, true);
					}
				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("reslist")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					Town town = null;
					try {
						if (split.length == 1) {
							town = townyUniverse.getDatabase().getResident(player.getName()).getTown();
						} else {
							town = townyUniverse.getDatabase().getTown(split[1]);
						}
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));
						return;
					}
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedResidents(town));

				} else if (split[0].equalsIgnoreCase("ranklist")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {
						Resident resident = townyUniverse.getDatabase().getResident(player.getName());
						Town town = resident.getTown();
						TownyMessaging.sendMessage(player, TownyFormatter.getRanks(town));
					} catch (NotRegisteredException x) {
						throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));
					}

				} else if (split[0].equalsIgnoreCase("outlawlist")) {

					Town town;
					try {
						if (split.length == 1)
							town = townyUniverse.getDatabase().getResident(player.getName()).getTown();
						else
							town = townyUniverse.getDatabase().getTown(split[1]);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));
						return;
					}
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOutlaws(town));

				} else if (split[0].equalsIgnoreCase("join")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownJoin(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townAdd(player, null, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {// He does have permission to manage Real invite Permissions. (Mayor or even assisstant)
					parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("claim")) {

					parseTownClaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownUnclaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SAY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {
						Town town = townyUniverse.getDatabase().getResident(player.getName()).getTown();
						StringBuilder builder = new StringBuilder();
						for (String s : newSplit) {
							builder.append(s + " ");
						}
						String message = builder.toString();
						TownyMessaging.sendPrefixedTownMessage(town, message);
					} catch (Exception ignored) {
					}
					
				} else if (split[0].equalsIgnoreCase("outlaw")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTLAW.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownOutlawCommand(player, newSplit);

				} else {
					try {
						final Town town = townyUniverse.getDatabase().getTown(split[0]);
						Resident resident = townyUniverse.getDatabase().getResident(player.getName());
						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OTHERTOWN.getNode()) && ( (resident.getTown() != town) || (!resident.hasTown()) ) ) {
							throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
						}
						Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town)));

					} catch (NotRegisteredException x) {
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
					}
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}
	private static final List<String> invite = new ArrayList<>();

	static {
		invite.add(ChatTools.formatTitle("/town invite"));
		invite.add(ChatTools.formatCommand("", "/town", "invite [player]", TownySettings.getLangString("town_invite_help_1")));
		invite.add(ChatTools.formatCommand("", "/town", "invite -[player]", TownySettings.getLangString("town_invite_help_2")));
		invite.add(ChatTools.formatCommand("", "/town", "invite sent", TownySettings.getLangString("town_invite_help_3")));
		invite.add(ChatTools.formatCommand("", "/town", "invite received", TownySettings.getLangString("town_invite_help_4")));
		invite.add(ChatTools.formatCommand("", "/town", "invite accept [nation]", TownySettings.getLangString("town_invite_help_5")));
		invite.add(ChatTools.formatCommand("", "/town", "invite deny [nation]", TownySettings.getLangString("town_invite_help_6")));
	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		// We know he has the main permission to manage this stuff. So Let's continue:

		Resident resident = townyUniverse.getDatabase().getResident(player.getName());

		String received = TownySettings.getLangString("town_received_invites")
				.replace("%a", Integer.toString(InviteHandler.getReceivedInvitesAmount(resident.getTown()))
				)
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown())));
		String sent = TownySettings.getLangString("town_sent_invites")
				.replace("%a", Integer.toString(InviteHandler.getSentInvitesAmount(resident.getTown()))
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown())));


		if (newSplit.length == 0) { // (/town invite)
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_SEE_HOME.getNode())) {
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			}
			String[] msgs;
			List<String> messages = new ArrayList<>();


			for (String msg : invite) {
				messages.add(Colors.strip(msg));
			}
			messages.add(sent);
			messages.add(received);
			msgs = messages.toArray(new String[0]);
			player.sendMessage(msgs);
			return;
		}
		if (newSplit.length >= 1) { // /town invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				for (String msg : invite) {
					player.sendMessage(Colors.strip(msg));
				}
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_SENT.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getSentInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("received")) { // /town invite received
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				List<Invite> receivedinvites = resident.getTown().getReceivedInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, receivedinvites, page, false);
				player.sendMessage(received);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("accept")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				// /town (gone)
				// invite (gone)
				// args[0] = accept = length = 1
				// args[1] = [Nation] = length = 2
				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					try {
						nation = townyUniverse.getDatabase().getNation(newSplit[1]);
					} catch (NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}
				ListMultimap<Nation, Town> nation2towns = InviteHandler.getNationtotowninvites();
				if (nation2towns.containsKey(nation)) {
					if (nation2towns.get(nation).contains(town)) {
						for (Invite invite : town.getReceivedInvites()) {
							if (invite.getSender().equals(nation)) {
								try {
									InviteHandler.acceptInvite(invite);
									return;
								} catch (InvalidObjectException e) {
									e.printStackTrace(); // Shouldn't happen, however like i said a fallback
								}
							}
						}
					}
				}
			}
			if (newSplit[0].equalsIgnoreCase("deny")) { // /town invite deny
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_DENY.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					try {
						nation = townyUniverse.getDatabase().getNation(newSplit[1]);
					} catch (NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}
				ListMultimap<Nation, Town> nation2towns = InviteHandler.getNationtotowninvites();
				if (nation2towns.containsKey(nation)) {
					if (nation2towns.get(nation).contains(town)) {
						for (Invite invite : town.getReceivedInvites()) {
							if (invite.getSender().equals(nation)) {
								try {
									InviteHandler.declineInvite(invite, false);
									TownyMessaging.sendMessage(player, TownySettings.getLangString("successful_deny"));
									return;
								} catch (InvalidObjectException e) {
									e.printStackTrace(); // Shouldn't happen, however like i said a fallback
								}
							}
						}
					}
				}
			} else {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				townAdd(player, null, newSplit);
				// It's none of those 4 subcommands, so it's a playername, I just expect it to be ok.
				// If it is invalid it is handled in townAdd() so, I'm good
			}
		}
	}

	private void parseTownOutlawCommand(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/town outlaw"));
			player.sendMessage(ChatTools.formatCommand("", "/town outlaw", "add/remove [name]", ""));

		} else {

			Resident resident, target;
			Town town = null;
			Town targetTown = null;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 2)
				throw new TownyException("Eg: /town outlaw add/remove [name]");

			try {
				resident = townyUniverse.getDatabase().getResident(player.getName());
				target = townyUniverse.getDatabase().getResident(split[1]);
				town = resident.getTown();
			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			if (split[0].equalsIgnoreCase("add")) {
				try {
					try {
						targetTown = target.getTown();
					} catch (Exception e1) {
					}
					// Don't allow a resident to outlaw their own mayor.
					if (resident.getTown().getMayor().equals(target))
						return;
					// Kick outlaws from town if they are residents.
					if (targetTown != null)
						if (targetTown == town){
							townRemoveResident(town, target);
							TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_kicked_by"), player.getName()));
							TownyMessaging.sendPrefixedTownMessage(town,String.format(TownySettings.getLangString("msg_kicked"), player.getName(), target.getName()));
						}
					town.addOutlaw(target);
					townyUniverse.getDatabase().saveTown(town);
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_declared_outlaw"), town.getName()));
					TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_you_have_declared_an_outlaw"), target.getName(), town.getName()));
				} catch (AlreadyRegisteredException e) {
					// Must already be an outlaw
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_err_resident_already_an_outlaw"));
					return;
				} catch (EmptyTownException e) {
					e.printStackTrace();
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				try {
					town.removeOutlaw(target);
					townyUniverse.getDatabase().saveTown(town);
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_undeclared_outlaw"), town.getName()));
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_you_have_undeclared_an_outlaw"), target.getName(), town.getName()));
				} catch (NotRegisteredException e) {
					// Must already not be an outlaw
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_err_player_not_an_outlaw"));
					return;
				}

			} else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
				return;
			}

			/*
			 * If we got here we have made a change Save the altered resident
			 * data.
			 */
			townyUniverse.getDatabase().saveTown(town);

		}

	}

	private void townPlots(Player player, Town town) {

		List<String> out = new ArrayList<>();

		int townOwned = 0;
		int resident = 0;
		int residentOwned = 0;
		int residentOwnedFS = 0;
		int embassy = 0;
		int embassyRO = 0;
		int embassyFS = 0;
		int shop = 0;
		int shopRO = 0;
		int shopFS = 0;
		int farm = 0;
		int arena = 0;
		int wilds = 0;
		int jail = 0;
		int inn = 0;
		for (TownBlock townBlock : town.getTownBlocks()) {

			if (townBlock.getType() == TownBlockType.EMBASSY) {
				embassy++;
				if (townBlock.hasResident())
					embassyRO++;
				if (townBlock.isForSale())
					embassyFS++;
			} else if (townBlock.getType() == TownBlockType.COMMERCIAL) {
				shop++;
				if (townBlock.hasResident())
					shopRO++;
				if (townBlock.isForSale())
					shopFS++;
			} else if (townBlock.getType() == TownBlockType.FARM) {
				farm++;
			} else if (townBlock.getType() == TownBlockType.ARENA) {
				arena++;
			} else if (townBlock.getType() == TownBlockType.WILDS) {
				wilds++;
			} else if (townBlock.getType() == TownBlockType.JAIL) {
				jail++;
			} else if (townBlock.getType() == TownBlockType.INN) {
				inn++;
			} else if (townBlock.getType() == TownBlockType.RESIDENTIAL) {
				resident++;
				if (townBlock.hasResident())
					residentOwned++;
				if (townBlock.isForSale())
					residentOwnedFS++;
			}
			if (!townBlock.hasResident()) {
				townOwned++;
			}
		}
		out.add(ChatTools.formatTitle(town + " Town Plots"));
		out.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownBlocks().size() + " / " + TownySettings.getMaxTownBlocks(town) + (TownySettings.isSellingBonusBlocks() ? Colors.LightBlue + " [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchedBlocks() + "]" : "") + (town.getBonusBlocks() > 0 ? Colors.LightBlue + " [Bonus: " + town.getBonusBlocks() + "]" : "") + ((TownySettings.getNationBonusBlocks(town) > 0) ? Colors.LightBlue + " [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]" : ""));
		out.add(Colors.Green + "Town Owned Land: " + Colors.LightGreen + townOwned);
		out.add(Colors.Green + "Farms   : " + Colors.LightGreen + farm);
		out.add(Colors.Green + "Arenas : " + Colors.LightGreen + arena);
		out.add(Colors.Green + "Wilds    : " + Colors.LightGreen + wilds);
		out.add(Colors.Green + "Jails    : " + Colors.LightGreen + jail);
		out.add(Colors.Green + "Inns    : " + Colors.LightGreen + inn);
		out.add(Colors.Green + "Type: " + Colors.LightGreen + "Player-Owned / ForSale / Total / Daily Revenue");
		out.add(Colors.Green + "Residential: " + Colors.LightGreen + residentOwned + " / " + residentOwnedFS + " / " + resident + " / " + (residentOwned * town.getPlotTax()));
		out.add(Colors.Green + "Embassies : " + Colors.LightGreen + embassyRO + " / " + embassyFS + " / " + embassy + " / " + (embassyRO * town.getEmbassyPlotTax()));
		out.add(Colors.Green + "Shops      : " + Colors.LightGreen + shopRO + " / " + shopFS + " / " + shop + " / " + (shop * town.getCommercialPlotTax()));
		out.add(String.format(TownySettings.getLangString("msg_town_plots_revenue_disclaimer")));
		TownyMessaging.sendMessage(player, out);

	}

	private void parseTownOnlineCommand(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length > 0) {
			try {
				Town town = townyUniverse.getDatabase().getTown(split[0]);
				List<Resident> onlineResidents = getOnlineResidentsViewable(player, town);
				if (onlineResidents.size() > 0) {
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_town_online"), town, player));
				} else {
					TownyMessaging.sendMessage(player, ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.White + "0 " + TownySettings.getLangString("res_list") + " " + (TownySettings.getLangString("msg_town_online") + ": " + town)));
				}

			} catch (NotRegisteredException e) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		} else {
			try {
				Resident resident = townyUniverse.getDatabase().getResident(player.getName());
				Town town = resident.getTown();
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_town_online"), town, player));
			} catch (NotRegisteredException x) {
				TownyMessaging.sendMessage(player, TownySettings.getLangString("msg_err_dont_belong_town"));
			}
		}
	}

	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 *
	 * @param sender
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void listTowns(CommandSender sender, String[] split) {

		List<Town> townsToSort = TownyUniverse.getInstance().getDatabase().getTowns();
		int page = 1;
		int total = (int) Math.ceil(((double) townsToSort.size()) / ((double) 10));
		if (split.length > 1) {
			try {
				page = Integer.parseInt(split[1]);
				if (page < 0) {
					TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_err_negative"));
					return;
				} else if (page == 0) {
					TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_must_be_int"));
				return;
			}
		}
		if (page > total) {
			TownyMessaging.sendErrorMsg(sender, TownySettings.getListNotEnoughPagesMsg(total));
			return;
		}

		townsToSort.sort((Comparator) (t1, t2) -> {
			if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents()) {
				return 0;
			}
			return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
		});
		int iMax = page * 10;
		if ((page * 10) > townsToSort.size()) {
			iMax = townsToSort.size();
		}
		List<String> townsformatted = new ArrayList();
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Town town = townsToSort.get(i);
			String output = Colors.Blue + town.getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + town.getNumResidents() + ")";
			if (town.isOpen())
				output += Colors.White + " (Open)";
			townsformatted.add(output);
		}
		sender.sendMessage(ChatTools.formatList(TownySettings.getLangString("town_plu"),
				Colors.Blue + "Town Name" + Colors.Gray + " - " + Colors.LightBlue + "(Number of Residents)",
				townsformatted, TownySettings.getListPageMsg(page, total)
				)
		);
	}

	public void townMayor(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showTownMayorHelp(player);
	}

	/**
	 * Send a the status of the town the player is physically at to him
	 *
	 * @param player
	 */
	public void showTownStatusHere(Player player) {

		try {
			TownyWorld world = TownyUniverse.getInstance().getDatabase().getWorld(player.getWorld().getName());
			Coord coord = Coord.parseCoord(player);
			showTownStatusAtCoord(player, world, coord);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	/**
	 * Send a the status of the town at the target coordinates to the player
	 *
	 * @param player
	 * @param world
	 * @param coord
	 * @throws TownyException
	 */
	public void showTownStatusAtCoord(Player player, TownyWorld world, Coord coord) throws TownyException {

		if (!world.hasTownBlock(coord))
			throw new TownyException(String.format(TownySettings.getLangString("msg_not_claimed"), coord));

		Town town = world.getTownBlock(coord).getTown();
		TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
	}

	public void showTownMayorHelp(Player player) {

		player.sendMessage(ChatTools.formatTitle("Town Mayor Help"));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "claim", "'/town claim ?' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "unclaim", "'/town " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "[add/kick] " + TownySettings.getLangString("res_2") + " .. []", TownySettings.getLangString("res_6")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "set [] .. []", "'/town set' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "plots", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "toggle", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "rank add/remove [resident] [rank]", "'/town rank ?' " + TownySettings.getLangString("res_5")));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town",
		// "wall [type] [height]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town",
		// "wall remove", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "delete", ""));
	}

	public void townToggle(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "public", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "mobs", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "taxpercent", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "open", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "jail [number] [resident]", ""));
		} else {
			Resident resident;
			Town town;
			try {
				resident = townyUniverse.getDatabase().getResident(player.getName());
				town = resident.getTown();

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("public")) {

				town.setPublic(!town.isPublic());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_public"), town.isPublic() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

			} else if (split[0].equalsIgnoreCase("pvp")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				boolean outsiderintown = false;
				if (TownySettings.getOutsidersPreventPVPToggle()) {
					for (Player target : Bukkit.getOnlinePlayers()) {
						Resident targetresident = townyUniverse.getDatabase().getResident(target.getName());
						Block block = target.getLocation().getBlock().getRelative(BlockFace.DOWN);
						if (!TownyAPI.getInstance().isWilderness(block.getLocation())) {
							WorldCoord coord = WorldCoord.parseWorldCoord(target.getLocation());
							for (TownBlock tb : town.getTownBlocks()) {
								if (coord.equals(tb.getWorldCoord()) && ((!(targetresident.hasTown())) || (!(targetresident.getTown().equals(town))))) {
									outsiderintown = true;
								}
							}
						}
					}
				}
				if (!outsiderintown) {
					town.setPVP(!town.isPVP());
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_pvp"), town.getName(), town.isPVP() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));
				} else if (outsiderintown) {
					throw new TownyException(TownySettings.getLangString("msg_cant_toggle_pvp_outsider_in_town"));
				}
			} else if (split[0].equalsIgnoreCase("explosion")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setBANG(!town.isBANG());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_expl"), town.getName(), town.isBANG() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

			} else if (split[0].equalsIgnoreCase("fire")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setFire(!town.isFire());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_fire"), town.getName(), town.isFire() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

			} else if (split[0].equalsIgnoreCase("mobs")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setHasMobs(!town.hasMobs());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_mobs"), town.getName(), town.hasMobs() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

			} else if (split[0].equalsIgnoreCase("taxpercent")) {
				town.setTaxPercentage(!town.isTaxPercentage());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_taxpercent"), town.isTaxPercentage() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));
			} else if (split[0].equalsIgnoreCase("open")) {

				town.setOpen(!town.isOpen());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_open"), town.isOpen() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

				// Send a warning when toggling on (a reminder about plot
				// permissions).
				if (town.isOpen())
					throw new TownyException(String.format(TownySettings.getLangString("msg_toggle_open_on_warning")));

			} else if (split[0].equalsIgnoreCase("jail")) {
				if (!town.hasJailSpawn())
					throw new TownyException(String.format(TownySettings.getLangString("msg_town_has_no_jails")));

				Integer index;
				if (split.length <= 2) {
					player.sendMessage(ChatTools.formatTitle("/town toggle jail"));
					player.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));

				} else if (split.length == 3) {
					try {
						Integer.parseInt(split[1]);
						index = Integer.valueOf(split[1]);
						Resident jailedresident = townyUniverse.getDatabase().getResident(split[2]);
						if (!player.hasPermission("towny.command.town.toggle.jail"))
							throw new TownyException(TownySettings.getLangString("msg_no_permission_to_jail_your_residents"));
						if (!jailedresident.hasTown())
							if (!jailedresident.isJailed())
								throw new TownyException(TownySettings.getLangString("msg_resident_not_part_of_any_town"));

						try {

							if (jailedresident.isJailed() && index != jailedresident.getJailSpawn())
								index = jailedresident.getJailSpawn();

							Player jailedplayer = TownyAPI.getInstance().getPlayer(jailedresident);
							if (jailedplayer == null) {
								throw new TownyException(String.format("%s is not online", jailedresident.getName()));
							}
							Town sendertown = resident.getTown();
							if (jailedplayer.getUniqueId().equals(player.getUniqueId()))
								throw new TownyException(TownySettings.getLangString("msg_no_self_jailing"));

							if (jailedresident.isJailed()) {
								Town jailTown = townyUniverse.getDatabase().getTown(jailedresident.getJailTown());
								if (jailTown != sendertown) {
									throw new TownyException(TownySettings.getLangString("msg_player_not_jailed_in_your_town"));
								} else {
									jailedresident.setJailedByMayor(jailedplayer, index, sendertown);
									return;

								}
							}

							if (jailedresident.getTown() != sendertown)
								throw new TownyException(TownySettings.getLangString("msg_resident_not_your_town"));

							jailedresident.setJailedByMayor(jailedplayer, index, sendertown);

						} catch (NotRegisteredException x) {
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
						}

					} catch (NumberFormatException e) {
						player.sendMessage(ChatTools.formatTitle("/town toggle jail"));
						player.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
						return;
					} catch (NullPointerException e) {
						e.printStackTrace();
						return;
					}
				}

			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
			}

			//Propagate perms to all unchanged, town owned, townblocks

			for (TownBlock townBlock : town.getTownBlocks()) {
				if (!townBlock.hasResident() && !townBlock.isChanged()) {
					townBlock.setType(townBlock.getType());
					townyUniverse.getDatabase().saveTownBlock(townBlock);
				}
			}

			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(town);
			Bukkit.getServer().getPluginManager().callEvent(event);
			
			townyUniverse.getDatabase().saveTown(town);
		}
	}

	private void toggleTest(Player player, Town town, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (town.getWorld().isForceTownMobs())
				throw new TownyException(TownySettings.getLangString("msg_world_mobs"));
		}

		if (split.contains("fire")) {
			if (town.getWorld().isForceFire())
				throw new TownyException(TownySettings.getLangString("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (town.getWorld().isForceExpl())
				throw new TownyException(TownySettings.getLangString("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (town.getWorld().isForcePVP())
				throw new TownyException(TownySettings.getLangString("msg_world_pvp"));
		}
	}

	public void townRank(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/town rank"));
			player.sendMessage(ChatTools.formatCommand("", "/town rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3)
				throw new TownyException("Eg: /town rank add/remove [resident] [rank]");

			try {
				resident = townyUniverse.getDatabase().getResident(player.getName());
				target = townyUniverse.getDatabase().getResident(split[1]);
				town = resident.getTown();

				if (town != target.getTown())
					throw new TownyException(TownySettings.getLangString("msg_resident_not_your_town"));

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			rank = split[2];
			/*
			 * Is this a known rank?
			 */
			if (!TownyPerms.getTownRanks().contains(rank))
				throw new TownyException(String.format(TownySettings.getLangString("msg_unknown_rank_available_ranks"), rank, StringMgmt.join(TownyPerms.getTownRanks(), ",")));

			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank.toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_no_permission_to_give_rank"));

			if (split[0].equalsIgnoreCase("add")) {
				try {
					if (target.addTownRank(split[2])) {
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_given_rank"), "Town", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_given_rank"), "Town", rank, target.getName()));
					} else {
						// Not in a town or Rank doesn't exist
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_resident_not_your_town"));
						return;
					}
				} catch (AlreadyRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_already_has_rank"), target.getName(), "Town"));
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				try {
					if (target.removeTownRank(split[2])) {
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_had_rank_taken"), "Town", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_taken_rank_from"), "Town", rank, target.getName()));
					}
				} catch (NotRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_doesnt_have_rank"), target.getName(), "Town"));
					return;
				}

			} else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
				return;
			}

			/*
			 * If we got here we have made a change Save the altered resident
			 * data.
			 */
			townyUniverse.getDatabase().saveResident(target);

		}

	}

	public void townSet(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town set"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor " + TownySettings.getLangString("town_help_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn/outpost/jail", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' " + TownySettings.getLangString("res_5")));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plottax/shoptax/embassytax] [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plotprice/shopprice/embassyprice] [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawncost [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "tag [upto 4 letters] or clear", ""));
		} else {
			Resident resident;
			Town town = null;
			Nation nation = null;
			TownyWorld oldWorld = null;

			try {
				resident = townyUniverse.getDatabase().getResident(player.getName());
				town = resident.getTown();

				if (town.hasNation())
					nation = town.getNation();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("board")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /town set board " + TownySettings.getLangString("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!NameValidation.isValidString(line)) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_string_board_not_set"));
						return;
					}

					town.setTownBoard(line);
					TownyMessaging.sendTownBoard(player, town);
				}

			} else {

				/*
				 * Test we have permission to use this command.
				 */
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("mayor")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
						return;
					} else
						try {
							if (!resident.isMayor())
								throw new TownyException(TownySettings.getLangString("msg_not_mayor"));

							String oldMayor = town.getMayor().getName();
							Resident newMayor = townyUniverse.getDatabase().getResident(split[1]);
							town.setMayor(newMayor);
							TownyPerms.assignPermissions(townyUniverse.getDatabase().getResident(oldMayor), null);
							plugin.deleteCache(oldMayor);
							plugin.deleteCache(newMayor.getName());
							TownyMessaging.sendTownMessage(town, TownySettings.getNewMayorMsg(newMayor.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
							return;
						}

				} else if (split[0].equalsIgnoreCase("taxes")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set taxes 7");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							if (town.isTaxPercentage() && amount > 100) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_percentage"));
								return;
							}
							if (TownySettings.getTownDefaultTaxMinimumTax() > amount) {
								TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_tax_minimum_not_met"), TownySettings.getTownDefaultTaxMinimumTax()));
								return;
							}
							town.setTaxes(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_tax"), player.getName(), town.getTaxes()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plottax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plottax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plottax"), player.getName(), town.getPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("shoptax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shoptax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_alttax"), player.getName(), "shop", town.getCommercialPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("embassytax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassytax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_alttax"), player.getName(), "embassy", town.getEmbassyPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plotprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plotprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plotprice"), player.getName(), town.getPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("shopprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shopprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_altprice"), player.getName(), "shop", town.getCommercialPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("embassyprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassyprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_altprice"), player.getName(), "embassy", town.getEmbassyPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("spawncost")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set spawncost 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							if (TownySettings.getSpawnTravelCost() < amount) {
								TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_cannot_set_spawn_cost_more_than"), TownySettings.getSpawnTravelCost()));
								return;
							}
							town.setSpawnCost(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_spawn_cost_set_to"), player.getName(), TownySettings.getLangString("town_sing"), split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("name")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set name BillyBobTown");
						return;
					}

                    if(TownySettings.getTownRenameCost() > 0) {
                        try {
                            if (TownySettings.isUsingEconomy() && !town.pay(TownySettings.getTownRenameCost(), String.format("Town renamed to: %s", split[1])))
                                throw new TownyException(String.format(TownySettings.getLangString("msg_err_no_money"), TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())));
                        } catch (EconomyException e) {
                            throw new TownyException("Economy Error");
                        }
                    }

					if (!NameValidation.isBlacklistName(split[1]))
						townRename(player, town, split[1]);
					else
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

				} else if (split[0].equalsIgnoreCase("tag")) {

					if (split.length < 2)
						TownyMessaging.sendErrorMsg(player, "Eg: /town set tag PLTC");
					else if (split[1].equalsIgnoreCase("clear")) {
						try {
							town.setTag(" ");
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_reset_town_tag"), player.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}
					} else
						try {
							town.setTag(NameValidation.checkAndFilterName(split[1]));
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_set_town_tag"), player.getName(), town.getTag()));
						} catch (TownyException | InvalidNameException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}
					
				} else if (split[0].equalsIgnoreCase("homeblock")) {

					Coord coord = Coord.parseCoord(player);
					TownBlock townBlock;
					TownyWorld world;
					try {
						if (TownyAPI.getInstance().isWarTime())
							throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

						world = townyUniverse.getDatabase().getWorld(player.getWorld().getName());
						if (world.getMinDistanceFromOtherTowns(coord, resident.getTown()) < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
							if ((world.getMinDistanceFromOtherTowns(coord, resident.getTown()) > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
								throw new TownyException(TownySettings.getLangString("msg_too_far"));

						townBlock = townyUniverse.getDatabase().getWorld(player.getWorld().getName()).getTownBlock(coord);
						oldWorld = town.getWorld();
						town.setHomeBlock(townBlock);
						town.setSpawn(player.getLocation());

						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_town_home"), coord.toString()));

					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("spawn")) {

					try {
						town.setSpawn(player.getLocation());
						if(town.isCapital()) {
							nation.recheckTownDistance();
						}
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_town_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("outpost")) {

					try {
						TownyWorld townyWorld = townyUniverse.getDatabase().getWorld(player.getLocation().getWorld().getName());
						if (townyWorld.getTownBlock(Coord.parseCoord(player.getLocation())).getTown().getName().equals(town.getName())) {
							town.addOutpostSpawn(player.getLocation());
							TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_outpost_spawn"));
						} else
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_not_own_area"));

					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("jail")) {

					try {
						town.addJailSpawn(player.getLocation());
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_jail_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("perm")) {

					// Make sure we are allowed to set these permissions.
					try {
						toggleTest(player, town, StringMgmt.join(split, " "));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}
					String[] newSplit = StringMgmt.remFirstArg(split);
					setTownBlockOwnerPermissions(player, town, newSplit);

				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
					return;
				}
			}
			
			townyUniverse.getDatabase().saveTown(town);
			townyUniverse.getDatabase().saveTownList();

			if (nation != null) {
				townyUniverse.getDatabase().saveNation(nation);
				// TownyUniverse.getDataSource().saveNationList();
			}

			// If the town (homeblock) has moved worlds we need to update the
			// world files.
			if (oldWorld != null) {
				townyUniverse.getDatabase().saveWorld(town.getWorld());
				townyUniverse.getDatabase().saveWorld(oldWorld);
			}
		}
	}

	public void townBuy(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!TownySettings.isSellingBonusBlocks()) {
			TownyMessaging.sendErrorMsg(player, "Config.yml max_purchased_blocks: '0' ");
		}
			
		Resident resident;
		Town town;
		try {
			resident = townyUniverse.getDatabase().getResident(player.getName());
			town = resident.getTown();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town buy"));
			String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
			player.sendMessage(String.format(line, TownyEconomyHandler.getFormattedBalance(town.getBonusBlockCost()), TownySettings.getMaxPurchedBlocks()));
			player.sendMessage(ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
		} else {
			try {
				if (split[0].equalsIgnoreCase("bonus")) {
					if (split.length == 2) {
						try {
							townBuyBonusTownBlocks(town, Integer.parseInt(split[1].trim()), player);
						} catch (NumberFormatException e) {
							throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
						}
					} else {
						throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town buy bonus #"));
					}
				}
				
				townyUniverse.getDatabase().saveTown(town);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}
	}

	/**
	 * Town buys bonus blocks after checking the configured maximum.
	 *
	 * @param town
	 * @param inputN
	 * @param player
	 * @return The number of purchased bonus blocks.
	 * @throws TownyException
	 */
	public static int townBuyBonusTownBlocks(Town town, int inputN, Object player) throws TownyException {

		if (inputN < 0)
			throw new TownyException(TownySettings.getLangString("msg_err_negative"));

		int current = town.getPurchasedBlocks();

		int n;
		if (current + inputN > TownySettings.getMaxPurchedBlocks()) {
			n = TownySettings.getMaxPurchedBlocks() - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			return n;
		double cost = town.getBonusBlockCostN(n);
		try {
			boolean pay = town.pay(cost, String.format("Town Buy Bonus (%d)", n));
			if (TownySettings.isUsingEconomy() && !pay) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_to_buy"), n, "bonus town blocks", TownyEconomyHandler.getFormattedBalance(cost)));
			} else if (TownySettings.isUsingEconomy() && pay) {
				town.addPurchasedBlocks(n);
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_buy"), n, "bonus town blocks", TownyEconomyHandler.getFormattedBalance(cost)));
			}

		} catch (EconomyException e1) {
			throw new TownyException("Economy Error");
		}

		return n;
	}

	/**
	 * Create a new town. Command: /town new [town]
	 *
	 * @param player
	 * @param name - name of town
	 * @param mayorName - name of mayor
	 * @param noCharge - charging for creation - /ta town new NAME MAYOR has no charge.
	 */

	public static void newTown(Player player, String name, String mayorName, boolean noCharge) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			if (TownyAPI.getInstance().isWarTime())
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

			if (TownySettings.hasTownLimit() && townyUniverse.getDatabase().getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException(TownySettings.getLangString("msg_err_universe_limit"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || townyUniverse.getDatabase().hasTown(filteredName))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));

			Resident resident = townyUniverse.getDatabase().getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_already_res"), resident.getName()));

			TownyWorld world = townyUniverse.getDatabase().getWorld(player.getWorld().getName());

			if (!world.isUsingTowny())
				throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));

			if (!world.isClaimable())
				throw new TownyException(TownySettings.getLangString("msg_not_claimable"));

			Coord key = Coord.parseCoord(player);

			if (world.hasTownBlock(key))
				throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));
			
			if ((world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceFromTownPlotblocks()))
				throw new TownyException(TownySettings.getLangString("msg_too_close"));

			if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
				throw new TownyException(TownySettings.getLangString("msg_too_close"));

			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
				if ((world.getMinDistanceFromOtherTowns(key) > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
					throw new TownyException(TownySettings.getLangString("msg_too_far"));

			if (!noCharge && TownySettings.isUsingEconomy() && !resident.pay(TownySettings.getNewTownPrice(), "New Town Cost"))
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_new_town2"), (resident.getName().equals(player.getName()) ? "You" : resident.getName()), TownySettings.getNewTownPrice()));

			newTown(world, name, resident, key, player.getLocation());
			TownyMessaging.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), name));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			// TODO: delete town data that might have been done
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, "No valid economy found, your server admin might need to install Vault.jar or set using_economy: false in the Towny config.yml");
		}
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		world.newTownBlock(key);
		townyUniverse.getDatabase().newTown(name);
		Town town = townyUniverse.getDatabase().getTown(name);
		town.addResident(resident);
		town.setMayor(resident);
		TownBlock townBlock = world.getTownBlock(key);
		townBlock.setTown(town);
		town.setHomeBlock(townBlock);
		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());

		town.setSpawn(spawn);
		town.setUuid(UUID.randomUUID());
		town.setRegistered(System.currentTimeMillis());
		// world.addTown(town);

		if (world.isUsingPlotManagementRevert()) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null) {

				TownyRegenAPI.deletePlotChunk(plotChunk); // just claimed so stop regeneration.

			} else {

				plotChunk = new PlotBlockData(townBlock); // Not regenerating so create a new snapshot.
				plotChunk.initialize();

			}
			TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
			plotChunk = null;
		}
		TownyMessaging.sendDebugMsg("Creating new Town account: " + "town-" + name);
		if (TownySettings.isUsingEconomy()) {
			try {
				town.setBalance(0, "Deleting Town");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		
		townyUniverse.getDatabase().saveResident(resident);
		townyUniverse.getDatabase().saveTownBlock(townBlock);
		townyUniverse.getDatabase().saveTown(town);
		townyUniverse.getDatabase().saveWorld(world);
		
		townyUniverse.getDatabase().saveTownList();
		townyUniverse.getDatabase().saveTownBlockList();

		// Reset cache permissions for anyone in this TownBlock
		plugin.updateCache(townBlock.getWorldCoord());

		BukkitTools.getPluginManager().callEvent(new NewTownEvent(town));

		return town;
	}

	public void townRename(Player player, Town town, String newName) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			townyUniverse.getDatabase().renameTown(town, newName);
			town = townyUniverse.getDatabase().getTown(newName);
			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), player.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void townLeave(Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident resident;
		Town town;
		try {
			// TODO: Allow leaving town during war.
			if (TownyAPI.getInstance().isWarTime())
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

			resident = townyUniverse.getDatabase().getResident(player.getName());
			town = resident.getTown();
			plugin.deleteCache(resident.getName());

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		if (resident.isMayor()) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getMayorAbondonMsg());
			return;
		}

		if (resident.isJailed()) {
			try {
				if (resident.getJailTown().equals(resident.getTown().getName())) {
					if (TownySettings.JailDeniesTownLeave()) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_cannot_abandon_town_while_jailed"));
						return;
					}
					resident.setJailed(false);
					resident.setJailSpawn(0);
					resident.setJailTown("");
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_player_escaped_jail_by_leaving_town"), resident.getName()));
				}
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
		}

		try {
			townRemoveResident(town, resident);
		} catch (EmptyTownException et) {
			townyUniverse.getDatabase().removeTown(et.getTown());

		} catch (NotRegisteredException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		
		townyUniverse.getDatabase().saveResident(resident);
		townyUniverse.getDatabase().saveTown(town);

		// Reset everyones cache permissions as this player leaving could affect
		// multiple areas
		plugin.resetCache();

		TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
		TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));

		try {
			checkTownResidents(town, resident);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wrapper for the townSpawn() method. All calls should be through here
	 * unless bypassing for admins.
	 *
	 * @param player
	 * @param split
	 * @param outpost
	 * @throws TownyException
	 */
	public static void townSpawn(Player player, String[] split, Boolean outpost) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {

			Resident resident = townyUniverse.getDatabase().getResident(player.getName());
			Town town;
			String notAffordMSG;

			// Set target town and affiliated messages.
			if ((split.length == 0) || ((split.length > 0) && (outpost))) {

				if (!resident.hasTown()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));
					return;
				}

				town = resident.getTown();
				notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");

				townSpawn(player, split, town, notAffordMSG, outpost);

			} else {
				// split.length > 1
				town = townyUniverse.getDatabase().getTown(split[0]);
				notAffordMSG = String.format(TownySettings.getLangString("msg_err_cant_afford_tp_town"), town.getName());

				townSpawn(player, split, town, notAffordMSG, outpost);

			}
		} catch (NotRegisteredException e) {

			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));

		}

	}

	/**
	 * Core spawn function to allow admin use.
	 *
	 * @param player
	 * @param split
	 * @param town
	 * @param notAffordMSG
	 * @param outpost
	 */
	public static void townSpawn(Player player, String[] split, Town town, String notAffordMSG, Boolean outpost) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			boolean isTownyAdmin = townyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_OTHER.getNode());
			Resident resident = townyUniverse.getDatabase().getResident(player.getName());
			Location spawnLoc;
			TownSpawnLevel townSpawnPermission;

			if (outpost) {
				
				if (!town.hasOutpostSpawn())
					throw new TownyException(TownySettings.getLangString("msg_err_outpost_spawn"));

				Integer index = null;
				try {
					if (!split[split.length - 1].contains("name:")) {
						index = Integer.parseInt(split[split.length - 1]);
					} else { // So now it say's name:123
						split[split.length -1] = split[split.length -1].replace("name:", "").replace("_", " ");
						for (Location loc : town.getAllOutpostSpawns()) {
							TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(loc);
							if (tboutpost != null) {
								String name = tboutpost.getName();
								if (name.startsWith(split[split.length - 1])) {
									index = 1 + town.getAllOutpostSpawns().indexOf(loc);
								}
							}
						}
						if (index == null) { // If it persists to be null, so it's not been given a value, set it to the fallback (1).
							index = 1;
						}
					}
				} catch (NumberFormatException e) {
					// invalid entry so assume the first outpost, also note: We DO NOT HAVE a number now, which means: if you type abc, you get brought to that outpost.
					// Let's consider the fact however: an outpost name begins with "123" and there are 123 Outposts. Then we put the prefix name:123 and that solves that.
					index = 1;
					// Trying to get Outpost  names.
					split[split.length -1] = split[split.length -1].replace("_", " ");
					for (Location loc : town.getAllOutpostSpawns()) {
						TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(loc);
						if (tboutpost != null) {
							String name = tboutpost.getName();
							if (name.startsWith(split[split.length - 1])) {
								index = 1 + town.getAllOutpostSpawns().indexOf(loc);
							}
						}
					}
				} catch (ArrayIndexOutOfBoundsException i) {
					// Number not present so assume the first outpost.
					index = 1;
				}
				
				if (TownySettings.isOutpostLimitStoppingTeleports() && TownySettings.isOutpostsLimitedByLevels() && town.isOverOutpostLimit() && (Math.max(1, index) > town.getOutpostLimit())) {					
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_over_outposts_limit"), town.getMaxOutpostSpawn(), town.getOutpostLimit()));
				}
				
				spawnLoc = town.getOutpostSpawn(Math.max(1, index));
			} else
				spawnLoc = town.getSpawn();

			// Determine conditions
			if (isTownyAdmin) {
				townSpawnPermission = TownSpawnLevel.ADMIN;
			} else if ((split.length == 0) && (!outpost)) {
				townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
			} else {
				// split.length > 1
				if (!resident.hasTown()) {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				} else if (resident.getTown() == town) {
					townSpawnPermission = outpost ? TownSpawnLevel.TOWN_RESIDENT_OUTPOST : TownSpawnLevel.TOWN_RESIDENT;
				} else if (resident.hasNation() && town.hasNation()) {
					Nation playerNation = resident.getTown().getNation();
					Nation targetNation = town.getNation();

					if (playerNation == targetNation) {
						if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_ally_isnt_public"), town));
						else 
							townSpawnPermission = TownSpawnLevel.PART_OF_NATION;
					} else if (targetNation.hasEnemy(playerNation)) {
						// Prevent enemies from using spawn travel.
						throw new TownyException(TownySettings.getLangString("msg_err_public_spawn_enemy"));
					} else if (targetNation.hasAlly(playerNation)) {
						if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_ally_isnt_public"), town));
						else 
							townSpawnPermission = TownSpawnLevel.NATION_ALLY;
					} else {
						townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
					}
				} else {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				}
			}

			TownyMessaging.sendDebugMsg(townSpawnPermission.toString() + " " + townSpawnPermission.isAllowed(town));
			townSpawnPermission.checkIfAllowed(plugin, player, town);

			// Check the permissions
			if (!(isTownyAdmin || ((townSpawnPermission == TownSpawnLevel.UNAFFILIATED) ? town.isPublic() : townSpawnPermission.hasPermissionNode(plugin, player, town)))) {

				throw new TownyException(TownySettings.getLangString("msg_err_not_public"));

			}

			if (!isTownyAdmin) {
				// Prevent spawn travel while in disallowed zones (if
				// configured)
				List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();

				if (!disallowedZones.isEmpty()) {
					String inTown;
					try {
						Location loc = plugin.getCache(player).getLastLocation();
						inTown = TownyAPI.getInstance().getTownName(loc);
					} catch (NullPointerException e) {
						inTown = TownyAPI.getInstance().getTownName(player.getLocation());
					}

					if (inTown == null && disallowedZones.contains("unclaimed"))
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "the Wilderness"));
					if (inTown != null && resident.hasNation() && townyUniverse.getDatabase().getTown(inTown).hasNation()) {
						Nation inNation = townyUniverse.getDatabase().getTown(inTown).getNation();
						Nation playerNation = resident.getTown().getNation();
						if (inNation.hasEnemy(playerNation) && disallowedZones.contains("enemy"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Enemy areas"));
						if (!inNation.hasAlly(playerNation) && !inNation.hasEnemy(playerNation) && disallowedZones.contains("neutral"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Neutral towns"));
					}
				}
			}
			
			double travelCost = 0;
			
			// Taking whichever is smaller, the cost of the spawn price set by the town, or the cost set in the config (which is the maximum a town can set their spawncost to.)
			travelCost = Math.min(townSpawnPermission.getCost(town), townSpawnPermission.getCost());

			// Check if need/can pay
			if ((!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode())) &&
					(travelCost > 0 && TownySettings.isUsingEconomy() && (resident.getHoldingBalance() < travelCost)) ) {
				throw new TownyException(notAffordMSG);
			}

			// Used later to make sure the chunk we teleport to is loaded.
			Chunk chunk = spawnLoc.getChunk();

			// isJailed test
			if (resident.isJailed()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_cannot_spawn_while_jailed"));
				return;
			}

			// Essentials tests
			boolean UsingESS = plugin.isEssentials();

			if (UsingESS && !isTownyAdmin) {
				try {
					User user = plugin.getEssentials().getUser(player);

					if (!user.isJailed() && !resident.isJailed()) {

						Teleport teleport = user.getTeleport();
						if (!chunk.isLoaded())
							chunk.load();
						// Cause an essentials exception if in cooldown.
						teleport.cooldown(true);
						teleport.teleport(spawnLoc, null, TeleportCause.COMMAND);
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, "Error: " + e.getMessage());
					// cooldown?
					return;
				}
			}

			
			// Show message if we are using Vault and are charging for spawn travel.
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode()) ) {
				TownyEconomyObject payee = town;
				if (!TownySettings.isTownSpawnPaidToTown())					
					payee = TownyEconomyObject.SERVER_ACCOUNT;
				if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, payee, String.format("Town Spawn (%s)", townSpawnPermission))) {
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"), TownyEconomyHandler.getFormattedBalance(travelCost)));
				}
			}

			// If an Admin or Essentials teleport isn't being used, use our own.
			if (isTownyAdmin) {
				if (player.getVehicle() != null)
					player.getVehicle().eject();
				if (!chunk.isLoaded())
					chunk.load();
				player.teleport(spawnLoc, TeleportCause.COMMAND);
				return;
			}

			if (!UsingESS) {
				if (TownyTimerHandler.isTeleportWarmupRunning()) {
					// Use teleport warmup
					player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
					TownyAPI.getInstance().requestTeleport(player, spawnLoc);
				} else {
					// Don't use teleport warmup
					if (player.getVehicle() != null)
						player.getVehicle().eject();
					if (!chunk.isLoaded())
						chunk.load();
					player.teleport(spawnLoc, TeleportCause.COMMAND);
				}
			}
		} catch (TownyException | EconomyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void townDelete(Player player, String[] split) {

		Town town = null;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			try {
				Resident resident = townyUniverse.getDatabase().getResident(player.getName());
				ConfirmationHandler.addConfirmation(resident, ConfirmationType.TOWNDELETE, null); // It takes the senders town & nation, an admin deleting another town has no confirmation.
				TownyMessaging.sendConfirmationMessage(player, null, null, null, null);

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		} else {
			try {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_town"));

				town = townyUniverse.getDatabase().getTown(split[0]);

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
			TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
			townyUniverse.getDatabase().removeTown(town);
		}

	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 *
	 * @param player
	 * @param names
	 */

	public static void townKick(Player player, String[] names) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getInstance().getDatabase().getResident(player.getName());
			town = resident.getTown();
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		townKickResidents(player, resident, town, getValidatedResidents(player, names));

		// Reset everyones cache permissions as this player leaving can affect
		// multiple areas.
		plugin.resetCache();
	}

	/*
	 * private static List<Resident> getResidentMap(Player player, String[] names)
	 * { List<Resident> invited = new ArrayList<Resident>(); for (String name :
	 * names) try { Resident target =
	 * plugin.getTownyUniverse().getResident(name); invited.add(target); } catch
	 * (TownyException x) { TownyMessaging.sendErrorMsg(player, x.getMessage());
	 * } return invited; }
	 */
	public static void townAddResidents(Object sender, Town town, List<Resident> invited) {
		String name;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
		} else {
			name = null;
		}
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		for (Resident newMember : new ArrayList<>(invited)) {
			try {
				// only add players with the right permissions.
				if (BukkitTools.matchPlayer(newMember.getName()).isEmpty()) { // Not
																				// online
					TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_offline_no_join"), newMember.getName()));
					invited.remove(newMember);
				} else if (!townyUniverse.getPermissionSource().has(BukkitTools.getPlayer(newMember.getName()), PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
					TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_not_allowed_join"), newMember.getName()));
					invited.remove(newMember);
				} else if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown()){
					TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_err_max_residents_per_town_reached"), TownySettings.getMaxResidentsPerTown() ));
					invited.remove(newMember);
				} else if (TownySettings.getTownInviteCooldown() > 0 && ( (System.currentTimeMillis()/1000 - newMember.getRegistered()/1000) < (TownySettings.getTownInviteCooldown()) )) {
					TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_err_resident_doesnt_meet_invite_cooldown"), newMember));
					invited.remove(newMember);
				} else {
					town.addResidentCheck(newMember);
					townInviteResident(name,town, newMember);
				}
			} catch (TownyException e) {
				invited.remove(newMember);
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}
			if (town.hasOutlaw(newMember)) {
				try {
					town.removeOutlaw(newMember);
				} catch (NotRegisteredException ignored) {
				}
			}
		}

		if (invited.size() > 0) {
			StringBuilder msg = new StringBuilder();
			if (name == null){
				name = "Console";
			}
			for (Resident newMember : invited)
				msg.append(newMember.getName()).append(", ");

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));


			msg = new StringBuilder(String.format(TownySettings.getLangString("msg_invited_join_town"), name, msg.toString()));
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg.toString()));
			townyUniverse.getDatabase().saveTown(town);
		} else
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_invalid_name"));
	}

	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {

		town.addResident(resident);
		plugin.deleteCache(resident.getName());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDatabase().saveResident(resident);
		townyUniverse.getDatabase().saveTown(town);
	}

	private static void townInviteResident(String sender,Town town, Resident newMember) throws TownyException {

		PlayerJoinTownInvite invite = new PlayerJoinTownInvite(sender, town, newMember);
		try {
			if (!InviteHandler.getTowntoresidentinvites().containsEntry(town, newMember)) {
				newMember.newReceivedInvite(invite);
				town.newSentInvite(invite);
				InviteHandler.addInviteToList(invite);
				TownyMessaging.sendRequestMessage(newMember,invite);
				Bukkit.getPluginManager().callEvent(new TownInvitePlayerEvent(invite));
			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_player_already_invited"), newMember.getName()));
			}
		} catch (TooManyInvitesException e) {
			newMember.deleteReceivedInvite(invite);
			town.deleteSentInvite(invite);
			throw new TownyException(TownySettings.getLangString(e.getMessage()));
		}
	}

	private static void townRevokeInviteResident(Object sender, Town town, List<Resident> residents) {

		for (Resident invited : residents) {
			if (InviteHandler.getTowntoresidentinvites().containsEntry(town, invited)) {
				InviteHandler.getTowntoresidentinvites().remove(town, invited);
				for (Invite invite : invited.getReceivedInvites()) {
					if (invite.getSender().equals(town)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMessage(sender, TownySettings.getLangString("town_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static void townRemoveResident(Town town, Resident resident) throws EmptyTownException, NotRegisteredException {

		town.removeResident(resident);
		plugin.deleteCache(resident.getName());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDatabase().saveResident(resident);
		townyUniverse.getDatabase().saveTown(town);
	}

	public static void townKickResidents(Object sender, Resident resident, Town town, List<Resident> kicking) {

		Player player = null;

		if (sender instanceof Player)
			player = (Player) sender;

		for (Resident member : new ArrayList<>(kicking)) {
			if (resident == member || member.isMayor() || town.hasAssistant(member)) {
				TownyMessaging.sendMessage(sender, "You cannot kick yourself, the mayor, or an assistant.");
				kicking.remove(member);
			} else {
				try {
					townRemoveResident(town, member);
				} catch (NotRegisteredException e) {
					kicking.remove(member);
				} catch (EmptyTownException e) {
					// You can't kick yourself and only the mayor can kick
					// assistants
					// so there will always be at least one resident.
				}
			}
		}
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (kicking.size() > 0) {
			StringBuilder msg = new StringBuilder();
			for (Resident member : kicking) {
				msg.append(member.getName()).append(", ");
				Player p = BukkitTools.getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_kicked_by"), (player != null) ? player.getName() : "CONSOLE"));
			}
			msg = new StringBuilder(msg.substring(0, msg.length() - 2));
			msg = new StringBuilder(String.format(TownySettings.getLangString("msg_kicked"), (player != null) ? player.getName() : "CONSOLE", msg.toString()));
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg.toString()));
			try {
				if (!(sender instanceof Player) || !townyUniverse.getDatabase().getResident(player.getName()).hasTown() || !TownyUniverse.getInstance().getDatabase().getResident(player.getName()).getTown().equals(town))
					// For when the an admin uses /ta town {name} kick {residents}
					TownyMessaging.sendMessage(sender, ChatTools.color(msg.toString()));
			} catch (NotRegisteredException e) {
			}
			townyUniverse.getDatabase().saveTown(town);
		} else {
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_invalid_name"));
		}

		try {
			checkTownResidents(town, resident);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
	}

	public static void checkTownResidents(Town town, Resident removedResident) throws NotRegisteredException {
		if (!town.hasNation())
			return;
		Nation nation = town.getNation();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if ((town.isCapital()) && (TownySettings.getNumResidentsCreateNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			for (Town newCapital : town.getNation().getTowns())
				if (newCapital.getNumResidents() >= TownySettings.getNumResidentsCreateNation()) {
					town.getNation().setCapital(newCapital);
					if ((TownySettings.getNumResidentsJoinNation() > 0) && (removedResident.getTown().getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
						try {
							town.getNation().removeTown(town);
							townyUniverse.getDatabase().saveTown(town);
							townyUniverse.getDatabase().saveNation(nation);
							TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_capital_not_enough_residents_left_nation"), town.getName()));
						} catch (EmptyNationException e) {
							e.printStackTrace();
						}
					}
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_not_enough_residents_no_longer_capital"), newCapital.getName()));
					return;
				}
			TownyMessaging.sendNationMessage(town.getNation(), String.format(TownySettings.getLangString("msg_nation_disbanded_town_not_enough_residents"), town.getName()));
			TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(town.getNation()));
			townyUniverse.getDatabase().removeNation(town.getNation());

			if (TownySettings.isRefundNationDisbandLowResidents()) {
				try {
					town.pay(TownySettings.getNewNationPrice(), "nation refund");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_not_enough_residents_refunded"), TownySettings.getNewNationPrice()));
			}
		} else if ((!town.isCapital()) && (TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
			try {
				TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_town_not_enough_residents_left_nation"), town.getName()));
				town.getNation().removeTown(town);
				townyUniverse.getDatabase().saveTown(town);
				townyUniverse.getDatabase().saveNation(nation);
			} catch (EmptyNationException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * If no arguments are given (or error), send usage of command. If sender is
	 * a player: args = [town]. Elsewise: args = [resident] [town]
	 *
	 * @param sender
	 * @param args
	 */
	public static void parseTownJoin(CommandSender sender, String[] args) {

		try {
			Resident resident;
			Town town;
			String residentName, townName, contextualResidentName;
			boolean console = false;

			if (sender instanceof Player) {
				// Player
				if (args.length < 1)
					throw new Exception(String.format("Usage: /town join [town]"));

				Player player = (Player) sender;
				residentName = player.getName();
				townName = args[0];
				contextualResidentName = "You";
			} else {
				// Console
				if (args.length < 2)
					throw new Exception(String.format("Usage: town join [resident] [town]"));

				residentName = args[0];
				townName = args[1];
				contextualResidentName = residentName;
			}
			
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			resident = townyUniverse.getDatabase().getResident(residentName);
			town = townyUniverse.getDatabase().getTown(townName);

			// Check if resident is currently in a town.
			if (resident.hasTown())
				throw new Exception(String.format(TownySettings.getLangString("msg_err_already_res"), contextualResidentName));

			if (!console) {
				// Check if town is town is free to join.
				if (!town.isOpen())
					throw new Exception(String.format(TownySettings.getLangString("msg_err_not_open"), town.getFormattedName()));
				if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown())
					throw new Exception(String.format(TownySettings.getLangString("msg_err_max_residents_per_town_reached"), TownySettings.getMaxResidentsPerTown()));
				if (town.hasOutlaw(resident))
					throw new Exception(TownySettings.getLangString("msg_err_outlaw_in_open_town"));
			}

			// Check if player is already in selected town (Pointless)
			// Then add player to town.
			townAddResident(town, resident);

			// Resident was added successfully.
			TownyMessaging.sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 *
	 * @param sender
	 * @param specifiedTown to add to if not null
	 * @param names
	 */

	public static void townAdd(Object sender, Town specifiedTown, String[] names) {

		String name;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
		} else {
			name = "Console";
		}
		Resident resident;
		Town town;
		try {
			if (name.equalsIgnoreCase("Console")) {
				town = specifiedTown;
			} else {
				resident = TownyUniverse.getInstance().getDatabase().getResident(name);
				if (specifiedTown == null)
					town = resident.getTown();
				else
					town = specifiedTown;
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage());
			return;
		}

		List<String> reslist = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newreslist = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeinvites = new ArrayList<>();
		// List of invites to be removed;
		for (String townname : reslist) {
			if (townname.startsWith("-")) {
				removeinvites.add(townname.substring(1));
				// Add to removing them, remove the "-"
			} else {
				newreslist.add(townname);
				// add to adding them,
			}
		}
		names = newreslist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length != 0) {
			townRevokeInviteResident(sender,town, getValidatedResidents(sender, namestoremove));
		}

		if (names.length != 0) {
			townAddResidents(sender, town, getValidatedResidents(sender, names));
		}

		// Reset this players cached permissions
		if (!name.equalsIgnoreCase("Console"))
			plugin.resetCache(BukkitTools.getPlayerExact(name));
	}

	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {

		setTownBlockPermissions(player, townBlockOwner, townBlockOwner.getPermissions(), split, false);
	}

	public static void setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));

		} else {

			// reset the friend to resident so the perm settings don't fail
			if (friend && split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset all townBlock permissions (by town/resident)
					for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {

						if (((townBlockOwner instanceof Town) && (!townBlock.hasResident())) || ((townBlockOwner instanceof Resident) && (townBlock.hasResident()))) {

							// Reset permissions
							townBlock.setType(townBlock.getType());
							townyUniverse.getDatabase().saveTownBlock(townBlock);
						}
					}
					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "Town owned"));
					else
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "your"));

					// Reset all caches as this can affect everyone.
					plugin.resetCache();
					return;

				} else {
					// Set all perms to On or Off
					// '/town set perm off'

					try {
						boolean b = plugin.parseOnOff(split[0]);
						for (String element : new String[] { "residentBuild",
								"residentDestroy", "residentSwitch",
								"residentItemUse", "outsiderBuild",
								"outsiderDestroy", "outsiderSwitch",
								"outsiderItemUse", "allyBuild", "allyDestroy",
								"allySwitch", "allyItemUse" })
							perm.set(element, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_set_perm_syntax_error"));
						return;
					}
				}

			} else if (split.length == 2) {
				if ((!split[0].equalsIgnoreCase("resident")
						&& !split[0].equalsIgnoreCase("friend")
						&& !split[0].equalsIgnoreCase("ally")
						&& !split[0].equalsIgnoreCase("outsider"))
						&& !split[0].equalsIgnoreCase("build")
						&& !split[0].equalsIgnoreCase("destroy")
						&& !split[0].equalsIgnoreCase("switch")
						&& !split[0].equalsIgnoreCase("itemuse")) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_set_perm_syntax_error"));
					return;
				}

				try {

					boolean b = plugin.parseOnOff(split[1]);

					if (split[0].equalsIgnoreCase("resident") || split[0].equalsIgnoreCase("friend")) {
						perm.residentBuild = b;
						perm.residentDestroy = b;
						perm.residentSwitch = b;
						perm.residentItemUse = b;
					} else if (split[0].equalsIgnoreCase("outsider")) {
						perm.outsiderBuild = b;
						perm.outsiderDestroy = b;
						perm.outsiderSwitch = b;
						perm.outsiderItemUse = b;
					} else if (split[0].equalsIgnoreCase("ally")) {
						perm.allyBuild = b;
						perm.allyDestroy = b;
						perm.allySwitch = b;
						perm.allyItemUse = b;
					} else if (split[0].equalsIgnoreCase("build")) {
						perm.residentBuild = b;
						perm.outsiderBuild = b;
						perm.allyBuild = b;
					} else if (split[0].equalsIgnoreCase("destroy")) {
						perm.residentDestroy = b;
						perm.outsiderDestroy = b;
						perm.allyDestroy = b;
					} else if (split[0].equalsIgnoreCase("switch")) {
						perm.residentSwitch = b;
						perm.outsiderSwitch = b;
						perm.allySwitch = b;
					} else if (split[0].equalsIgnoreCase("itemuse")) {
						perm.residentItemUse = b;
						perm.outsiderItemUse = b;
						perm.allyItemUse = b;
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_set_perm_syntax_error"));
					return;
				}

			} else if (split.length == 3) {

				if ((!split[0].equalsIgnoreCase("resident")
						&& !split[0].equalsIgnoreCase("friend")
						&& !split[0].equalsIgnoreCase("ally")
						&& !split[0].equalsIgnoreCase("outsider"))
						|| (!split[1].equalsIgnoreCase("build")
								&& !split[1].equalsIgnoreCase("destroy")
								&& !split[1].equalsIgnoreCase("switch")
								&& !split[1].equalsIgnoreCase("itemuse"))) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_set_perm_syntax_error"));
					return;
				}

				try {
					boolean b = plugin.parseOnOff(split[2]);
					String s = "";
					s = split[0] + split[1];
					perm.set(s, b);

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_set_perm_syntax_error"));
					return;
				}
			}

			// Propagate perms to all unchanged, town owned, townblocks
			for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {
				if ((townBlockOwner instanceof Town) && (!townBlock.hasResident())) {
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						townyUniverse.getDatabase().saveTownBlock(townBlock);
					}
				}
			}

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("f", "r") : perm.getColourString())));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
		}
	}

	public static void parseTownClaimCommand(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "", TownySettings.getLangString("msg_block_claim")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "outpost", TownySettings.getLangString("mayor_help_3")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "[circle/rect] [radius]", TownySettings.getLangString("mayor_help_4")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "[circle/rect] auto", TownySettings.getLangString("mayor_help_5")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (TownyAPI.getInstance().isWarTime()) {
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
				}

				resident = townyUniverse.getDatabase().getResident(player.getName());
				town = resident.getTown();
				world = townyUniverse.getDatabase().getWorld(player.getWorld().getName());

				if (!world.isUsingTowny()) {
					throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));
				}

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true, outpost = false;
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUPTPOST.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
					
					if (TownySettings.isOutpostsLimitedByLevels() && (town.getMaxOutpostSpawn() >= town.getOutpostLimit()))
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_enough_outposts_free_to_claim"), town.getMaxOutpostSpawn(), town.getOutpostLimit()));


					if (TownySettings.getAmountOfResidentsForOutpost() != 0 && town.getResidents().size() < TownySettings.getAmountOfResidentsForOutpost()) {
						throw new TownyException(TownySettings.getLangString("msg_err_not_enough_residents"));
					}

					int maxOutposts = TownySettings.getMaxResidentOutposts(resident);
					if (!townyUniverse.getPermissionSource().isTownyAdmin(player) && maxOutposts != -1 && (maxOutposts <= resident.getTown().getAllOutpostSpawns().size()))
						throw new TownyException(String.format(TownySettings.getLangString("msg_max_outposts_own"), maxOutposts));

					if (TownySettings.isAllowingOutposts()) {

						if (world.hasTownBlock(key))
							throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));

						if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						if ((world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceFromTownPlotblocks()))
							throw new TownyException(TownySettings.getLangString("msg_too_close"));
						
						if ((world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceForOutpostsFromPlot()))
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
						blockCost = TownySettings.getOutpostCost();
						attachedToEdge = false;
						outpost = true;
					} else
						throw new TownyException(TownySettings.getLangString("msg_outpost_disable"));
				} else {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), split);
					if ((selection.size() > 1) && (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE.getNode())))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (TownySettings.isUsingEconomy())
						blockCost = TownySettings.getClaimPrice();
				}

				if ((world.getMinDistanceFromOtherTownsPlots(key, town) < TownySettings.getMinDistanceFromTownPlotblocks()))
					throw new TownyException(TownySettings.getLangString("msg_too_close"));

				if(!town.hasHomeBlock() && world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
					throw new TownyException(TownySettings.getLangString("msg_too_close"));

				TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterTownOwnedBlocks(selection);
				selection = AreaSelectionUtil.filterInvalidProximityTownBlocks(selection, town);
				
				TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);
								
				//Check if other plugins have a problem with claiming this area
				int blockedClaims = 0;

				for(WorldCoord coord : selection){
					//Use the user's current world
					TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, new TownBlock(coord.getX(), coord.getZ(), world));
					BukkitTools.getPluginManager().callEvent(preClaimEvent);
					if(preClaimEvent.isCancelled())
						blockedClaims++;
				}

				if(blockedClaims > 0){
					throw new TownyException(String.format(TownySettings.getLangString("msg_claim_error"), blockedClaims, selection.size()));
				}
				try {
					double cost = blockCost * selection.size();
					double missingAmount = cost - town.getHoldingBalance();
					if (TownySettings.isUsingEconomy() && !town.pay(cost, String.format("Town Claim (%d)", selection.size())))
						throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim2"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost),  TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
				} catch (EconomyException e1) {
					throw new TownyException("Economy Error");
				}
				new TownClaim(plugin, player, town, selection, outpost, true, false).start();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town unclaim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "", TownySettings.getLangString("mayor_help_6")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "[circle/rect] [radius]", TownySettings.getLangString("mayor_help_7")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "all", TownySettings.getLangString("mayor_help_8")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "outpost", TownySettings.getLangString("mayor_help_9")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (TownyAPI.getInstance().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				resident = townyUniverse.getDatabase().getResident(player.getName());
				town = resident.getTown();
				world = townyUniverse.getDatabase().getWorld(player.getWorld().getName());

				List<WorldCoord> selection;
				if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM_ALL.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
					new TownClaim(plugin, player, town, null, false, false, false).start();
					// townUnclaimAll(town);
					// If the unclaim code knows its an outpost or not, doesnt matter its only used once the world deletes the townblock, where it takes the value from the townblock.
					// Which is why in AreaSelectionUtil, since outpost is not parsed in the main claiming of a section, it is parsed in the unclaiming with the circle, rect & all options.
				} else {
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
					selection = AreaSelectionUtil.filterOwnedBlocks(town, selection);

					// Set the area to unclaim
					new TownClaim(plugin, player, town, selection, false, false, false).start();

					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_abandoned_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
				}

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, List<WorldCoord> worldCoords) {

		// TODO: Better algorithm that doesn't duplicates checks.

		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord))
				return true;
		return false;
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				if (edgeTownBlock.isOwner(owner)) {
					//TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = True.");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		//TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = False.");
		return false;
	}

	public static void checkIfSelectionIsValid(TownBlockOwner owner, List<WorldCoord> selection, boolean attachedToEdge, double blockCost, boolean force) throws TownyException {

		if (force)
			return;
		Town town = (Town) owner;

		if (attachedToEdge && !isEdgeBlock(owner, selection) && !town.getTownBlocks().isEmpty()) {
			if (selection.size() == 0)
				throw new TownyException(TownySettings.getLangString("msg_already_claimed_2"));
			else
				throw new TownyException(TownySettings.getLangString("msg_err_not_attached_edge"));
		}

		if (owner instanceof Town) {
			// Town town = (Town)owner;
			int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
			TownyMessaging.sendDebugMsg("Claim Check Available: " + available);
			TownyMessaging.sendDebugMsg("Claim Selection Size: " + selection.size());
			if (available - selection.size() < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_not_enough_blocks"));
		}

		try {
			double cost = blockCost * selection.size();
			double missingAmount = cost - town.getHoldingBalance();
			if (TownySettings.isUsingEconomy() && !owner.canPayFromHoldings(cost))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_cant_afford_blocks2"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost),  TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
		} catch (EconomyException e1) {
			throw new TownyException("Economy Error");
		}
	}

	private void townWithdraw(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			if (!TownySettings.getTownBankAllowWithdrawls())
				throw new TownyException(TownySettings.getLangString("msg_err_withdraw_disabled"));

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			resident = TownyUniverse.getInstance().getDatabase().getResident(player.getName());
			town = resident.getTown();

			town.withdrawFromBank(resident, amount);
			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), resident.getName(), amount, "town"));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void townDeposit(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getInstance().getDatabase().getResident(player.getName());
			town = resident.getTown();

			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + town.getHoldingBalance() > bankcap)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
			}

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			if (!resident.payTo(amount, town, "Town Deposit"))
				throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));

			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_deposited_xx"), resident.getName(), amount, "town"));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}
	
	
	
	public static List<Resident> getValidatedResidents(Object sender, String[] names) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Resident> invited = new ArrayList<>();
		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				StringBuilder line = new StringBuilder("Multiple players selected: ");
				for (Player p : matches)
					line.append(", ").append(p.getName());
				TownyMessaging.sendErrorMsg(sender, line.toString());
			} else if (matches.size() == 1) {
				// Match found online
				try {
					Resident target = townyUniverse.getDatabase().getResident(matches.get(0).getName());
					invited.add(target);
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(sender, x.getMessage());
				}
			} else {
				// No online matches so test for offline.
				Resident target;
				try {
					target = townyUniverse.getDatabase().getResident(name);
					invited.add(target);
				} catch (NotRegisteredException x) {
					TownyMessaging.sendErrorMsg(sender, x.getMessage());
				}
			}
		}
		return invited;
	}
	
	public static List<Resident> getOnlineResidentsViewable(Player viewer, ResidentList residentList) {
		
		List<Resident> onlineResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				/*
				 * Loop town/nation resident list
				 */
				for (Resident resident : residentList.getResidents()) {
					if (resident.getName().equalsIgnoreCase(player.getName()))
						if ((viewer == null) || (viewer.canSee(BukkitTools.getPlayerExact(resident.getName())))) {
							onlineResidents.add(resident);
						}
				}
			}
		}
		
		return onlineResidents;
	}
}
