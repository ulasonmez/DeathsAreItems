================================================================================
DEATHS ARE ITEMS - FORGE 1.20.1 MOD
================================================================================

================================================================================
COMMANDS
================================================================================

Commands require operator permission level 2 or cheats enabled:

/deathsareitems revive [player]
- Revives a player from End Hardcore death.
- Clears the hardcore spectator death lock and resets End Hardcore status.
- Restores the player's game mode back to Survival and sets hearts HUD to normal.
- Parameters:
  [player] : Target player(s) (e.g. player name, @s, @a; defaults to self if omitted)
- Alternative aliases:
  /deathsareitems reset hardcore
  /deathsareitems hardcore reset [player]

/deathsareitems reset <type|all|hardcore>
- Resets death discovery history so you can unlock the reward again, or resets End Hardcore.
- Parameters:
  <type>   : Specific death type (see list below)
  all      : Resets all 14 death discoveries (default if omitted)
  hardcore : Resets End Hardcore status and revives you to Survival

/deathsareitems preview <type>
- Previews the reward chest sequence without dying.
- Parameters:
  <type> : Specific death type (defaults to 'fall' if omitted)

/deathsareitems die <type>
- Kills you with the specified death type (bypasses immunities).
- Parameters:
  <type> : Specific death type (see list below)

/give @s deathsareitems:<item_id>
- Gives the reward item directly into your inventory.

Valid death types:
fall, drown, suffocate, skeleton, golem, starve, anvil, cram, cactus, tnt, lava, bed, pearl, wither

================================================================================
END HARDCORE CHALLENGE
================================================================================

When a player enters The End dimension:
- Personal "End Hardcore" mode is activated (hardcore hearts render on the HUD).
- Reward chests are disabled while in End Hardcore mode.
- If the player dies after entering The End, they are permanently locked into
  Spectator mode across dimensions, respawns, and server restarts.
- To revive a player and restore them to Survival mode:
  Run `/deathsareitems revive <player>` (or `/deathsareitems reset hardcore`).

================================================================================
DEATH REWARDS & USAGE
================================================================================

NOTE: Choose a wide and flat spawn point so reward chests can spawn cleanly.
If your spawn location lacks enough open, level ground, the mod will wait until
you move into an open area, or chests may spawn at uneven heights (one high, one
low) and could even get lost.

1. Fall Damage - Fall Damage Boots
   Wear in feet slot. Negates all fall damage. Landing creates a 3x3 cushion
   cycling through water -> slime -> hay. Sneak while landing to launch
   breakable ground blocks into the air as flying debris.

2. Drowning - Drowning Mask
   Wear in head slot. Grants Water Breathing, Dolphin's Grace, and Conduit Power.
   With both hands empty, hold and release right-click to throw a trident that
   inflicts a drowning attack on target mobs.

3. Suffocation - Suffocation Boulder
   Right-click a block to turn a spherical area into sand or gravel after 3
   seconds. Right-click or hit a mob to trap and suffocate it inside a cage of
   sand or gravel.

4. Skeleton Arrow - Skeleton Bow
   Hold right-click to rapid-fire infinite arrows (fires every 2 ticks, consumes
   no ammunition).

5. Iron Golem - Mob Pickaxe
   Right-click to toggle mob spawning. When enabled, mining blocks spawns a
   random mob in that block's place. Deals 40 melee damage on charged hits.

6. Starvation - Hunger Staff
   Right-click a mob to drain it over 5 seconds (shrinks target and restores 10
   food points). Overflow fills an extra hunger reserve bar up to 100 food points.

7. Falling Anvil - Swarm of Anvils
   Right-click a block or mob to rain anvils for 15 seconds (pins mobs in place).
   At the end, a targeted block drops with Infinity 255; mob drops receive random
   level-255 enchantments.

8. Entity Cramming - Entity Crammer
   Right-click a mob to trap it in a glass cage that continuously clones it until
   cramming limit kills them all, then bursts outward.

9. Cactus - Cactus Chestplate
   Wear in chest slot (6 armor points). Automatically retaliates against
   attacking mobs with cactus spikes, pinning them and dealing damage every 0.5
   seconds.

10. TNT Explosion - TNT Cannon
    Right-click to fire primed TNT along an arc (explodes on impact or after 3s).
    Shoot downwards to rocket-jump without taking blast damage from your own shot.

11. Lava - Fire Pants
    Wear in leg slot (5 armor points, Fire Resistance). Sprint to build up to
    +150% movement speed. At top speed, turns ground beneath you into obsidian
    that melts into lava after 1.5s, and allows running safely across lava.

12. Bed / Anchor Explosion - Bedrock
    Place down like a bed and right-click. Creates a powerful, fiery explosion
    every 1.5 seconds without harming the user or destroying the block.

13. Ender Pearl - Ender Pearl Shield
    Hold and right-click to throw safe ender pearls with no teleport damage.
    When hit by a mob while holding it, retaliates by unleashing 60 pearls that
    repeatedly teleport and damage the attacker until killed.

14. Wither Effect - Soul Storm
    Sneak and hold right-click to harvest souls from nearby mobs (damaging and
    shrinking them). Right-click without sneaking to fire an explosive soul
    projectile (deals heavy damage and can break obsidian).
