package com.rs.world.npc.others;

import com.rs.world.Hit;
import com.rs.world.WorldTile;
import com.rs.world.npc.NPC;

@SuppressWarnings("serial")
public class Thunder extends NPC {

    public Thunder(final int id, final WorldTile tile,
                   final int mapAreaNameHash, final boolean canBeAttackFromOutOfArea,
                   final boolean spawned) {
        super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
        setCapDamage(1000);
    }

    @Override
    public double getMagePrayerMultiplier() {
        return 0.6;
    }

    @Override
    public double getRangePrayerMultiplier() {
        return 0.8;
    }

    @Override
    public double getMeleePrayerMultiplier() {
        return 0.5;
    }

    @Override
    public void handleIngoingHit(final Hit hit) {
        super.handleIngoingHit(hit);
        if (hit.getLook() != Hit.HitLook.MELEE_DAMAGE
                && hit.getLook() != Hit.HitLook.RANGE_DAMAGE
                && hit.getLook() != Hit.HitLook.MAGIC_DAMAGE)
            return;
        if (hit.getSource() != null) {
            final int recoil = 50;
            if (recoil > 0) {
                hit.getSource().applyHit(
                        new Hit(this, recoil, Hit.HitLook.REFLECTED_DAMAGE));
            }
        }
    }

}