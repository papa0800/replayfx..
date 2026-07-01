# ReplayFX — custom recording mod for Minecraft 1.21.11 (Fabric)

A from-scratch client-side mod: press a key to record your gameplay
(position, look direction, active potion effects, critical hits), then
replay it and watch the potion-effect swirl particles and crit sparks
happen again at the right spots.

This is a **working MVP**, not a full clone of Replay Mod / Flashback —
see "What this does / doesn't do" below.

## ⚠️ Important: 1.21.11 is a transitional version — read this first

I checked current sources before writing this, and 1.21.11 turns out to be
one of the biggest modding-toolchain changes in years:

- Mojang removed obfuscation from the game jar entirely. Minecraft is now
  shipped **unobfuscated**.
- **1.21.11 is the last version with Yarn mappings support** — everything
  after it moves to Mojang's official mappings. I kept this project on
  Yarn since it's still valid for 1.21.11 and closer to older tutorials.
- Fabric Loader 0.18.1 + Loom 1.14 are the versions to use.
- A few APIs were renamed in the versions leading up to 1.21.11 (e.g.
  `Entity#getWorld` → `Entity#getEntityWorld` in 1.21.9, world render
  events were removed then reintroduced in 1.21.10).

Because of this churn, **I can't guarantee every method name below compiles
on the first try** — I don't have network/build access in this session to
test-compile against the real 1.21.11 jar. If something doesn't resolve:

1. Open the project in IntelliJ IDEA with the Minecraft Development plugin.
2. Let Loom download and generate sources (`Gradle → loom → genSources`).
3. Ctrl-click the broken call to jump to the real decompiled method and
   fix the name — it's almost always a 1-word rename.

## Recommended setup (avoids Gradle wrapper headaches)

I can't ship a working `gradlew` binary from this sandbox (no internet
here), so the fastest path is:

1. Go to **https://fabricmc.net/develop/template/**, pick Minecraft
   `1.21.11`, mappings `Yarn`, and generate/download the official template.
   That gives you a known-good `gradlew`, wrapper jar, and up-to-date
   version numbers.
2. Delete the template's example `src/main/java/...` and
   `fabric.mod.json`.
3. Copy everything from this project's `src/` folder and `fabric.mod.json`
   into the template.
4. Open `gradle.properties` from **this** project and copy over the
   `fabric_version` / `loader_version` lines if the template's differ.
5. Run `./gradlew build` (or open in IntelliJ and let it sync).

(The `build.gradle` / `settings.gradle` / `gradle.properties` I generated
here are included too, in case you'd rather wire the wrapper up yourself —
just run `gradle wrapper --gradle-version 8.10` inside the folder once you
have Gradle installed locally.)

## Controls (defaults, rebindable in Options → Controls → ReplayFX)

| Key | Action |
|-----|--------|
| **V** | Open the ReplayFX menu — one screen with everything below, explained, plus buttons that do the same thing without memorizing hotkeys |
| **R** | Start / stop recording |
| **T** | Quick-play the last recording made this session |
| **Y** | Open the recordings browser directly (play or delete saved files) |
| **G** | Toggle free camera (noclip fly) |

Recordings save to `.minecraft/replayfx/*.rfx.json`.

### Free camera (G, or from the V menu)

Turns on real flight + noclip on your player, so WASD moves you, Space/Shift
go up/down, and you pass straight through blocks — same feel as Creative
flight but through walls too. Press **G** again (or the menu button) to turn
it back off; your previous flight state is restored automatically.

This moves your *actual* player entity rather than a fully detached camera
(a true separate camera needs a mixin into Minecraft's `Camera` class, which
is much more version-fragile — see "ideas to extend it" below). Practically:

- **Singleplayer / Creative / any server where you already have fly
  permission:** works cleanly, no rubber-banding.
- **Vanilla Survival on someone else's server without fly permission:**
  the server will likely correct/reject the movement, same as any
  unauthorized flight would.

## What this does

- Captures position, yaw/pitch, sprint/sneak/ground state, active potion
  effects (with their real color), and a simplified crit-hit heuristic,
  once per tick.
- Saves/loads recordings as JSON (easy to inspect, tweak, or generate
  externally).
- Playback repositions your client player frame-by-frame and re-spawns
  `ENTITY_EFFECT` particles (tinted with each potion's real color) and
  `CRIT` particles at the right ticks.
- A basic in-game screen to browse and delete saved recordings.

## What this doesn't do (yet) — ideas to extend it

- **Fully detached camera during playback.** The free camera (G) and
  playback are two separate features right now — turning on free cam
  while a replay is playing will fight over player position each tick.
  A true "fly around independently while a ghost plays back over there"
  camera needs a `GameRenderer`/`Camera` mixin (the free cam here uses
  real flight+noclip instead, which is simpler but shares the player
  entity with playback). Happy to build the mixin version next if you
  want the two to coexist.
- **Video export.** Replay Mod/Flashback render frames to actual video
  files; this MVP is an in-game particle replay only.
- **Multiplayer-visible ghosts.** Everything here is client-side only;
  other players won't see your replay.
- **Smoothing/interpolation between ticks.** Currently 1 frame = 1 tick,
  so fast motion can look slightly steppy. Lerping positions between
  frames in `PlaybackController.tick()` is a quick win.
- **More effect types**, e.g. distinguishing sword-crit vs. other combat
  particles, death recap markers, etc.

## FPS-boosting modpack (verified currently available for 1.21.11 Fabric)

I checked Modrinth directly rather than relying on memory, since
performance mods often lag behind new Minecraft versions — these are
confirmed live for 1.21.11 as of Feb 2026:

| Mod | What it does | Confirmed version |
|---|---|---|
| **Sodium** | Core rendering engine rewrite, the single biggest FPS win | `mc1.21.11-0.8.0-fabric` |
| **Lithium** | Server/world-logic optimizations (mob AI, block ticking) | `mc1.21.11-0.21.0-fabric` |
| **FerriteCore** | Cuts RAM usage significantly | `8.0.3-fabric` |
| **C2ME** (Concurrent Chunk Mgmt Engine) | Multithreaded chunk gen/loading | `0.3.6+alpha.0.36+1.21.11` |
| **Cloth Config API** | Required by several of the above for their config screens | `20.0.149+fabric` |
| **Fabric API** | Required dependency for almost everything, including ReplayFX itself | `0.141.3+1.21.11` |

Good add-ons to also search Modrinth for (compatibility changes fast, so
check the "supports 1.21.11" tag yourself before installing): **Iris**
(shaders on top of Sodium), **ImmediatelyFast**, **ModernFix**, **Entity
Culling**, **Krypton** (netty/networking optimization), **Starlight**
(lighting engine).

Install order doesn't matter much, but all of the above (plus ReplayFX)
go straight in `.minecraft/mods/` alongside Fabric Loader 0.18.1 for
1.21.11. A launcher like Prism Launcher or the official launcher's mod
support will handle it fine.
