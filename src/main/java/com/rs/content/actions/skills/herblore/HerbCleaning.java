package com.rs.content.actions.skills.herblore;

import com.rs.content.actions.skills.Skills;
import com.rs.player.Player;
import com.rs.world.item.Item;
import com.rs.task.worldtask.WorldTask;
import com.rs.task.worldtask.WorldTasksManager;

public class HerbCleaning {

	public static Herbs getHerb(final int id) {
		for (final Herbs herb : Herbs.values())
			if (herb.getHerbId() == id)
				return herb;
		return null;
	}

	public static boolean clean(final Player player, final Item item,
								final int slotId) {
		final Herbs herb = getHerb(item.getId());
		if (herb == null)
			return false;
		if (player.getSkills().getLevel(Skills.HERBLORE) < herb.getLevel()) {
			player.getPackets().sendGameMessage(
					"You do not have the required level to clean this.", true);
			return true;
		}
		WorldTasksManager.schedule(new WorldTask() {
			@Override
			public void run() {
				final Item i = player.getInventory().getItem(slotId);
				if (i == null)
					return;
				if (i.getId() != herb.getHerbId())
					return;
				i.setId(herb.getCleanId());
				player.getInventory().refresh(slotId);
				player.getSkills().addXp(Skills.HERBLORE, herb.getExperience());
				player.getPackets()
						.sendGameMessage("You clean the herb.", true);
			}

		});
		return true;
	}

	public enum Herbs {

		GUAM(199, 2.5, 3, 249),

		MARRENTILL(201, 3.8, 5, 251),

		TARROMIN(203, 5, 11, 253),

		HARRALANDER(205, 6.3, 20, 255),

		RANARR(207, 7.5, 25, 257),

		TOADFLAX(3049, 8, 30, 2998),

		SPIRIT_WEED(12174, 7.8, 35, 12172),

		IRIT(209, 8.8, 40, 259),

		WERGALI(14836, 9.5, 41, 14854),

		AVANTOE(211, 10, 48, 261),

		KWUARM(213, 11.3, 54, 263),

		SNAPDRAGON(3051, 11.8, 59, 3000),

		CADANTINE(215, 12.5, 65, 265),

		LANTADYME(2485, 13.1, 67, 2481),

		DWARF_WEED(217, 13.8, 70, 267),

		TORSTOL(219, 15, 75, 269);

		private final int herbId;
		private final int level;
		private final int cleanId;
		private final double xp;

		Herbs(final int herbId, final double xp, final int level,
				final int cleanId) {
			this.herbId = herbId;
			this.xp = xp;
			this.level = level;
			this.cleanId = cleanId;
		}

		public int getHerbId() {
			return herbId;
		}

		public double getExperience() {
			return xp;
		}

		public int getLevel() {
			return level;
		}

		public int getCleanId() {
			return cleanId;
		}
	}

}
