================================================================================
DEATHS ARE ITEMS - FORGE 1.20.1 MOD
================================================================================

================================================================================
INSTALLATION & LANGUAGES
================================================================================

Requires Minecraft Java Edition 1.20.1, Minecraft Forge 47.x, and Java 17.
This build uses Forge 47.1.3. Extract deathsareitems-1.0.0.jar from the ZIP and
place it in the mods folder of your Forge installation. For multiplayer, install
the same JAR on the server and on each player's client.

Select your language in Minecraft's language settings. Included translations:
English (US), Turkish, German, Korean, Portuguese (Brazil), Portuguese (Portugal).
Reward names and jokes are adapted to each language and death theme. For example,
Bedrock appears as "Taş Gibi Yatak" in Turkish and "Cama de Pedra" in Portuguese.
Each player sees their own selected language. Commands and item IDs stay the same.

* EinfachGustaf additions: paragraphs marked with * are planned features for
  this edition, not features implemented in the current JAR. The existing
  instructions and 14 death rewards remain the foundation of the mod.

* Discover different ways to die, then turn those deaths into an increasingly
  powerful loadout. Collect the personal rewards below before entering The End;
  none of them removes the End Hardcore death lock or enables reward chests
  after End Hardcore has started.

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

* Birch Is Useless: breaking a birch log drops only worthless Birch Splinters
  instead of usable wood and deals 1 heart of splinter damage to the miner.
  Splinters have no crafting, food, or fuel use. Other wood types work normally,
  so avoid birch when gathering supplies or building your first death setups.
  This is a bad-resource gag, not an extra death discovery or an OP reward.

* Don't Press the Button: manually pressing any placed button gives the player
  a loud ticking warning and spawns one primed TNT beside that button with a
  3-second fuse. The TNT damages entities but does not destroy terrain. That
  button cannot trigger another TNT until its current one explodes; redstone
  and projectile activation do not trigger this extra effect. Escape the blast,
  or deliberately use it to discover the existing TNT death and earn the cannon.
  There is no separate button death type or extra reward for pressing buttons.

1. Fall Damage - Fall Damage Boots
   Wear in feet slot. Negates all fall damage. Landing creates a 3x3 cushion
   cycling through water -> slime -> hay. Sneak while landing to launch
   breakable ground blocks into the air as flying debris.

2. Drowning - Drowning Mask
   Wear in head slot. Grants Water Breathing, Dolphin's Grace, and Conduit Power.
   With both hands empty, hold and release right-click to throw a trident that
   inflicts a drowning attack on target mobs.

   * Viny's Bucket is a bonus alongside your first Drowning Mask. Right-click
     water to summon Viny, your named axolotl companion; sneak-right-click to
     recall him. While you wear the mask, Viny traps one nearby hostile mob
     within 8 blocks in a water bubble for 4 seconds, once every 15 seconds.
     This holds a target still for your mask's trident. Bosses take water damage
     instead of being trapped. Viny follows through water and returns to his
     bucket on dry land rather than suffocating.

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

   * Juky's Whistle is a bonus alongside your first Mob Pickaxe. Right-click to
     summon Juky, your named dog companion; sneak-right-click to recall him.
     Juky attacks hostile mobs that threaten you, including hostile results
     from the pickaxe. Sneak-right-click the summoned Juky with an empty hand
     to toggle guarding or following. His howl knocks nearby ordinary enemies
     back once every 20 seconds, giving you room when mining gets crowded.

   * Juky and Viny each have one active summon per owner. They do not attack
     players, passive animals, or owned pets, and your death-item area attacks
     ignore them. If defeated, they return to their whistle or bucket and can
     be summoned again after 30 seconds. They return when you change dimension;
     resummon them in a suitable place. Extra copies cannot create extra pets.

6. Starvation - Hunger Staff
   Right-click a mob to drain it over 5 seconds (shrinks target and restores 10
   food points). Overflow fills an extra hunger reserve bar up to 100 food points.

   * Your first starvation reward also includes 8 Cheese Slices and 2 Hamburgers:
     dying of hunger earns Gustaf his favorite food. Hold right-click to eat
     cheese even with a full hunger bar. Each slice restores
     4 food points; overflow enters the staff's existing reserve, up to its
     existing 100-point cap. Cheese also heals 1 heart, with a shared 10-second
     healing cooldown so repeated eating cannot provide instant full healing.

   * After this discovery, cook a milk bucket in a furnace to make 4 Cheese
     Slices, returning the empty bucket. Combine 2 bread, 1 cooked beef, and
     1 Cheese Slice in a shapeless recipe for 1 Hamburger. Eating a hamburger
     restores 12 food points, sends overflow into the same capped reserve, and
     grants Regeneration II for 5 seconds. Repeated hamburgers refresh that
     effect instead of stacking it. Cheese remains the easy everyday snack;
     hamburgers are preparation for dangerous death attempts and the End trip.

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

* After claiming rewards for 5 different death types, receive Gustaf's Jukebox
  and one vanilla music disc alongside the fifth reward. Place it and insert
  any music disc to start a Death Remix. While music plays, the owner gains
  Strength II and Resistance II within 16 blocks; every 5 seconds, a bass pulse
  deals 16 damage and knocks back hostile mobs in that area. It leaves terrain,
  players, and pets unharmed. A pulse can
  damage bosses but cannot launch or immobilize them. Stop the disc to stop the
  effects; break and carry the jukebox to move your powerful preparation area.
  Multiple jukeboxes do not stack effects or multiply pulse damage on one mob.

* After claiming rewards for 10 different death types, receive Gustaf's New Book
  alongside the tenth reward. Use his actual new book's title and cover for the
  final item; "Gustaf's New Book" is the working name until those are supplied.
  The book turns your discovered deaths into an OP spell collection. Its five
  combat chapters unlock through the matching discoveries: fall slams a targeted
  mob into the ground, drowning traps it in a damaging water bubble, anvil calls
  a focused anvil barrage, cactus pins it in a damaging thorn ring, and wither
  fires a soul burst. Undiscovered chapters stay locked. Sneak-right-click to
  cycle unlocked chapters; right-click to cast at a hostile mob within 24 blocks.
  All chapters share a 15-second cooldown
  across book copies. These are separate book spells, so casting never activates
  an equipped death item's ability a second time. Bosses take damage without
  forced movement or cages. The book remains reusable and cannot revive players.

* The jukebox and book milestones count distinct death rewards actually claimed
  by each player, in any order. Repeating a death, previewing a chest, summoning
  pets, and crafting or giving items do not advance them. Each milestone and
  companion bonus is granted once per player; later deaths do not farm copies.
  These additions accompany the normal successful reward, without replacing
  its item or changing the reward chest presentation. Unlock the companions,
  stock up on cheese and hamburgers, and choose your death gear and book chapters
  before entering The End. The original End Hardcore rules still decide what
  happens if you die there.
