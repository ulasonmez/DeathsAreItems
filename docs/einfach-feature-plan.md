# EinfachGustaf feature plan

The starred EinfachGustaf gameplay additions below are implemented on the einfach
branch alongside the original 14 death rewards. Custom models/textures and the supplied book title/cover are integrated.
Editable assets and validation details are in art/models/einfach/README.md. See [einfach-content.md](einfach-content.md)
for registered IDs, final visual assets, controls and implementation details.

* EinfachGustaf additions: paragraphs marked with * describe implemented gameplay for
  this edition. Custom art and the supplied reference assets are integrated. The existing
  instructions and 14 death rewards remain the foundation of the mod.

* Discover different ways to die, then turn those deaths into an increasingly
  powerful loadout. Collect the personal rewards below before entering The End;
  none of them removes the End Hardcore death lock or enables reward chests
  after End Hardcore has started.

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

   * Viny's Bucket is a bonus alongside your first Drowning Mask. Right-click
     water to summon Viny, your named axolotl companion; sneak-right-click to
     recall him. While you wear the mask, Viny traps one nearby hostile mob
     within 8 blocks in a water bubble for 4 seconds, once every 15 seconds.
     This holds a target still for your mask's trident. Bosses take water damage
     instead of being trapped. Viny follows through water and returns to his
     bucket on dry land rather than suffocating.

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
  alongside the tenth reward. The supplied model and unchanged cover are used; all six language files name
  it EinfachGustaf und die Wächter der Stille, with spaces in the display name.
  The book turns your discovered deaths into an OP spell collection. Its five
  combat chapters unlock through the matching discoveries: fall slams a targeted
  mob into the ground, drowning traps it in a damaging water bubble, anvil calls
  a focused anvil barrage, cactus pins it in a damaging thorn ring, and wither
  fires a soul burst. Undiscovered chapters stay locked. Sneak-right-click to
  cycle unlocked chapters; right-click to cast at a mob within 24 blocks.
  The book has no casting cooldown and can replace the reader's own active spell.
  Each chapter grants its own eight-second blessing. Three distinct chapters on
  the same target within eight seconds unleash Silent Finale: a six-block soul
  wave dealing 16 damage and slowing non-boss mobs for three seconds. Holding the
  book in either hand reveals mobs within twelve blocks. These are separate
  book spells, so casting never activates
  an equipped death item's ability a second time. Bosses take damage without
  forced movement or cages. The book remains reusable and cannot revive players.

  All chapters, the finale and sight also affect passive, neutral and tamed
  mobs, including Juky and Viny. Other death items still protect companions.

* The jukebox and book milestones count distinct death rewards actually claimed
  by each player, in any order. Repeating a death, previewing a chest, summoning
  pets, and crafting or giving items do not advance them. Each milestone and
  companion bonus is granted once per player; later deaths do not farm copies.
  These additions accompany the normal successful reward, without replacing
  its item or changing the reward chest presentation. Unlock the companions,
  stock up on cheese and hamburgers, and choose your death gear and book chapters
  before entering The End. The original End Hardcore rules still decide what
  happens if you die there.
