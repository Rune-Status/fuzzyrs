package com.rs.content.minigames.clanwars;

import com.rs.content.clans.ClanMember;
import com.rs.content.clans.ClansManager;
import com.rs.core.cores.CoresManager;
import com.rs.player.Player;
import com.rs.server.Server;
import com.rs.world.region.RegionBuilder;
import com.rs.world.World;
import com.rs.world.WorldObject;
import com.rs.world.WorldTile;
import com.rs.task.gametask.GameTask;
import com.rs.task.gametask.GameTaskManager;
import com.rs.task.gametask.GameTaskType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles the clan wars activity.
 * 
 * @author Emperor
 *
 */
public final class ClanWars implements Serializable {

	/**
	 * The list of currently active clan wars.
	 */
	private static final List<ClanWars> currentWars = new ArrayList<>();

	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 3329643510646371055L;
	/**
	 * The first team.
	 */
	private transient final ClansManager firstTeam;
	/**
	 * The second team.
	 */
	private transient final ClansManager secondTeam;
	/**
	 * The list of players ingame, of the first team.
	 */
	private transient final List<Player> firstPlayers = new ArrayList<Player>();
	/**
	 * The list of players ingame, of the second team.
	 */
	private transient final List<Player> secondPlayers = new ArrayList<Player>();
	/**
	 * The list of players viewing the first team.
	 */
	private transient final List<Player> firstViewers = new ArrayList<Player>();
	/**
	 * The list of players viewing the second team.
	 */
	private transient final List<Player> secondViewers = new ArrayList<Player>();
	/**
	 * A bit set containing the rules which have been activated.
	 */
	private transient final BitSet rules = new BitSet();
	/**
	 * The wall objects list.
	 */
	private transient List<WorldObject> wallObjects;

	/**
	 * The victory type for this war.
	 */
	private transient int victoryType = -1;

	/**
	 * The amount of time left.
	 */
	private transient int timeLeft = -1;

	/**
	 * The current magic rule's counter.
	 */
	private transient int magicRuleCount;

	/**
	 * The current area type.
	 */
	private transient AreaType areaType = AreaType.CLASSIC_AREA;
	/**
	 * The base location used during this war.
	 */
	private transient WorldTile baseLocation;
	/**
	 * The current clan wars clanWarsTask instance.
	 */
	private transient ClanWarsTask clanWarsTask;
	/**
	 * The amount of kills done.
	 */
	private transient int kills = 0;

	/**
	 * Constructs a new {@code ClanWars} {@code Object}.
	 *
	 * @param first
	 *            The first team.
	 * @param second
	 *            The second team.
	 */
	public ClanWars(final ClansManager first,
			final ClansManager second) {
		this.firstTeam = first;
		this.secondTeam = second;
	}

	/**
	 * Sends a config to both the players.
	 *
	 * @param player
	 *            The first player.
	 * @param other
	 *            The other player.
	 * @param configId
	 *            The config id.
	 * @param value
	 *            The value.
	 */
	public static void sendConfig(final Player player, final Player other,
								  final int configId, final int value) {
		boolean resetAccept = false;
		if (player.getTemporaryAttributtes().get("accepted_war_terms") == Boolean.TRUE) {
			player.getTemporaryAttributtes().remove("accepted_war_terms");
			resetAccept = true;
		}
		if (other.getTemporaryAttributtes().get("accepted_war_terms") == Boolean.TRUE) {
			other.getTemporaryAttributtes().remove("accepted_war_terms");
			resetAccept = true;
		}
		if (resetAccept) {
			player.getPackets().sendConfigByFile(5293, 0);
			other.getPackets().sendConfigByFile(5293, 0);
		}
		player.getPackets().sendConfigByFile(configId, value);
		other.getPackets().sendConfigByFile(configId, value);
	}

	/**
	 * Enters the purple portal.
	 *
	 * @param p The player.
	 */
	public static void enter(final Player p) {
		boolean hasWar = p.getClanManager() != null
				&& p.getClanManager().getClanWars() != null;
		final ClanWars c = hasWar ? p.getClanManager().getClanWars()
				: (ClanWars) p.getTemporaryAttributtes().get("view_clan");
		if (c == null)
			return;
		c.sendVictoryConfiguration(p);
		c.sendTimeConfiguration(p);
		p.getPackets().sendGlobalConfig(271, hasWar ? 1 : 0);
		p.getInterfaceManager().sendTab(
				p.getInterfaceManager().hasRezizableScreen() ? 11 : 0, 265);
		if (hasWar && c.clanWarsTask.isStarted() && c.isKnockOut()) {
			hasWar = false;
			p.getPackets().sendGameMessage("The war has already started!");
			p.getTemporaryAttributtes().put("view_prefix",
					c.firstTeam == p.getClanManager() ? 0 : 1);
		}
		if (hasWar) {
			if (c.get(Rules.NO_FAMILIARS) && p.getFamiliar() != null) {
				p.getPackets().sendGameMessage(
						"You can't enter the clan war with a familiar.");
				return;
			}
			if (c.get(Rules.NO_PRAYER)) {
				p.getPrayer().closeAllPrayers();
			}
			if (c.firstTeam == p.getClanManager()) {
				p.setNextWorldTile(c.baseLocation.transform(
						c.areaType.getFirstSpawnOffsetX(),
						c.areaType.getFirstSpawnOffsetY(), 0));
				c.firstPlayers.add(p);
				c.clanWarsTask.refresh(p, true);
			} else {
				final WorldTile northEast = c.baseLocation.transform(c.areaType
						.getNorthEastTile().getX()
						- c.areaType.getSouthWestTile().getX(), c.areaType
						.getNorthEastTile().getY()
						- c.areaType.getSouthWestTile().getY(), 0);
				p.setNextWorldTile(northEast.transform(
						c.areaType.getSecondSpawnOffsetX(),
						c.areaType.getSecondSpawnOffsetY(), 0));
				c.secondPlayers.add(p);
				c.clanWarsTask.refresh(p, false);
			}
			p.getControllerManager().startController(WarController.class, c);
			Server.getInstance().getGameTaskManager().scheduleTask(new PlayerRefreshTask(c, GameTask.ExecutionType.SCHEDULE, 0, 0, TimeUnit.MILLISECONDS));
			return;
		}
		final Integer prefix = (Integer) p.getTemporaryAttributtes().get(
				"view_prefix");
		if (prefix == null || prefix == 0) {
			c.clanWarsTask.refresh(p, true);
			c.firstViewers.add(p);
			p.setNextWorldTile(c.baseLocation.transform(
					c.areaType.getFirstDeathOffsetX(),
					c.areaType.getFirstDeathOffsetY(), 0));
		} else {
			c.clanWarsTask.refresh(p, false);
			c.secondViewers.add(p);
			final WorldTile northEast = c.baseLocation.transform(c.areaType
					.getNorthEastTile().getX()
					- c.areaType.getSouthWestTile().getX(), c.areaType
					.getNorthEastTile().getY()
					- c.areaType.getSouthWestTile().getY(), 0);
			p.setNextWorldTile(northEast.transform(
					c.areaType.getSecondDeathOffsetX(),
					c.areaType.getSecondDeathOffsetY(), 0));
		}
	}

	/**
	 * Gets the currentwars.
	 *
	 * @return The currentwars.
	 */
	public static List<ClanWars> getCurrentwars() {
		return currentWars;
	}

	/**
	 * Flags a rule if the rule was previously inactivated, unflags the rule if
	 * the rule was previously activated.
	 *
	 * @param rule
	 *            The rule to switch.
	 * @param player
	 *            The player switching the rule.
	 */
	public void switchRule(final Rules rule, final Player player) {
		final Player other = (Player) player.getTemporaryAttributtes().get(
				"clan_request_p");
		if (other == null
				|| player.getTemporaryAttributtes().get("clan_wars") != other
						.getTemporaryAttributtes().get("clan_wars"))
			return;
		if (rule == Rules.NO_MAGIC) {
			if (get(Rules.NO_RANGE) && get(Rules.NO_MELEE)) {
				player.getPackets()
						.sendGameMessage(
								"You can't activate all combat style rules, how would you fight?");
				return;
			} else {
				magicRuleCount = ++magicRuleCount % 4;
			}
			sendConfig(player, other, 5286, magicRuleCount);
			return;
		}
		if (magicRuleCount != 0
				&& ((rule == Rules.NO_MELEE && get(Rules.NO_RANGE)) || (rule == Rules.NO_RANGE && get(Rules.NO_MELEE)))) {
			player.getPackets()
					.sendGameMessage(
							"You can't activate all combat style rules, how would you fight?");
		} else {
			rules.set(rule.ordinal(), !rules.get(rule.ordinal()));
		}
		sendConfig(player, other, rule.configId, rules.get(rule.ordinal()) ? 1
				: 0);
	}

	/**
	 * Sends the victory type configuration.
	 *
	 * @param p
	 *            The player.
	 */
	private void sendVictoryConfiguration(final Player p) {
		switch (victoryType) {
		case -1:
			p.getPackets().sendConfigByFile(5280, 0);
			break;
		case 25:
			p.getPackets().sendConfigByFile(5280, 1);
			break;
		case 50:
			p.getPackets().sendConfigByFile(5280, 2);
			break;
		case 100:
			p.getPackets().sendConfigByFile(5280, 3);
			break;
		case 200:
			p.getPackets().sendConfigByFile(5280, 4);
			break;
		case 400:
			p.getPackets().sendConfigByFile(5280, 5);
			break;
		case 750:
			p.getPackets().sendConfigByFile(5280, 6);
			break;
		case 1_000:
			p.getPackets().sendConfigByFile(5280, 7);
			break;
		case 2_500:
			p.getPackets().sendConfigByFile(5280, 8);
			break;
		case 5_000:
			p.getPackets().sendConfigByFile(5280, 9);
			break;
		case 10_000:
			p.getPackets().sendConfigByFile(5280, 10);
			break;
		case -2:
			p.getPackets().sendConfigByFile(5280, 15);
			break;
		}
	}

	/**
	 * Sends the time configuration.
	 *
	 * @param p
	 *            The player.
	 */
	private void sendTimeConfiguration(final Player p) {
		switch (timeLeft) {
		case 500:
			p.getPackets().sendConfigByFile(5281, 1);
			break;
		case 1_000:
			p.getPackets().sendConfigByFile(5281, 2);
			break;
		case 3_000:
			p.getPackets().sendConfigByFile(5281, 3);
			break;
		case 6_000:
			p.getPackets().sendConfigByFile(5281, 4);
			break;
		case 9_000:
			p.getPackets().sendConfigByFile(5281, 5);
			break;
		case 12_000:
			p.getPackets().sendConfigByFile(5281, 6);
			break;
		case 15_000:
			p.getPackets().sendConfigByFile(5281, 7);
			break;
		case 18_000:
			p.getPackets().sendConfigByFile(5281, 8);
			break;
		case 24_000:
			p.getPackets().sendConfigByFile(5281, 9);
			break;
		case 30_000:
			p.getPackets().sendConfigByFile(5281, 10);
			break;
		case 36_000:
			p.getPackets().sendConfigByFile(5281, 11);
			break;
		case 48_000:
			p.getPackets().sendConfigByFile(5281, 12);
			break;
		case -1:
			p.getPackets().sendConfigByFile(5281, 0);
			break;
		}
	}

	/**
	 * Checks if a rule has been activated.
	 *
	 * @param rule
	 *            The rule to check.
	 * @return {@code True} if so.
	 */
	public boolean get(final Rules rule) {
		return rules.get(rule.ordinal());
	}

	/**
	 * Gets the firstTeam.
	 *
	 * @return The firstTeam.
	 */
	public ClansManager getFirstTeam() {
		return firstTeam;
	}

	/**
	 * Gets the secondTeam.
	 *
	 * @return The secondTeam.
	 */
	public ClansManager getSecondTeam() {
		return secondTeam;
	}

	/**
	 * Sends the interface for challenge request.
	 *
	 * @param p
	 *            The player to send to interface to.
	 * @param other
	 */
	public void sendInterface(final Player p, final Player other) {
		p.getTemporaryAttributtes().put("clan_wars", this);
		p.getInterfaceManager().sendInterface(791);
		p.getPackets().sendUnlockIComponentOptionSlots(791, 141, 0, 63, 0);
		p.getPackets().sendConfigByFile(5291, 0);
		p.getPackets().sendConfigByFile(5292, 0);
		p.getPackets().sendConfigByFile(5293, 0);
	}

	/**
	 * Called when the player accepts the challenge terms.
	 *
	 * @param player
	 *            The player.
	 */
	public void accept(final Player player) {
		final Player other = (Player) player.getTemporaryAttributtes().get(
				"clan_request_p");
		if (other != null
				&& other.getTemporaryAttributtes().get(
						"accepted_war_terms") == Boolean.TRUE) {
			CoresManager.SLOW_EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					player.getTemporaryAttributtes().remove(
							"accepted_war_terms");
					other.getTemporaryAttributtes()
							.remove("accepted_war_terms");
					player.getInterfaceManager().closeScreenInterface();
					other.getInterfaceManager().closeScreenInterface();
					for (final ClanMember member : firstTeam.getClan().getMembers()) {
						Player p = World.getPlayer(member.getUsername());
						if (p != player && p != other) {
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Your clan has been challenged to a clan war!</col>");
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Step through the purple portal in the Challenge Hall.</col>");
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Battle will commence in 2 minutes.</col>");
						}
					}
					for (final ClanMember member : secondTeam.getClan().getMembers()) {
						Player p = World.getPlayer(member.getUsername());
						if (p != player && p != other) {
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Your clan has been challenged to a clan war!</col>");
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Step through the purple portal in the Challenge Hall.</col>");
							p.getPackets()
									.sendGameMessage(
											"<col=FF0000>Battle will commence in 2 minutes.</col>");
						}
					}
					firstTeam.setClanWars(ClanWars.this);
					secondTeam.setClanWars(ClanWars.this);
					final int width = (areaType.getNorthEastTile().getX() - areaType
							.getSouthWestTile().getX()) / 8 + 1;
					final int height = (areaType.getNorthEastTile().getY() - areaType
							.getSouthWestTile().getY()) / 8 + 1;
					final int[] newCoords = RegionBuilder.findEmptyChunkBound(
							width, height);
					RegionBuilder.copyAllPlanesMap(areaType.getSouthWestTile()
							.getChunkX(), areaType.getSouthWestTile()
							.getChunkY(), newCoords[0], newCoords[1], width,
							height);
					baseLocation = new WorldTile(newCoords[0] << 3,
							newCoords[1] << 3, 0);
					WallHandler.loadWall(ClanWars.this);
					Server.getInstance().getGameTaskManager().scheduleTask(
							clanWarsTask = new ClanWarsTask((ClanWars.this), GameTask.ExecutionType.FIXED_RATE, 600, 600, TimeUnit.MILLISECONDS));
					enter(player);
					enter(other);
					currentWars.add(ClanWars.this);
				}
			});
			return;
		}
		player.getTemporaryAttributtes().put("accepted_war_terms", true);
	}

	/**
	 * Leaves the war.
	 *
	 * @param p
	 *            The player.
	 * @param ingame
	 *            If we're sure the player is ingame.
	 */
	public void leave(final Player p, final boolean ingame) {
		if (firstPlayers.contains(p)) {
			firstPlayers.remove(p);
		} else if (secondPlayers.contains(p)) {
			secondPlayers.remove(p);
		} else if (!ingame)
			return;
		final boolean resized = p.getInterfaceManager().hasRezizableScreen();
		p.getPackets().closeInterface(resized ? 746 : 548, resized ? 11 : 27);
		p.setNextWorldTile(new WorldTile(2992, 9676, 0));
		p.getControllerManager().startController(RequestController.class);
		p.setForceMultiArea(true);
		updateWar();
	}

	/**
	 * Updates the war.
	 */
	public void updateWar() {
		if (clanWarsTask.isStarted() && isKnockOut()) {
			if (firstPlayers.size() < 1 || secondPlayers.size() < 1) {
				clanWarsTask.cancel(true);
				endWar();
			}
		} else if (clanWarsTask.isStarted()
				&& !isMostKills()
				&& ((kills & 0xFFFF) >= victoryType || (kills >> 24 & 0xFFFF) >= victoryType)) {
			clanWarsTask.cancel(true);
			endWar();
		} else {
			for (final Player p : firstPlayers) {
				clanWarsTask.refresh(p, true);
			}
			for (final Player p : secondPlayers) {
				clanWarsTask.refresh(p, false);
			}
			for (final Player p : firstViewers) {
				clanWarsTask.refresh(p, true);
			}
			for (final Player p : secondViewers) {
				clanWarsTask.refresh(p, false);
			}
		}
	}

	/**
	 * Ends the current war.
	 */
	public void endWar() {
		currentWars.remove(this);
		firstTeam.setClanWars(null);
		secondTeam.setClanWars(null);
		final WorldTile target = new WorldTile(2992, 9676, 0);
		int firstType;
		int secondType;
		if (clanWarsTask.isTimeOut()) {
			firstType = 1;
			secondType = 1;
		} else if (isKnockOut() && firstPlayers.size() == secondPlayers.size()) {
			firstType = 3;
			secondType = 3;
		} else if (isMostKills() && (kills >> 24 & 0xFFFF) == (kills & 0xFFFF)) {
			firstType = 2;
			secondType = 2;
		} else if (isKnockOut()) {
			final boolean firstWon = firstPlayers.size() > secondPlayers.size();
			firstType = firstWon ? 4 : 8 + (clanWarsTask.getTimeLeft() == 0 ? 3 : 0);
			secondType = firstWon ? 8 : 4 + (clanWarsTask.getTimeLeft() == 0 ? 3 : 0);
		} else if (isMostKills()) {
			final boolean firstWon = (kills & 0xFFFF) > (kills >> 24 & 0xFFFF);
			firstType = firstWon ? 6 : 10;
			secondType = firstWon ? 10 : 6;
		} else {
			if ((kills & 0xFFFF) >= victoryType) {
				firstType = 5;
				secondType = 9;
			} else if ((kills >> 24 & 0xFFFF) >= victoryType) {
				firstType = 9;
				secondType = 5;
			} else if ((kills >> 24 & 0xFFFF) == (kills & 0xFFFF)) {
				firstType = 2;
				secondType = 2;
			} else if ((kills & 0xFFFF) > (kills >> 24 & 0xFFFF)) {
				firstType = 6;
				secondType = 10;
			} else {
				firstType = 10;
				secondType = 6;
			}
		}
		for (final Player player : firstPlayers) {
			player.setNextWorldTile(target);
			final boolean resized = player.getInterfaceManager()
					.hasRezizableScreen();
			player.getPackets().closeInterface(resized ? 746 : 548,
					resized ? 1 : 11);
			player.getInterfaceManager().sendInterface(790);
			player.getPackets().sendGlobalConfig(268, firstType);
			player.getControllerManager().startController(RequestController.class);
			player.setForceMultiArea(true);
			player.stopAll(true, false);
			player.reset();
		}
		for (final Player player : secondPlayers) {
			player.setNextWorldTile(target);
			final boolean resized = player.getInterfaceManager()
					.hasRezizableScreen();
			player.getPackets().closeInterface(resized ? 746 : 548,
					resized ? 1 : 11);
			player.getInterfaceManager().sendInterface(790);
			player.getPackets().sendGlobalConfig(268, secondType);
			player.getControllerManager().startController(RequestController.class);
			player.setForceMultiArea(true);
			player.stopAll(true, false);
			player.reset();
		}
		final List<Player> viewers = firstViewers;
		viewers.addAll(secondViewers);
		for (final Player p : viewers) {
			p.setNextWorldTile(target);
			final boolean resized = p.getInterfaceManager()
					.hasRezizableScreen();
			p.getPackets()
					.closeInterface(resized ? 746 : 548, resized ? 1 : 11);
			p.getControllerManager().startController(RequestController.class);
			p.setForceMultiArea(true);
		}
		final String firstMessage = "Your clan "
				+ (firstType < 4 ? "drawed." : firstType < 8 ? "is victorious!"
						: "has been defeated!");
		final String secondMessage = "Your clan "
				+ (secondType < 4 ? "drawed."
						: secondType < 8 ? "is victorious!"
								: "has been defeated!");
		for (final ClanMember member : firstTeam.getClan().getMembers()) {
            Player player = World.getPlayer(member.getUsername());
			player.getPackets().sendGameMessage(firstMessage);
		}
		for (final ClanMember member : secondTeam.getClan().getMembers()) {
            Player player = World.getPlayer(member.getUsername());
            player.getPackets().sendGameMessage(secondMessage);
		}
		CoresManager.SLOW_EXECUTOR.schedule(() -> {
			final int width = (areaType.getNorthEastTile().getX() - areaType
                    .getSouthWestTile().getX()) / 8 + 1;
            final int height = (areaType.getNorthEastTile().getY() - areaType
                    .getSouthWestTile().getY()) / 8 + 1;
            RegionBuilder.destroyMap(baseLocation.getChunkX(),
                    baseLocation.getChunkY(), width, height);
        }, 1200, TimeUnit.MILLISECONDS);
	}

	/**
	 * Gets the victoryType.
	 *
	 * @return The victoryType.
	 */
	public int getVictoryType() {
		return victoryType;
	}

	/**
	 * Checks if the victory type is knock-out.
	 *
	 * @return {@code True} if so (thus victory type equals {@code -1}).
	 */
	public boolean isKnockOut() {
		return victoryType == -1;
	}

	/**
	 * Checks if the victory type is most kills.
	 *
	 * @return {@code True} if so (thus victory type equals {@code -2}).
	 */
	public boolean isMostKills() {
		return victoryType == -2;
	}

	/**
	 * Sets the victoryType.
	 *
	 * @param victoryType
	 *            The victoryType to set.
	 * @param p
	 *            The player.
	 * @param other
	 *            The other player.
	 */
	public void setVictoryType(final int victoryType, final Player p,
			final Player other) {
		this.victoryType = victoryType;
		sendVictoryConfiguration(p);
		sendVictoryConfiguration(other);
	}

	/**
	 * Gets the timeLeft.
	 *
	 * @return The timeLeft.
	 */
	public int getTimeLeft() {
		return timeLeft;
	}

	/**
	 * Sets the timeLeft.
	 *
	 * @param timeLeft
	 *            The timeLeft to set.
	 * @param p
	 *            The player.
	 * @param other
	 *            The other player.
	 */
	public void setTimeLeft(final int timeLeft, final Player p,
			final Player other) {
		this.timeLeft = timeLeft;
		sendTimeConfiguration(p);
		sendTimeConfiguration(other);
	}

	/**
	 * Gets the clan wars clanWarsTask.
	 *
	 * @return The clan wars clanWarsTask.
	 */
	public ClanWarsTask getClanWarsTask() {
		return clanWarsTask;
	}

	/**
	 * Gets the areaType.
	 *
	 * @return The areaType.
	 */
	public AreaType getAreaType() {
		return areaType;
	}

	/**
	 * Sets the areaType.
	 *
	 * @param areaType
	 *            The areaType to set.
	 */
	public void setAreaType(final AreaType areaType) {
		this.areaType = areaType;
	}

	/**
	 * Gets the magicRuleCount.
	 *
	 * @return The magicRuleCount.
	 */
	public int getMagicRuleCount() {
		return magicRuleCount;
	}

	/**
	 * Sets the magicRuleCount.
	 *
	 * @param magicRuleCount
	 *            The magicRuleCount to set.
	 */
	public void setMagicRuleCount(final int magicRuleCount) {
		this.magicRuleCount = magicRuleCount;
	}

	/**
	 * Gets the baseLocation.
	 *
	 * @return The baseLocation.
	 */
	public WorldTile getBaseLocation() {
		return baseLocation;
	}

	/**
	 * Sets the baseLocation.
	 *
	 * @param baseLocation
	 *            The baseLocation to set.
	 */
	public void setBaseLocation(final WorldTile baseLocation) {
		this.baseLocation = baseLocation;
	}

	/**
	 * Gets the wallObjects.
	 *
	 * @return The wallObjects.
	 */
	public List<WorldObject> getWallObjects() {
		return wallObjects;
	}

	/**
	 * Sets the wallObjects.
	 *
	 * @param wallObjects
	 *            The wallObjects to set.
	 */
	public void setWallObjects(final List<WorldObject> wallObjects) {
		this.wallObjects = wallObjects;
	}

	/**
	 * Gets the firstPlayers.
	 *
	 * @return The firstPlayers.
	 */
	public List<Player> getFirstPlayers() {
		return firstPlayers;
	}

	/**
	 * Gets the secondPlayers.
	 *
	 * @return The secondPlayers.
	 */
	public List<Player> getSecondPlayers() {
		return secondPlayers;
	}

	/**
	 * Gets the kills.
	 *
	 * @return The kills.
	 */
	public int getKills() {
		return kills;
	}

	/**
	 * Sets the current kills.
	 *
	 * @param kills
	 *            The kills.
	 */
	public void setKills(final int kills) {
		this.kills = kills;
	}

	/**
	 * Gets the firstViewers.
	 *
	 * @return The firstViewers.
	 */
	public List<Player> getFirstViewers() {
		return firstViewers;
	}

	/**
	 * Gets the secondViewers.
	 *
	 * @return The secondViewers.
	 */
	public List<Player> getSecondViewers() {
		return secondViewers;
	}

	/**
	 * The possible rules.
	 *
	 * @author Emperor
	 *
	 */
	public enum Rules {
		NO_FOOD(5288), NO_POTIONS(5289), NO_PRAYER(5290), NO_MAGIC(-1), NO_MELEE(
				5284), NO_RANGE(5285), NO_FAMILIARS(5287), ITEMS_LOST(5283);

		/**
		 * The config id.
		 */
		private final int configId;

		/**
		 * Constructs a new {@code Rules} {@code Object}.
		 *
		 * @param configId The config id.
		 */
		Rules(final int configId) {
			this.configId = configId;
		}
	}

	@GameTaskType(GameTaskManager.GameTaskType.FAST)
	private static class PlayerRefreshTask extends GameTask {

		private ClanWars c;

		public PlayerRefreshTask(ClanWars c, ExecutionType executionType, long initialDelay, long tick, TimeUnit timeUnit) {
			super(executionType, initialDelay, tick, timeUnit);
			this.c = c;
		}

		@Override
		public void run() {
			for (final Player player : c.firstPlayers) {
				c.clanWarsTask.refresh(player, true);
			}
			for (final Player player : c.secondPlayers) {
				c.clanWarsTask.refresh(player, false);
			}
			for (final Player player : c.firstViewers) {
				c.clanWarsTask.refresh(player, true);
			}
			for (final Player player : c.secondViewers) {
				c.clanWarsTask.refresh(player, false);
			}
		}
	}
}