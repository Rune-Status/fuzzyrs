package com.rs.content.minigames;

import com.rs.utils.Utils;
import com.rs.player.Player;
import com.rs.world.Animation;

/**
 * Represents the chest on which the key is used.
 * 
 * @author 'Corey 2010 <MobbyGFX96@hotmail.co.uk>
 */

public class TreasureChest {

	public static final int[] KEY_HALVES = { 985, 987, 2677 };
	public static final int KEY = 2404;
	public static final int Animation = 5078;
	private static final int[] CHEST_REWARDS = {2651, 2997, 7114, 20197, 7505};

	/**
	 * Represents the key being made. Using tooth halves.
	 */
	public static void makeKey(final Player p) {
		if (p.getInventory().containsItem(toothHalf(), 1)
				&& p.getInventory().containsItem(loopHalf(), 1)) {
			p.getInventory().deleteItem(toothHalf(), 1);
			p.getInventory().deleteItem(loopHalf(), 1);
			p.getInventory().addItem(KEY, 1);
			p.sendMessage("You made a crystal key.");
		}
	}

	/**
	 * If the player can open the chest.
	 */
	public static boolean canOpen(final Player p) {
		if (p.getInventory().containsItem(KEY, 1))
			return true;
		else {
			p.sendMessage("This chest is locked.");
			return false;
		}
	}

	/**
	 * When the player searches the chest.
	 */
	public static void searchChest(final Player p) {
		if (canOpen(p)) {
			p.sendMessage("You unlock the chest with your key.");
			p.getInventory().deleteItem(KEY, 1);
			p.setNextAnimation(new Animation(Animation));
			p.getInventory().addItem(995, Utils.random(10000000));
			p.getInventory().addItem(
					CHEST_REWARDS[Utils.random(getLength() - 1)], 1);
			p.sendMessage("You quikly grab a treasure.");
		}
	}

	public static int getLength() {
		return CHEST_REWARDS.length;
	}

	/**
	 * Represents the toothHalf of the key.
	 */
	public static int toothHalf() {
		return KEY_HALVES[0];
	}

	/**
	 * Represent the loop half of the key.
	 */
	public static int loopHalf() {
		return KEY_HALVES[1];
	}

}