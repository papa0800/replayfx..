package com.replayfx.camera;

import net.minecraft.client.network.ClientPlayerEntity;

/**
 * A lightweight "noclip fly" free camera.
 * <p>
 * How it works: while active, we set the local player's flight abilities
 * on (so normal WASD + Space/Shift + mouse-look controls fly it, exactly
 * like Creative flight) and additionally set {@code Entity#noClip} so it
 * passes through blocks instead of colliding with them. Turning it off
 * restores whatever flight/noclip state the player had before.
 * <p>
 * This is intentionally the simple, robust option rather than a true
 * detached render-camera (which needs a mixin into {@code Camera#update}
 * and is much more fragile across Minecraft versions). Because it's real
 * flight, it works everywhere a normal fly move would -- but note it moves
 * your actual player entity, so:
 * <ul>
 *   <li>In Survival on a real server, the server may reject/rubber-band
 *       you unless you have flight/noclip permission there too.</li>
 *   <li>In Singleplayer or on a server where you already have Creative/
 *       Spectator or fly permission, it works cleanly.</li>
 * </ul>
 */
public class FreeCamController {
    private boolean active;

    private boolean prevFlying;
    private boolean prevAllowFlying;
    private boolean prevNoClip;

    public boolean isActive() {
        return active;
    }

    public void toggle(ClientPlayerEntity player) {
        if (player == null) return;
        if (active) {
            disable(player);
        } else {
            enable(player);
        }
    }

    private void enable(ClientPlayerEntity player) {
        prevFlying = player.getAbilities().flying;
        prevAllowFlying = player.getAbilities().allowFlying;
        prevNoClip = player.noClip;

        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.noClip = true;

        active = true;
    }

    private void disable(ClientPlayerEntity player) {
        player.getAbilities().flying = prevFlying;
        player.getAbilities().allowFlying = prevAllowFlying;
        player.noClip = prevNoClip;

        active = false;
    }

    /** Forces free cam off without toggling, e.g. on disconnect. */
    public void forceDisable(ClientPlayerEntity player) {
        if (active && player != null) {
            disable(player);
        }
        active = false;
    }
}
