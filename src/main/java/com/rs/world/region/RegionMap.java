package com.rs.world.region;

/*
 * Author Alex(Also known as dragonkk)
 */

/*
 * 4x64x64 map
 */

import com.rs.world.World;
import com.rs.world.WorldTile;

/* old
 * 2097152 cliped tile
 * 131072 solid tile
 * 262144 floor tile
 */
public class RegionMap {

	private final int regionX;
	private final int regionY;
	private final int masks[][][];
	private final boolean clipedOnly;

	public RegionMap(final int regionId, final boolean clipedOnly) {
		regionX = (regionId >> 8) * 64;
		regionY = (regionId & 0xff) * 64;
		masks = new int[4][64][64];
		this.clipedOnly = clipedOnly;
	}

	public RegionMap(final int regionId, final int[][][] masks,
			final boolean clipedOnly) {
		regionX = (regionId >> 8) * 64;
		regionY = (regionId & 0xff) * 64;
		this.masks = masks;
		this.clipedOnly = clipedOnly;
	}

	public int[][][] getMasks() {
		return masks;
	}

	public int getRegionX() {
		return regionX;
	}

	public int getRegionY() {
		return regionY;
	}

	public void clipTile(final int plane, final int x, final int y) {
		addMask(plane, x, y, 2097152);
	}

	public void addWall(final int plane, final int x, final int y,
			final int type, final int rotation, final boolean solid,
			final boolean unknown) {
		if (type == 0) {
			if (rotation == 0) {
				addMask(plane, x, y, 128);
				addMask(plane, x - 1, y, 8);
			}
			if (rotation == 1) {
				addMask(plane, x, y, 2);
				addMask(plane, x, 1 + y, 32);
			}
			if (rotation == 2) {
				addMask(plane, x, y, 8);
				addMask(plane, 1 + x, y, 128);
			}
			if (rotation == 3) {
				addMask(plane, x, y, 32);
				addMask(plane, x, -1 + y, 2);
			}
		}
		if (type == 1 || type == 3) {
			if (rotation == 0) {
				addMask(plane, x, y, 1);
				addMask(plane, -1 + x, 1 + y, 16);
			}
			if (rotation == 1) {
				addMask(plane, x, y, 4);
				addMask(plane, 1 + x, 1 + y, 64);
			}
			if (rotation == 2) {
				addMask(plane, x, y, 16);
				addMask(plane, x + 1, -1 + y, 1);
			}
			if (rotation == 3) {
				addMask(plane, x, y, 64);
				addMask(plane, x - 1, -1 + y, 4);
			}
		}
		if (type == 2) {
			if (rotation == 0) {
				addMask(plane, x, y, 130);
				addMask(plane, -1 + x, y, 8);
				addMask(plane, x, y + 1, 32);
			}
			if (rotation == 1) {
				addMask(plane, x, y, 10);
				addMask(plane, x, 1 + y, 32);
				addMask(plane, 1 + x, y, 128);
			}
			if (rotation == 2) {
				addMask(plane, x, y, 40);
				addMask(plane, 1 + x, y, 128);
				addMask(plane, x, -1 + y, 2);
			}
			if (rotation == 3) {
				addMask(plane, x, y, 160);
				addMask(plane, x, -1 + y, 2);
				addMask(plane, -1 + x, y, 8);
			}
		}
		if (solid) {
			if (type == 0) {
				if (rotation == 0) {
					addMask(plane, x, y, 0x10000);
					addMask(plane, x - 1, y, 4096);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 1024);
					addMask(plane, x, 1 + y, 16384);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 4096);
					addMask(plane, x + 1, y, 0x10000);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 16384);
					addMask(plane, x, -1 + y, 1024);
				}
			}
			if (type == 1 || type == 3) {
				if (rotation == 0) {
					addMask(plane, x, y, 512);
					addMask(plane, x - 1, y + 1, 8192);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 2048);
					addMask(plane, x + 1, 1 + y, 32768);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 8192);
					addMask(plane, x + 1, y - 1, 512);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 32768);
					addMask(plane, x - 1, -1 + y, 2048);
				}
			}
			if (type == 2) {
				if (rotation == 0) {
					addMask(plane, x, y, 0x10400);
					addMask(plane, -1 + x, y, 4096);
					addMask(plane, x, y + 1, 16384);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 5120);
					addMask(plane, x, y + 1, 16384);
					addMask(plane, 1 + x, y, 0x10000);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 20480);
					addMask(plane, x + 1, y, 0x10000);
					addMask(plane, x, y - 1, 1024);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 0x14000);
					addMask(plane, x, -1 + y, 1024);
					addMask(plane, x - 1, y, 4096);
				}
			}
		}
		if (unknown) {
			if (type == 0) {
				if (rotation == 0) {
					addMask(plane, x, y, 0x20000000);
					addMask(plane, x - 1, y, 0x2000000);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 0x800000);
					addMask(plane, x, y + 1, 0x8000000);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 0x2000000);
					addMask(plane, x + 1, y, 0x20000000);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 0x8000000);
					addMask(plane, x, y - 1, 0x800000);
				}
			}
			if (type == 1 || type == 3) {
				if (rotation == 0) {
					addMask(plane, x, y, 0x400000);
					addMask(plane, x - 1, y + 1, 0x4000000);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 0x1000000);
					addMask(plane, 1 + x, 1 + y, 0x10000000);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 0x4000000);
					addMask(plane, x + 1, -1 + y, 0x400000);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 0x10000000);
					addMask(plane, -1 + x, y - 1, 0x1000000);
				}
			}
			if (type == 2) {
				if (rotation == 0) {
					addMask(plane, x, y, 0x20800000);
					addMask(plane, -1 + x, y, 0x2000000);
					addMask(plane, x, 1 + y, 0x8000000);
				}
				if (rotation == 1) {
					addMask(plane, x, y, 0x2800000);
					addMask(plane, x, 1 + y, 0x8000000);
					addMask(plane, x + 1, y, 0x20000000);
				}
				if (rotation == 2) {
					addMask(plane, x, y, 0xa000000);
					addMask(plane, 1 + x, y, 0x20000000);
					addMask(plane, x, y - 1, 0x800000);
				}
				if (rotation == 3) {
					addMask(plane, x, y, 0x28000000);
					addMask(plane, x, y - 1, 0x800000);
					addMask(plane, -1 + x, y, 0x2000000);
				}
			}
		}
	}

	public void removeWall(final int plane, final int x, final int y,
			final int type, final int rotation, final boolean solid,
			final boolean unknown) {
		if (type == 0) {
			if (rotation == 0) {
				removeMask(plane, x, y, 128);
				removeMask(plane, x - 1, y, 8);
			}
			if (rotation == 1) {
				removeMask(plane, x, y, 2);
				removeMask(plane, x, 1 + y, 32);
			}
			if (rotation == 2) {
				removeMask(plane, x, y, 8);
				removeMask(plane, 1 + x, y, 128);
			}
			if (rotation == 3) {
				removeMask(plane, x, y, 32);
				removeMask(plane, x, -1 + y, 2);
			}
		}
		if (type == 1 || type == 3) {
			if (rotation == 0) {
				removeMask(plane, x, y, 1);
				removeMask(plane, -1 + x, 1 + y, 16);
			}
			if (rotation == 1) {
				removeMask(plane, x, y, 4);
				removeMask(plane, 1 + x, 1 + y, 64);
			}
			if (rotation == 2) {
				removeMask(plane, x, y, 16);
				removeMask(plane, x + 1, -1 + y, 1);
			}
			if (rotation == 3) {
				removeMask(plane, x, y, 64);
				removeMask(plane, x - 1, -1 + y, 4);
			}
		}
		if (type == 2) {
			if (rotation == 0) {
				addMask(plane, x, y, 130);
				removeMask(plane, -1 + x, y, 8);
				removeMask(plane, x, y + 1, 32);
			}
			if (rotation == 1) {
				removeMask(plane, x, y, 10);
				removeMask(plane, x, 1 + y, 32);
				removeMask(plane, 1 + x, y, 128);
			}
			if (rotation == 2) {
				removeMask(plane, x, y, 40);
				removeMask(plane, 1 + x, y, 128);
				removeMask(plane, x, -1 + y, 2);
			}
			if (rotation == 3) {
				removeMask(plane, x, y, 160);
				removeMask(plane, x, -1 + y, 2);
				removeMask(plane, -1 + x, y, 8);
			}
		}
		if (solid) {
			if (type == 0) {
				if (rotation == 0) {
					removeMask(plane, x, y, 0x10000);
					removeMask(plane, x - 1, y, 4096);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 1024);
					removeMask(plane, x, 1 + y, 16384);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 4096);
					removeMask(plane, x + 1, y, 0x10000);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 16384);
					removeMask(plane, x, -1 + y, 1024);
				}
			}
			if (type == 1 || type == 3) {
				if (rotation == 0) {
					removeMask(plane, x, y, 512);
					removeMask(plane, x - 1, y + 1, 8192);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 2048);
					removeMask(plane, x + 1, 1 + y, 32768);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 8192);
					removeMask(plane, x + 1, y - 1, 512);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 32768);
					removeMask(plane, x - 1, -1 + y, 2048);
				}
			}
			if (type == 2) {
				if (rotation == 0) {
					removeMask(plane, x, y, 0x10400);
					removeMask(plane, -1 + x, y, 4096);
					removeMask(plane, x, y + 1, 16384);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 5120);
					removeMask(plane, x, y + 1, 16384);
					removeMask(plane, 1 + x, y, 0x10000);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 20480);
					removeMask(plane, x + 1, y, 0x10000);
					removeMask(plane, x, y - 1, 1024);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 0x14000);
					removeMask(plane, x, -1 + y, 1024);
					removeMask(plane, x - 1, y, 4096);
				}
			}
		}
		if (unknown) {
			if (type == 0) {
				if (rotation == 0) {
					removeMask(plane, x, y, 0x20000000);
					removeMask(plane, x - 1, y, 0x2000000);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 0x800000);
					removeMask(plane, x, y + 1, 0x8000000);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 0x2000000);
					removeMask(plane, x + 1, y, 0x20000000);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 0x8000000);
					removeMask(plane, x, y - 1, 0x800000);
				}
			}
			if (type == 1 || type == 3) {
				if (rotation == 0) {
					removeMask(plane, x, y, 0x400000);
					removeMask(plane, x - 1, y + 1, 0x4000000);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 0x1000000);
					removeMask(plane, 1 + x, 1 + y, 0x10000000);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 0x4000000);
					removeMask(plane, x + 1, -1 + y, 0x400000);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 0x10000000);
					removeMask(plane, -1 + x, y - 1, 0x1000000);
				}
			}
			if (type == 2) {
				if (rotation == 0) {
					removeMask(plane, x, y, 0x20800000);
					removeMask(plane, -1 + x, y, 0x2000000);
					removeMask(plane, x, 1 + y, 0x8000000);
				}
				if (rotation == 1) {
					removeMask(plane, x, y, 0x2800000);
					removeMask(plane, x, 1 + y, 0x8000000);
					removeMask(plane, x + 1, y, 0x20000000);
				}
				if (rotation == 2) {
					removeMask(plane, x, y, 0xa000000);
					removeMask(plane, 1 + x, y, 0x20000000);
					removeMask(plane, x, y - 1, 0x800000);
				}
				if (rotation == 3) {
					removeMask(plane, x, y, 0x28000000);
					removeMask(plane, x, y - 1, 0x800000);
					removeMask(plane, -1 + x, y, 0x2000000);
				}
			}
		}
	}

	public void removeObject(final int plane, final int x, final int y,
			final int sizeX, final int sizeY, final boolean solid,
			final boolean b) {
		int mask = 256;
		if (solid) {
			mask |= 131072;
		}
		if (b) {
			mask |= 1073741824;
		}
		for (int tileX = x; tileX < x + sizeX; tileX++) {
			for (int tileY = y; tileY < y + sizeY; tileY++) {
				removeMask(plane, tileX, tileY, mask);
			}
		}

	}

	public void addObject(final int plane, final int x, final int y,
			final int sizeX, final int sizeY, final boolean solid,
			final boolean b) {
		int mask = 256;
		if (solid) {
			mask |= 131072;
		}
		if (b) {
			mask |= 1073741824;
		}
		for (int tileX = x; tileX < x + sizeX; tileX++) {
			for (int tileY = y; tileY < y + sizeY; tileY++) {
				addMask(plane, tileX, tileY, mask);
			}
		}
	}

	public void setMask(final int plane, final int x, final int y,
			final int mask) {
		if (x >= 64 || y >= 64 || x < 0 || y < 0) {
			final WorldTile tile = new WorldTile(regionX + x, regionY + y,
					plane);
			final int regionId = tile.getRegionId();
			final int newRegionX = (regionId >> 8) * 64;
			final int newRegionY = (regionId & 0xff) * 64;
			if (clipedOnly) {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMapClipedOnly()
				.setMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			} else {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMap()
				.setMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			}
			return;
		}
		masks[plane][x][y] = mask;
	}

	public void removeMask(final int plane, final int x, final int y,
			final int mask) {
		if (x >= 64 || y >= 64 || x < 0 || y < 0) {
			final WorldTile tile = new WorldTile(regionX + x, regionY + y,
					plane);
			final int regionId = tile.getRegionId();
			final int newRegionX = (regionId >> 8) * 64;
			final int newRegionY = (regionId & 0xff) * 64;
			if (clipedOnly) {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMapClipedOnly()
				.removeMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			} else {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMap()
				.removeMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			}
			return;
		}
		masks[plane][x][y] = masks[plane][x][y] & (~mask);
	}

	public void addMask(final int plane, final int x, final int y,
			final int mask) {
		if (x >= 64 || y >= 64 || x < 0 || y < 0) {
			final WorldTile tile = new WorldTile(regionX + x, regionY + y,
					plane);
			final int regionId = tile.getRegionId();
			final int newRegionX = (regionId >> 8) * 64;
			final int newRegionY = (regionId & 0xff) * 64;
			if (clipedOnly) {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMapClipedOnly()
				.addMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			} else {
				World.getRegion(tile.getRegionId())
				.forceGetRegionMap()
				.addMask(plane, tile.getX() - newRegionX,
						tile.getY() - newRegionY, mask);
			}
			return;
		}
		masks[plane][x][y] = masks[plane][x][y] | mask;
	}

	public void addFloor(final int plane, final int x, final int y) {
		addMask(plane, x, y, 262144);
	}

}
