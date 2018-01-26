package com.rs.world;

import com.rs.server.Server;
import com.rs.content.actions.skills.Skills;
import com.rs.core.cache.loaders.AnimationDefinitions;
import com.rs.core.cache.loaders.ObjectDefinitions;
import com.rs.core.settings.GameConstants;
import com.rs.core.utils.Utils;
import com.rs.player.Player;
import com.rs.player.content.Magic;
import com.rs.world.npc.NPC;
import com.rs.world.npc.familiar.Familiar;
import com.rs.world.npc.qbd.TorturedSoul;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Entity extends WorldTile {

	private static final long serialVersionUID = -3372926325008880753L;
	private final static AtomicInteger hashCodeGenerator = new AtomicInteger();
	private final Poison poison;
	// transient stuff
	private transient int index;
	private transient int lastRegionId; // the last region the entity was at
	private transient WorldTile lastLoadedMapRegionTile;
	private transient CopyOnWriteArrayList<Integer> mapRegionsIds; // called by
	// more than
	// 1thread
	// so
	// concurent
	private transient int direction;
	private transient WorldTile lastWorldTile;
	private transient WorldTile nextWorldTile;
	private transient int nextWalkDirection;
	private transient int nextRunDirection;
	private transient WorldTile nextFaceWorldTile;
	private transient boolean teleported;
	private transient ConcurrentLinkedQueue<int[]> walkSteps;// called by more
	// than 1thread
	// so concurent
	private transient ConcurrentLinkedQueue<Hit> receivedHits;
	private transient Map<Entity, Integer> receivedDamage;
	private transient boolean finished; // if removed
	private transient long freezeDelay;
	// entity masks
	private transient Animation nextAnimation;
	private transient Graphics nextGraphics1;
	private transient Graphics nextGraphics2;
	private transient Graphics nextGraphics3;
	private transient Graphics nextGraphics4;
	private transient ArrayList<Hit> nextHits;
	private transient ForceMovement nextForceMovement;
	private transient ForceTalk nextForceTalk;
	private transient int nextFaceEntity;
	private transient int lastFaceEntity;
	private transient Entity attackedBy; // whos attacking you, used for single
	private transient long attackedByDelay; // delay till someone else can
	// attack you
	private transient boolean multiArea;
	private transient boolean isAtDynamicRegion;
	private transient long lastAnimationEnd;
	private transient boolean forceMultiArea;
	private transient long frozenBlocked;
	private transient long findTargetDelay;
	private transient ConcurrentHashMap<Object, Object> temporaryAttributes;
	private transient int hashCode;
	// saving stuff
	private int hitpoints;
	private int mapSize; // default 0, can be setted other value usefull on
	// static maps
	private boolean run;

	// creates Entity and saved classes
	public Entity(final WorldTile tile) {
		super(tile);
		poison = new Poison();
	}

	private static boolean colides(final int x1, final int y1, final int size1,
								   final int x2, final int y2, final int size2) {
		for (int checkX1 = x1; checkX1 < x1 + size1; checkX1++) {
			for (int checkY1 = y1; checkY1 < y1 + size1; checkY1++) {
				for (int checkX2 = x2; checkX2 < x2 + size2; checkX2++) {
					for (int checkY2 = y2; checkY2 < y2 + size2; checkY2++) {
						if (checkX1 == checkX2 && checkY1 == checkY2)
							return true;
					}

				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public boolean inArea(final int a, final int b, final int c, final int d) {
		return getX() >= a && getY() >= b && getX() <= c && getY() <= d;
	}

	public final void initEntity() {
		hashCode = hashCodeGenerator.getAndIncrement();
		mapRegionsIds = new CopyOnWriteArrayList<>();
		walkSteps = new ConcurrentLinkedQueue<>();
		receivedHits = new ConcurrentLinkedQueue<>();
		receivedDamage = new ConcurrentHashMap<>();
		temporaryAttributes = new ConcurrentHashMap<>();
		nextHits = new ArrayList<>();
		nextWalkDirection = nextRunDirection - 1;
		lastFaceEntity = -1;
		nextFaceEntity = -2;
		poison.setEntity(this);
	}

	public int getClientIndex() {
		return index + (this instanceof Player ? 32768 : 0);
	}

	public void applyHit(final Hit hit) {
		if (isDead())
			return;
		// todo damage for who gets drop
		receivedHits.add(hit); // added hit first because, soaking added after,
		// if applyhit used right there shouldnt be any
		// problem
		handleIngoingHit(hit);
	}

	public abstract void handleIngoingHit(Hit hit);

	public void reset(final boolean attributes) {
		setHitpoints(getMaxHitpoints());
		receivedHits.clear();
		resetCombat();
		walkSteps.clear();
		poison.reset();
		resetReceivedDamage();
		if (attributes) {
			temporaryAttributes.clear();
		}
	}

	public void reset() {
		reset(true);
	}

	public void resetCombat() {
		attackedBy = null;
		attackedByDelay = 0;
		freezeDelay = 0;
	}

	public void processReceivedHits() {
		if (this instanceof Player) {
			if (((Player) this).getEmotesManager().getNextEmoteEnd() >= Utils
					.currentTimeMillis())
				return;
		}
		Hit hit;
		int count = 0;
		while ((hit = receivedHits.poll()) != null && count++ < 10) {
			processHit(hit);
		}
	}

	private void processHit(final Hit hit) {
		if (isDead())
			return;
		removeHitpoints(hit);
		nextHits.add(hit);
	}

	public void removeHitpoints(final Hit hit) {
		if (isDead() || hit.getLook() == Hit.HitLook.ABSORB_DAMAGE)
			return;
		if (hit.getLook() == Hit.HitLook.HEALED_DAMAGE) {
			heal(hit.getDamage());
			return;
		}
		if (hit.getDamage() > hitpoints) {
			hit.setDamage(hitpoints);
		}
		addReceivedDamage(hit.getSource(), hit.getDamage());
		setHitpoints(hitpoints - hit.getDamage());
		if (hitpoints <= 0) {
			sendDeath(hit.getSource());
		} else if (this instanceof Player) {
			final Player player = (Player) this;
			if (player.getEquipment().getRingId() == 2550) {
				if (hit.getSource() != null && hit.getSource() != player) {
					hit.getSource().applyHit(
							new Hit(player, (int) (hit.getDamage() * 0.1),
									Hit.HitLook.REFLECTED_DAMAGE));
				}
			}
			if (player.getPrayer().hasPrayersOn()) {
				if ((hitpoints < player.getMaxHitpoints() * 0.1)
						&& player.getPrayer().usingPrayer(0, 23)) {
					setNextGraphics(new Graphics(436));
					setHitpoints((int) (hitpoints + player.getSkills()
							.getLevelForXp(Skills.PRAYER) * 2.5));
					player.getSkills().set(Skills.PRAYER, 0);
					player.getPrayer().setPrayerpoints(0);
				} else if (player.getEquipment().getAmuletId() != 11090
						&& player.getEquipment().getRingId() == 11090
						&& player.getHitpoints() <= player.getMaxHitpoints() * 0.1) {
					Magic.sendNormalTeleportSpell(player, 1, 0,
							Server.getInstance().getSettingsManager().getSettings().getRespawnPlayerLocation());
					player.getEquipment().deleteItem(11090, 1);
					player.getPackets()
					.sendGameMessage(
							"Your ring of life saves you, but is destroyed in the process.");
				}
			}
			if (player.getEquipment().getAmuletId() == 11090
					&& player.getHitpoints() <= player.getMaxHitpoints() * 0.2) {// priority
				// over
				// ring
				// of
				// life
				player.heal((int) (player.getMaxHitpoints() * 0.3));
				player.getEquipment().deleteItem(11090, 1);
				player.getPackets()
				.sendGameMessage(
						"Your pheonix necklace heals you, but is destroyed in the process.");
			}
		}
	}

	public void resetReceivedDamage() {
		receivedDamage.clear();
	}

	public void removeDamage(final Entity entity) {
		receivedDamage.remove(entity);
	}

	public Player getMostDamageReceivedSourcePlayer() {
		Player player = null;
		int damage = -1;
		for (final Entity source : receivedDamage.keySet()) {
			if (!(source instanceof Player)) {
				continue;
			}
			final Integer d = receivedDamage.get(source);
			if (d == null || source.hasFinished()) {
				receivedDamage.remove(source);
				continue;
			}
			if (d > damage) {
				player = (Player) source;
				damage = d;
			}
		}
		return player;
	}

	public void processReceivedDamage() {
		for (final Entity source : receivedDamage.keySet()) {
			Integer damage = receivedDamage.get(source);
			if (damage == null || source.hasFinished()) {
				receivedDamage.remove(source);
				continue;
			}
			damage--;
			if (damage == 0) {
				receivedDamage.remove(source);
				continue;
			}
			receivedDamage.put(source, damage);
		}
	}

	public void addReceivedDamage(final Entity source, final int amount) {
		if (source == null)
			return;
		Integer damage = receivedDamage.get(source);
		damage = damage == null ? amount : damage + amount;
		if (damage < 0) {
			receivedDamage.remove(source);
		} else {
			receivedDamage.put(source, damage);
		}
	}

	public void heal(final int ammount) {
		heal(ammount, 0);
	}

	public void heal(final int ammount, final int extra) {
		setHitpoints((hitpoints + ammount) >= (getMaxHitpoints() + extra) ? (getMaxHitpoints() + extra)
				: (hitpoints + ammount));
	}

	public boolean hasWalkSteps() {
		return !walkSteps.isEmpty();
	}

	public abstract void sendDeath(Entity source);

	public void processMovement() {
		lastWorldTile = new WorldTile(this);
		if (lastFaceEntity >= 0) {
			final Entity target = lastFaceEntity >= 32768 ? World.getPlayers()
					.get(lastFaceEntity - 32768) : World.getNPCs().get(
							lastFaceEntity);
					if (target != null) {
						direction = Utils.getFaceDirection(
								target.getCoordFaceX(target.getSize()) - getX(),
								target.getCoordFaceY(target.getSize()) - getY());
					}
		}
		nextWalkDirection = nextRunDirection = -1;
		if (nextWorldTile != null) {
			final int lastPlane = getPlane();
			setLocation(nextWorldTile);
			nextWorldTile = null;
			teleported = true;
			if (this instanceof Player
					&& ((Player) this).getTemporaryMoveType() == -1) {
				((Player) this).setTemporaryMoveType(Player.TELE_MOVE_TYPE);
			}
			World.updateEntityRegion(this);
			if (needMapUpdate()) {
				loadMapRegions();
			} else if (this instanceof Player && lastPlane != getPlane()) {
				((Player) this).setClientHasntLoadedMapRegion();
			}
			resetWalkSteps();
			return;
		}
		teleported = false;
		if (walkSteps.isEmpty())
			return;
		if (this instanceof Player) {
			if (((Player) this).getEmotesManager().getNextEmoteEnd() >= Utils
					.currentTimeMillis())
				return;
		}
		if (this instanceof TorturedSoul) {
			if (((TorturedSoul) this).switchWalkStep())
				return;
		}
		nextWalkDirection = getNextWalkStep();
		if (nextWalkDirection != -1) {
			if (this instanceof Player) {
				if (!((Player) this).getControllerManager().canMove(
						nextWalkDirection)) {
					nextWalkDirection = -1;
					resetWalkSteps();
					return;
				}
			}
			moveLocation(Utils.DIRECTION_DELTA_X[nextWalkDirection],
					Utils.DIRECTION_DELTA_Y[nextWalkDirection], 0);
			if (run) {
				if (this instanceof Player
						&& ((Player) this).getRunEnergy() <= 0) {
					setRun(false);
				} else {
					nextRunDirection = getNextWalkStep();
					if (nextRunDirection != -1) {
						if (this instanceof Player) {
							final Player player = (Player) this;
							if (!player.getControllerManager().canMove(nextRunDirection)) {
								nextRunDirection = -1;
								resetWalkSteps();
								return;
							}
							player.drainRunEnergy();
						}
						moveLocation(Utils.DIRECTION_DELTA_X[nextRunDirection],
								Utils.DIRECTION_DELTA_Y[nextRunDirection], 0);
					} else if (this instanceof Player) {
						((Player) this).setTemporaryMoveType(Player.WALK_MOVE_TYPE);
					}
				}
			}
		}
		World.updateEntityRegion(this);
		if (needMapUpdate()) {
			loadMapRegions();
		}
	}

	@Override
	public void moveLocation(final int xOffset, final int yOffset,
			final int planeOffset) {
		super.moveLocation(xOffset, yOffset, planeOffset);
		direction = Utils.getFaceDirection(xOffset, yOffset);
	}

	private boolean needMapUpdate() {
		final int lastMapRegionX = lastLoadedMapRegionTile.getChunkX();
		final int lastMapRegionY = lastLoadedMapRegionTile.getChunkY();
		final int regionX = getChunkX();
		final int regionY = getChunkY();
		final int size = ((GameConstants.MAP_SIZES[mapSize] >> 3) / 2) - 1;
		return Math.abs(lastMapRegionX - regionX) >= size
				|| Math.abs(lastMapRegionY - regionY) >= size;
	}

	public boolean addWalkSteps(final int destX, final int destY) {
		return addWalkSteps(destX, destY, -1);
	}

	/*
	 * returns if cliped
	 */
	public boolean clipedProjectile(WorldTile tile, final boolean checkClose) {
		if (tile instanceof NPC) {
			final NPC n = (NPC) tile;
			if (this instanceof Player)
				return n.clipedProjectile(this, checkClose);
			tile = n.getMiddleWorldTile();
		} else if (tile instanceof Player && this instanceof Player) {
			final Player p = (Player) tile;
			return clipedProjectile(tile, checkClose, 1)
					|| p.clipedProjectile(this, checkClose, 1);
		}
		return clipedProjectile(tile, checkClose, 1); // size 1 thats arrow
		// size, the tile has to
		// be target center
		// coord not base
	}

	/*
	 * return added all steps
	 */
	public boolean checkWalkStepsInteract(final int fromX, final int fromY,
			final int destX, final int destY, final int maxStepsCount,
			final int size, final boolean calculate) {
		final int[] lastTile = new int[] { fromX, fromY };
		int myX = lastTile[0];
		int myY = lastTile[1];
		int stepCount = 0;
		while (true) {
			stepCount++;
			final int myRealX = myX;
			final int myRealY = myY;

			if (myX < destX) {
				myX++;
			} else if (myX > destX) {
				myX--;
			}
			if (myY < destY) {
				myY++;
			} else if (myY > destY) {
				myY--;
			}
			if (!checkWalkStep(myX, myY, lastTile[0], lastTile[1], true)) {
				if (!calculate)
					return false;
				myX = myRealX;
				myY = myRealY;
				final int[] myT = checkcalculatedStep(myRealX, myRealY, destX,
						destY, lastTile[0], lastTile[1], size);
				if (myT == null)
					return false;
				myX = myT[0];
				myY = myT[1];
			}
			final int distanceX = myX - destX;
			final int distanceY = myY - destY;
			if (!(distanceX > size || distanceX < -1 || distanceY > size || distanceY < -1))
				return true;
			if (stepCount == maxStepsCount)
				return true;
			lastTile[0] = myX;
			lastTile[1] = myY;
			if (lastTile[0] == destX && lastTile[1] == destY)
				return true;
		}
	}

	public int[] checkcalculatedStep(int myX, int myY, final int destX,
			final int destY, final int lastX, final int lastY, final int size) {
		if (myX < destX) {
			myX++;
			if (!checkWalkStep(myX, myY, lastX, lastY, true)) {
				myX--;
			} else if (!(myX - destX > size || myX - destX < -1
					|| myY - destY > size || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		} else if (myX > destX) {
			myX--;
			if (!checkWalkStep(myX, myY, lastX, lastY, true)) {
				myX++;
			} else if (!(myX - destX > size || myX - destX < -1
					|| myY - destY > size || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		}
		if (myY < destY) {
			myY++;
			if (!checkWalkStep(myX, myY, lastX, lastY, true)) {
				myY--;
			} else if (!(myX - destX > size || myX - destX < -1
					|| myY - destY > size || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		} else if (myY > destY) {
			myY--;
			if (!checkWalkStep(myX, myY, lastX, lastY, true)) {
				myY++;
			} else if (!(myX - destX > size || myX - destX < -1
					|| myY - destY > size || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		}
		if (myX == lastX || myY == lastY)
			return null;
		return new int[] { myX, myY };
	}

	/*
	 * returns if cliped
	 */
	public boolean clipedProjectile(final WorldTile tile,
			final boolean checkClose, final int size) {
		int myX = getX();
		int myY = getY();
		if (this instanceof NPC && size == 1) {
			final NPC n = (NPC) this;
			final WorldTile thist = n.getMiddleWorldTile();
			myX = thist.getX();
			myY = thist.getY();
		}
		final int destX = tile.getX();
		final int destY = tile.getY();
		int lastTileX = myX;
		int lastTileY = myY;
		while (true) {
			if (myX < destX) {
				myX++;
			} else if (myX > destX) {
				myX--;
			}
			if (myY < destY) {
				myY++;
			} else if (myY > destY) {
				myY--;
			}
			final int dir = Utils.getMoveDirection(myX - lastTileX, myY
					- lastTileY);
			if (dir == -1)
				return false;
			if (checkClose) {
				if (!World.checkWalkStep(getPlane(), lastTileX, lastTileY, dir,
						size))
					return false;
			} else if (!World.checkProjectileStep(getPlane(), lastTileX,
					lastTileY, dir, size))
				return false;
			lastTileX = myX;
			lastTileY = myY;
			if (lastTileX == destX && lastTileY == destY)
				return true;
		}
	}

	public boolean addWalkStepsInteract(final int destX, final int destY,
			final int maxStepsCount, final int size, final boolean calculate) {
		return addWalkStepsInteract(destX, destY, maxStepsCount, size, size,
				calculate);
	}

	public boolean canWalkNPC(final int toX, final int toY) {
		return canWalkNPC(toX, toY, false);
	}

	private int getPreviewNextWalkStep() {
		final int step[] = walkSteps.poll();
		if (step == null)
			return -1;
		return step[0];
	}

	public boolean canWalkNPC(final int toX, final int toY,
			final boolean checkUnder) {
		if (!isAtMultiArea() /*
							 * || (!checkUnder && !canWalkNPC(getX(), getY(),
							 * true))
							 */)
			return true;
		final int size = getSize();
		for (final int regionId : getMapRegionsIds()) {
			final List<Integer> npcIndexes = World.getRegion(regionId)
					.getNPCsIndexes();
			if (npcIndexes != null) {
				for (final int npcIndex : npcIndexes) {
					final NPC target = World.getNPCs().get(npcIndex);
					if (target == null
							|| target == this
							|| target.isDead()
							|| target.hasFinished()
							|| target.getPlane() != getPlane()
							|| !target.isAtMultiArea()
							|| (!(this instanceof Familiar) && target instanceof Familiar)) {
						continue;
					}
					final int targetSize = target.getSize();
					if (!checkUnder && target.getNextWalkDirection() == -1) { // means
																				// the
																				// walk
																				// hasnt
																				// been
																				// processed
																				// yet
						final int previewDir = getPreviewNextWalkStep();
						if (previewDir != -1) {
							final WorldTile tile = target.transform(
									Utils.DIRECTION_DELTA_X[previewDir],
									Utils.DIRECTION_DELTA_Y[previewDir], 0);
							if (colides(tile.getX(), tile.getY(), targetSize,
									getX(), getY(), size)) {
								continue;
							}

							if (colides(tile.getX(), tile.getY(), targetSize,
									toX, toY, size))
								return false;
						}
					}
					if (colides(target.getX(), target.getY(), targetSize,
							getX(), getY(), size)) {
						continue;
					}
					if (colides(target.getX(), target.getY(), targetSize, toX,
							toY, size))
						return false;
				}
			}
		}
		return true;
	}

	/*
	 * return added all steps
	 */
	public boolean addWalkStepsInteract(final int destX, final int destY,
			final int maxStepsCount, final int sizeX, final int sizeY,
			final boolean calculate) {
		final int[] lastTile = getLastWalkTile();
		int myX = lastTile[0];
		int myY = lastTile[1];
		int stepCount = 0;
		while (true) {
			stepCount++;
			final int myRealX = myX;
			final int myRealY = myY;

			if (myX < destX) {
				myX++;
			} else if (myX > destX) {
				myX--;
			}
			if (myY < destY) {
				myY++;
			} else if (myY > destY) {
				myY--;
			}
			if ((this instanceof NPC && !canWalkNPC(myX, myY))
					|| !addWalkStep(myX, myY, lastTile[0], lastTile[1], true)) {
				if (!calculate)
					return false;
				myX = myRealX;
				myY = myRealY;
				final int[] myT = calculatedStep(myRealX, myRealY, destX,
						destY, lastTile[0], lastTile[1], sizeX, sizeY);
				if (myT == null)
					return false;
				myX = myT[0];
				myY = myT[1];
			}
			final int distanceX = myX - destX;
			final int distanceY = myY - destY;
			if (!(distanceX > sizeX || distanceX < -1 || distanceY > sizeY || distanceY < -1))
				return true;
			if (stepCount == maxStepsCount)
				return true;
			lastTile[0] = myX;
			lastTile[1] = myY;
			if (lastTile[0] == destX && lastTile[1] == destY)
				return true;
		}
	}

	public int[] calculatedStep(int myX, int myY, final int destX,
			final int destY, final int lastX, final int lastY, final int sizeX,
			final int sizeY) {
		if (myX < destX) {
			myX++;
			if ((this instanceof NPC && !canWalkNPC(myX, myY))
					|| !addWalkStep(myX, myY, lastX, lastY, true)) {
				myX--;
			} else if (!(myX - destX > sizeX || myX - destX < -1
					|| myY - destY > sizeY || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		} else if (myX > destX) {
			myX--;
			if ((this instanceof NPC && !canWalkNPC(myX, myY))
					|| !addWalkStep(myX, myY, lastX, lastY, true)) {
				myX++;
			} else if (!(myX - destX > sizeX || myX - destX < -1
					|| myY - destY > sizeY || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		}
		if (myY < destY) {
			myY++;
			if ((this instanceof NPC && !canWalkNPC(myX, myY))
					|| !addWalkStep(myX, myY, lastX, lastY, true)) {
				myY--;
			} else if (!(myX - destX > sizeX || myX - destX < -1
					|| myY - destY > sizeY || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		} else if (myY > destY) {
			myY--;
			if ((this instanceof NPC && !canWalkNPC(myX, myY))
					|| !addWalkStep(myX, myY, lastX, lastY, true)) {
				myY++;
			} else if (!(myX - destX > sizeX || myX - destX < -1
					|| myY - destY > sizeY || myY - destY < -1)) {
				if (myX == lastX || myY == lastY)
					return null;
				return new int[] { myX, myY };
			}
		}
		if (myX == lastX || myY == lastY)
			return null;
		return new int[] { myX, myY };
	}

	/*
	 * return added all steps
	 */
	public boolean addWalkSteps(final int destX, final int destY,
			final int maxStepsCount) {
		return addWalkSteps(destX, destY, -1, true);
	}

	/*
	 * return added all steps
	 */
	public boolean addWalkSteps(final int destX, final int destY,
			final int maxStepsCount, final boolean check) {
		final int[] lastTile = getLastWalkTile();
		int myX = lastTile[0];
		int myY = lastTile[1];
		int stepCount = 0;
		while (true) {
			stepCount++;
			if (myX < destX) {
				myX++;
			} else if (myX > destX) {
				myX--;
			}
			if (myY < destY) {
				myY++;
			} else if (myY > destY) {
				myY--;
			}
			if (!addWalkStep(myX, myY, lastTile[0], lastTile[1], check)) // cliped
				// here
				// so
				// stop
				return false;
			if (stepCount == maxStepsCount)
				return true;
			lastTile[0] = myX;
			lastTile[1] = myY;
			if (lastTile[0] == destX && lastTile[1] == destY)
				return true;
		}
	}

	public int[] getLastWalkTile() {
		final Object[] objects = walkSteps.toArray();
		if (objects.length == 0)
			return new int[] { getX(), getY() };
		final int step[] = (int[]) objects[objects.length - 1];
		return new int[] { step[1], step[2] };
	}

	// return cliped step
	public boolean checkWalkStep(final int nextX, final int nextY,
			final int lastX, final int lastY, final boolean check) {
		final int dir = Utils.getMoveDirection(nextX - lastX, nextY - lastY);
		if (dir == -1)
			return false;

		return !(check
				&& !World.checkWalkStep(getPlane(), lastX, lastY, dir,
				getSize()));
	}

	// return cliped step
	public boolean addWalkStep(final int nextX, final int nextY,
			final int lastX, final int lastY, final boolean check) {
		final int dir = Utils.getMoveDirection(nextX - lastX, nextY - lastY);
		if (dir == -1)
			return false;

		if (check) {
			if (!World.checkWalkStep(getPlane(), lastX, lastY, dir, getSize()))
				return false;
			if (this instanceof Player) {
				if (!((Player) this).getControllerManager().checkWalkStep(lastX,
						lastY, nextX, nextY))
					return false;
			}
		}
		walkSteps.add(new int[] { dir, nextX, nextY });
		return true;
	}

	public ConcurrentLinkedQueue<int[]> getWalkSteps() {
		return walkSteps;
	}

	public void resetWalkSteps() {
		walkSteps.clear();
	}

	private int getNextWalkStep() {
		final int step[] = walkSteps.poll();
		if (step == null)
			return -1;
		return step[0];
	}

	public boolean restoreHitPoints() {
		final int maxHp = getMaxHitpoints();
		if (hitpoints > maxHp) {
			if (this instanceof Player) {
				final Player player = (Player) this;
				if (player.getPrayer().usingPrayer(1, 5)
						&& Utils.getRandom(100) <= 15)
					return false;
			}
			setHitpoints(hitpoints - 1);
			return true;
		} else if (hitpoints < maxHp) {
			setHitpoints(hitpoints + 1);
			if (this instanceof Player) {
				final Player player = (Player) this;
				if (player.getPrayer().usingPrayer(0, 9) && hitpoints < maxHp) {
					setHitpoints(hitpoints + 1);
				} else if (player.getPrayer().usingPrayer(0, 26)
						&& hitpoints < maxHp) {
					setHitpoints(hitpoints
							+ (hitpoints + 4 > maxHp ? maxHp - hitpoints : 4));
				}

			}
			return true;
		}
		return false;
	}

	public boolean needMasksUpdate() {
		return nextFaceEntity != -2 || nextAnimation != null
				|| nextGraphics1 != null || nextGraphics2 != null
				|| nextGraphics3 != null || nextGraphics4 != null
				|| (nextWalkDirection == -1 && nextFaceWorldTile != null)
				|| !nextHits.isEmpty() || nextForceMovement != null
				|| nextForceTalk != null;
	}

	public boolean isDead() {
		return hitpoints == 0;
	}

	public void resetMasks() {
		nextAnimation = null;
		nextGraphics1 = null;
		nextGraphics2 = null;
		nextGraphics3 = null;
		nextGraphics4 = null;
		if (nextWalkDirection == -1) {
			nextFaceWorldTile = null;
		}
		nextForceMovement = null;
		nextForceTalk = null;
		nextFaceEntity = -2;
		nextHits.clear();
	}

	public abstract void finish();

	public abstract int getMaxHitpoints();

	public void processEntity() {
		poison.processPoison();
		processMovement();
		processReceivedHits();
		processReceivedDamage();
	}

	public void loadMapRegions() {
		mapRegionsIds.clear();
		isAtDynamicRegion = false;
		final int chunkX = getChunkX();
		final int chunkY = getChunkY();
		final int mapHash = GameConstants.MAP_SIZES[mapSize] >> 4;
				final int minRegionX = (chunkX - mapHash) / 8;
				final int minRegionY = (chunkY - mapHash) / 8;
				for (int xCalc = minRegionX < 0 ? 0 : minRegionX; xCalc <= ((chunkX + mapHash) / 8); xCalc++) {
					for (int yCalc = minRegionY < 0 ? 0 : minRegionY; yCalc <= ((chunkY + mapHash) / 8); yCalc++) {
						final int regionId = yCalc + (xCalc << 8);
						if (World.getRegion(regionId, this instanceof Player) instanceof DynamicRegion) {
							isAtDynamicRegion = true;
						}
						mapRegionsIds.add(regionId);
					}
				}
				lastLoadedMapRegionTile = new WorldTile(this); // creates a immutable
				// copy of this
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public int getHitpoints() {
		return hitpoints;
	}

	public void setHitpoints(final int hitpoints) {
		this.hitpoints = hitpoints;

	}

	public int getLastRegionId() {
		return lastRegionId;
	}

	public void setLastRegionId(final int lastRegionId) {
		this.lastRegionId = lastRegionId;
	}

	public int getMapSize() {
		return mapSize;
	}

	public void setMapSize(final int size) {
		this.mapSize = size;
		loadMapRegions();
	}

	public CopyOnWriteArrayList<Integer> getMapRegionsIds() {
		return mapRegionsIds;
	}

	public void setNextAnimationNoPriority(final Animation nextAnimation) {
		if (lastAnimationEnd > Utils.currentTimeMillis())
			return;
		setNextAnimation(nextAnimation);
	}

	public Animation getNextAnimation() {
		return nextAnimation;
	}

	public void setNextAnimation(final Animation nextAnimation) {
		if (nextAnimation != null && nextAnimation.getIds()[0] >= 0) {
			lastAnimationEnd = Utils.currentTimeMillis()
					+ AnimationDefinitions.getAnimationDefinitions(
					nextAnimation.getIds()[0]).getEmoteTime();
		}
		this.nextAnimation = nextAnimation;
	}

	public void setNextGraphics(final Graphics nextGraphics) {
		if (nextGraphics == null) {
			if (nextGraphics4 != null) {
				nextGraphics4 = null;
			} else if (nextGraphics3 != null) {
				nextGraphics3 = null;
			} else if (nextGraphics2 != null) {
				nextGraphics2 = null;
			} else {
				nextGraphics1 = null;
			}
		} else {
			if (nextGraphics.equals(nextGraphics1)
					|| nextGraphics.equals(nextGraphics2)
					|| nextGraphics.equals(nextGraphics3)
					|| nextGraphics.equals(nextGraphics4))
				return;
			if (nextGraphics1 == null) {
				nextGraphics1 = nextGraphics;
			} else if (nextGraphics2 == null) {
				nextGraphics2 = nextGraphics;
			} else if (nextGraphics3 == null) {
				nextGraphics3 = nextGraphics;
			} else {
				nextGraphics4 = nextGraphics;
			}
		}
	}

	public Graphics getNextGraphics1() {
		return nextGraphics1;
	}

	public Graphics getNextGraphics2() {
		return nextGraphics2;
	}

	public Graphics getNextGraphics3() {
		return nextGraphics3;
	}

	public Graphics getNextGraphics4() {
		return nextGraphics4;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(final int direction) {
		this.direction = direction;
	}

	public void setFinished(final boolean finished) {
		this.finished = finished;
	}

	public boolean hasFinished() {
		return finished;
	}

	public WorldTile getNextWorldTile() {
		return nextWorldTile;
	}

	public void setNextWorldTile(final WorldTile nextWorldTile) {
		this.nextWorldTile = nextWorldTile;
	}

	public boolean hasTeleported() {
		return teleported;
	}

	public WorldTile getLastLoadedMapRegionTile() {
		return lastLoadedMapRegionTile;
	}

	public int getNextWalkDirection() {
		return nextWalkDirection;
	}

	public int getNextRunDirection() {
		return nextRunDirection;
	}

	public boolean getRun() {
		return run;
	}

	public void setRun(final boolean run) {
		this.run = run;
	}

	public WorldTile getNextFaceWorldTile() {
		return nextFaceWorldTile;
	}

	public void setNextFaceWorldTile(final WorldTile nextFaceWorldTile) {
		if (nextFaceWorldTile.getX() == getX()
				&& nextFaceWorldTile.getY() == getY())
			return;
		this.nextFaceWorldTile = nextFaceWorldTile;
		if (nextWorldTile != null) {
			direction = Utils.getFaceDirection(nextFaceWorldTile.getX()
					- nextWorldTile.getX(), nextFaceWorldTile.getY()
					- nextWorldTile.getY());
		} else {
			direction = Utils.getFaceDirection(nextFaceWorldTile.getX()
					- getX(), nextFaceWorldTile.getY() - getY());
		}
	}

	public abstract int getSize();

	public void cancelFaceEntityNoCheck() {
		nextFaceEntity = -2;
		lastFaceEntity = -1;
	}

	public int getNextFaceEntity() {
		return nextFaceEntity;
	}

	public void setNextFaceEntity(final Entity entity) {
		if (entity == null) {
			nextFaceEntity = -1;
			lastFaceEntity = -1;
		} else {
			nextFaceEntity = entity.getClientIndex();
			lastFaceEntity = nextFaceEntity;
		}
	}

	public long getFreezeDelay() {
		return freezeDelay; // 2500 delay
	}

	public void setFreezeDelay(final int time) {
		this.freezeDelay = time;
	}

	public int getLastFaceEntity() {
		return lastFaceEntity;
	}

	public long getFrozenBlockedDelay() {
		return frozenBlocked;
	}

	public void setFrozeBlocked(final int time) {
		this.frozenBlocked = time;
	}

	public void addFrozenBlockedDelay(final int time) {
		frozenBlocked = time + Utils.currentTimeMillis();
	}

	public void addFreezeDelay(final long time) {
		addFreezeDelay(time, false);
	}

	public void addFreezeDelay(final long time, final boolean entangleMessage) {
		final long currentTime = Utils.currentTimeMillis();
		if (currentTime > freezeDelay) {
			resetWalkSteps();
			freezeDelay = time + currentTime;
			if (this instanceof Player) {
				final Player p = (Player) this;
				if (!entangleMessage) {
					p.getPackets().sendGameMessage("You have been frozen.");
				}
			}
		}
	}

	public abstract double getMagePrayerMultiplier();

	public abstract double getRangePrayerMultiplier();

	public abstract double getMeleePrayerMultiplier();

	public Entity getAttackedBy() {
		return attackedBy;
	}

	public void setAttackedBy(final Entity attackedBy) {
		this.attackedBy = attackedBy;
	}

	public long getAttackedByDelay() {
		return attackedByDelay;
	}

	public void setAttackedByDelay(final long attackedByDelay) {
		this.attackedByDelay = attackedByDelay;
	}

	public void checkMultiArea() {
		multiArea = forceMultiArea || World.isMultiArea(this);
	}

	public boolean isAtMultiArea() {
		return multiArea;
	}

	public void setAtMultiArea(final boolean multiArea) {
		this.multiArea = multiArea;
	}

	public boolean isAtDynamicRegion() {
		return isAtDynamicRegion;
	}

	public ForceMovement getNextForceMovement() {
		return nextForceMovement;
	}

	public void setNextForceMovement(final ForceMovement nextForceMovement) {
		this.nextForceMovement = nextForceMovement;
	}

	public Poison getPoison() {
		return poison;
	}

	public ForceTalk getNextForceTalk() {
		return nextForceTalk;
	}

	public void setNextForceTalk(final ForceTalk nextForceTalk) {
		this.nextForceTalk = nextForceTalk;
	}

	public void faceEntity(final Entity target) {
		setNextFaceWorldTile(new WorldTile(target.getCoordFaceX(target
				.getSize()), target.getCoordFaceY(target.getSize()),
				target.getPlane()));
	}

	public void faceObject(final WorldObject object) {
		final ObjectDefinitions objectDef = object.getDefinitions();
		setNextFaceWorldTile(new WorldTile(object.getCoordFaceX(
				objectDef.getSizeX(), objectDef.getSizeY(),
				object.getRotation()), object.getCoordFaceY(
						objectDef.getSizeX(), objectDef.getSizeY(),
						object.getRotation()), object.getPlane()));
	}

	public long getLastAnimationEnd() {
		return lastAnimationEnd;
	}

	public ConcurrentHashMap<Object, Object> getTemporaryAttributtes() {
		return temporaryAttributes;
	}

	public boolean isForceMultiArea() {
		return forceMultiArea;
	}

	public void setForceMultiArea(final boolean forceMultiArea) {
		this.forceMultiArea = forceMultiArea;
		checkMultiArea();
	}

	public WorldTile getLastWorldTile() {
		return lastWorldTile;
	}

	public ArrayList<Hit> getNextHits() {
		return nextHits;
	}

	public void playSound(final int soundId, final int type) {
		for (final int regionId : getMapRegionsIds()) {
			final List<Integer> playerIndexes = World.getRegion(regionId)
					.getPlayerIndexes();
			if (playerIndexes != null) {
				for (final int playerIndex : playerIndexes) {
					final Player player = World.getPlayers().get(playerIndex);
					if (player == null || !player.isRunning()
							|| !withinDistance(player)) {
						continue;
					}
					player.getPackets().sendSound(soundId, 0, type);
				}
			}
		}
	}

	public long getFindTargetDelay() {
		return findTargetDelay;
	}

	public void setFindTargetDelay(final long findTargetDelay) {
		this.findTargetDelay = findTargetDelay;
	}
}
