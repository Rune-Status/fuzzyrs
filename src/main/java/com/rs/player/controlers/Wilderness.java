package com.rs.player.controlers;

import com.rs.entity.Entity;
import com.rs.server.Server;
import com.rs.content.actions.skills.Skills;
import com.rs.content.actions.skills.thieving.Thieving;
import com.rs.utils.Utils;
import com.rs.player.Player;
import com.rs.world.*;
import com.rs.world.npc.NPC;
import com.rs.task.worldtask.WorldTask;
import com.rs.task.worldtask.WorldTasksManager;

public class Wilderness extends Controller {

	private boolean showingSkull;

	public static void checkBoosts(final Player player) {
		boolean changed = false;
		int level = player.getSkills().getLevelForXp(Skills.ATTACK);
		int maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.ATTACK)) {
			player.getSkills().set(Skills.ATTACK, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.STRENGTH);
		maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.STRENGTH)) {
			player.getSkills().set(Skills.STRENGTH, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.DEFENCE);
		maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.DEFENCE)) {
			player.getSkills().set(Skills.DEFENCE, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.RANGE);
		maxLevel = (int) (level + 5 + (level * 0.1));
		if (maxLevel < player.getSkills().getLevel(Skills.RANGE)) {
			player.getSkills().set(Skills.RANGE, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.MAGIC);
		maxLevel = level + 5;
		if (maxLevel < player.getSkills().getLevel(Skills.MAGIC)) {
			player.getSkills().set(Skills.MAGIC, maxLevel);
			changed = true;
		}
		if (changed) {
			player.getPackets().sendGameMessage(
					"Your extreme potion bonus has been reduced.");
		}
	}

	public static boolean isDitch(final int id) {
		return id >= 1440 && id <= 1444 || id >= 65076 && id <= 65087;
	}

	public static boolean isAtWild(final WorldTile tile) {// TODO fix this
		return (tile.getX() >= 3011 && tile.getX() <= 3132
				&& tile.getY() >= 10052 && tile.getY() <= 10175) // fortihrny
				// dungeon
				|| (tile.getX() >= 2940 && tile.getX() <= 3395
				&& tile.getY() >= 3525 && tile.getY() <= 4000)
				|| (tile.getX() >= 3264 && tile.getX() <= 3279
				&& tile.getY() >= 3279 && tile.getY() <= 3672)
				|| (tile.getX() >= 2756 && tile.getX() <= 2875
				&& tile.getY() >= 5512 && tile.getY() <= 5627)
				|| (tile.getX() >= 3158 && tile.getX() <= 3181
				&& tile.getY() >= 3679 && tile.getY() <= 3697)
				|| (tile.getX() >= 3280 && tile.getX() <= 3183
				&& tile.getY() >= 3885 && tile.getY() <= 3888)
				|| (tile.getX() >= 3012 && tile.getX() <= 3059
				&& tile.getY() >= 10303 && tile.getY() <= 10351);
	}

	@Override
	public void start() {
		checkBoosts(player);
	}

	@Override
	public boolean login() {
		moved();
		return false;
	}

	@Override
	public boolean keepCombating(final Entity target) {
		if (target instanceof NPC)
			return true;
		if (!canAttack(target))
			return false;
		if (target.getAttackedBy() != player
				&& player.getAttackedBy() != target) {
			player.setWildernessSkull();
		}
		if (player.getCombatDefinitions().getSpellId() <= 0
				&& Utils.inCircle(new WorldTile(3105, 3933, 0), target, 24)) {
			player.getPackets().sendGameMessage(
					"You can only use magic in the arena.");
			return false;
		}
		return true;
	}

	@Override
	public boolean canAttack(final Entity target) {
		if (target instanceof Player) {
			final Player p2 = (Player) target;
			if (player.isCanPvp() && !p2.isCanPvp()) {
				player.getPackets().sendGameMessage(
						"That player is not in the wilderness.");
				return false;
			}
			if (canHit(target))
				return true;
			else
				player.sendMessage("You need to move farther into the wilderness to attack this player!");
			return false;
		}
		return true;
	}

	@Override
	public boolean canHit(final Entity target) {
		if (target instanceof NPC)
			return true;
		final Player p2 = (Player) target;
		return Math.abs(player.getSkills().getCombatLevel()
				- p2.getSkills().getCombatLevel()) <= getWildLevel();
	}

	@Override
	public boolean processMagicTeleport(final WorldTile toTile) {
		if (getWildLevel() > 20) {
			player.getPackets().sendGameMessage(
					"A mysterious force prevents you from teleporting.");
			return false;
		}
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets().sendGameMessage(
					"A mysterious force prevents you from teleporting.");
			return false;
		}
		return true;

	}

	@Override
	public boolean processItemTeleport(final WorldTile toTile) {
		if (getWildLevel() > 20) {
			player.getPackets().sendGameMessage(
					"A mysterious force prevents you from teleporting.");
			return false;
		}
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets().sendGameMessage(
					"A mysterious force prevents you from teleporting.");
			return false;
		}
		return true;
	}

	@Override
	public boolean processObjectTeleport(final WorldTile toTile) {
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets().sendGameMessage(
					"A mysterious force prevents you from teleporting.");
			return false;
		}
		return true;
	}

	public void showSkull() {
		player.getInterfaceManager()
				.sendTab(
						player.getInterfaceManager().hasRezizableScreen() ? 11
								: 0, 381);
	}

	@Override
	public boolean processObjectClick1(final WorldObject object) {
		if (isDitch(object.getId())) {
			player.lock();
			player.setNextAnimation(new Animation(6132));
			final WorldTile toTile = new WorldTile(object.getRotation() == 1
					|| object.getRotation() == 3 ? object.getX() + 2
					: player.getX(), object.getRotation() == 0
					|| object.getRotation() == 2 ? object.getY() - 1
					: player.getY(), object.getPlane());

			player.setNextForceMovement(new ForceMovement(
					new WorldTile(player),
					1,
					toTile,
					2,
					object.getRotation() == 0 || object.getRotation() == 2 ? ForceMovement.SOUTH
							: ForceMovement.EAST));
			WorldTasksManager.schedule(new WorldTask() {
				@Override
				public void run() {
					player.setNextWorldTile(toTile);
					player.faceObject(object);
					removeIcon();
					removeControler();
					player.resetReceivedDamage();
					player.unlock();
				}
			}, 2);
			return false;
		} else if (object.getId() == 2557 || object.getId() == 65717) {
			player.getPackets()
					.sendGameMessage(
							"It seems it is locked, maybe you should try something else.");
			return false;
		}
		return true;
	}

	@Override
	public boolean processObjectClick2(final WorldObject object) {
		if (object.getId() == 2557 || object.getId() == 65717) {
			Thieving.pickDoor(player, object);
			return false;
		}
		return true;
	}

	@Override
	public void sendInterfaces() {
		if (isAtWild(player)) {
			showSkull();
		}
	}

	@Override
	public boolean sendDeath() {

		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
					player.setNextAnimation(new Animation(836));
					player.getPackets().sendGameMessage(
							"Oh dear, you are dead!");
				} else if (loop == 1) {
					final Player killer = player
							.getMostDamageReceivedSourcePlayer();
					if (killer != null) {
						killer.increaseKillCount(player);
					}
					player.sendItemsOnDeath(killer);
					player.getEquipment().init();
					player.getInventory().init();
					player.reset();
					player.setNextWorldTile(new WorldTile(
							Server.getInstance().getSettingsManager().getSettings().getRespawnPlayerLocation()));
					player.setNextAnimation(new Animation(-1));
				} else if (loop == 2) {
					removeIcon();
					removeControler();
					player.getPackets().sendMusicEffect(90);
					stop();
				}
				loop++;
			}
		}, 0, 1);
		return false;
	}

	@Override
	public void moved() {
		final boolean isAtWild = isAtWild(player);
		final boolean isAtWildSafe = isAtWildSafe();
		if (!showingSkull && isAtWild && !isAtWildSafe) {
			showingSkull = true;
			player.setCanPvp(true);
			player.getAppearance().setRenderEmote(-1);
			showSkull();
			player.getAppearance().generateAppearenceData();
		} else if (showingSkull && (isAtWildSafe || !isAtWild)) {
			removeIcon();
		} else if (!isAtWildSafe && !isAtWild) {
			player.setCanPvp(false);
			removeIcon();
			removeControler();
		} else if (Kalaboss.isAtKalaboss(player)) {
			removeIcon();
			player.setCanPvp(false);
			removeControler();
			player.getControllerManager().startController(Kalaboss.class);
		}
	}

	public void removeIcon() {
		if (showingSkull) {
			showingSkull = false;
			player.setCanPvp(false);
			player.getPackets().closeInterface(
					player.getInterfaceManager().hasRezizableScreen() ? 11 : 0);
			player.getAppearance().generateAppearenceData();
			player.getEquipment().refresh(null);
		}
	}

	@Override
	public boolean logout() {
		return false; // so doesnt remove script
	}

	@Override
	public void forceClose() {
		removeIcon();
	}

	public boolean isAtWildSafe() {
		return (player.getX() >= 2940 && player.getX() <= 3395
				&& player.getY() <= 3524 && player.getY() >= 3523);
	}

	public int getWildLevel() {
		if (player.getY() > 9900)
			return (player.getY() - 9912) / 8 + 1;
		return (player.getY() - 3520) / 8 + 1;
	}

}
