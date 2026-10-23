# Deaths Are Items

Turn the way you die into your next superpower. **Deaths Are Items** adds 14 special death rewards to Minecraft: discover a new way to die, respawn, and claim an item with an ability inspired by that death. Launch anvils, summon a drowning trident, steal hunger, or collect souls for explosive attacks.

Built for **Minecraft 1.20.1, Forge 47, and Java 17**. Each death type normally unlocks once per player in a world. Commands let you reopen one death type or reset your entire discovery history.

## Commands

These commands require **operator permission level 2** or cheats enabled in singleplayer. Run them as a player; they affect **only you**. Death type names are lowercase, and Tab suggests the available names.

| Command | What it does |
| --- | --- |
| `/deathsareitems reset <type>` | Resets one death discovery so your next death of that type can offer its reward again. |
| `/deathsareitems reset all` | Resets all 14 death discoveries for you. |
| `/deathsareitems reset` | Shortcut for `reset all`. |
| `/deathsareitems preview <type>` | Starts that reward's chest presentation without dying or changing your discovery history. You can claim its selectable reward. |
| `/deathsareitems preview` | Starts the Fall Damage Boots presentation. |
| `/deathsareitems die <type>` | Immediately kills you using that death type, bypassing reward equipment and immunity. Normal discovery, inventory loss, and final-life rules still apply. |
| `/give @s deathsareitems:<item_id>` | Gives you an item directly. Item IDs are listed below. |

**Already died that way and no choices appeared?** For example, use:

```mcfunction
/deathsareitems reset fall
/deathsareitems die fall
```

Respawn to see the Fall Damage Boots choices again. To reopen every death type, use:

```mcfunction
/deathsareitems reset all
```

A reset updates your discovery history immediately and is saved with the world. It does not spawn chests by itself: die that way again, or use `preview` to start a presentation immediately. Existing items, pending chest offers, and rewards already selected for delivery are preserved. A reset does not revive you or remove the End final-life rule described below. An unknown death type displays an error and leaves your history unchanged.

Available death types:

```text
fall, drown, suffocate, skeleton, golem, starve, anvil,
cram, cactus, tnt, lava, bed, pearl, wither
```

## Requirements and installation

- **Minecraft Java Edition 1.20.1**.
- **Minecraft Forge 47.x** for Minecraft 1.20.1. The project's build uses **47.1.3**.
- **Java 17** to run this Minecraft version and build the project.
- **No additional library mods are required.** The abilities, particles, animations, mob shrinking, and extra hunger display are included.

Install Forge, place the mod JAR in your instance's `mods` folder, and launch the Forge profile. For multiplayer, install the same mod version on **both the server and every player's client**. All reward items are also available in the mod's Creative inventory tab.

## How death rewards work

1. **Discover a supported death.** The first death of each type records a discovery for your player in that world.
2. **Respawn and find open ground.** A reward notification, sound, and confetti announce the unlock. After about **3.5 seconds**, three chests rise roughly three blocks away from your current position.
3. **Wait for the chests to finish opening.** Each has a different label. Only the actual reward listed below can be claimed; the other two labels are decorative choices that give no item.
4. **Right-click the reward chest.** The chests disappear, a short item reveal plays, and the item arrives after about **two seconds**. If your inventory is full, it drops as an item reserved for you.

Other players cannot take your reward. Presentations need room and solid ground; if the area is too cramped, move into an open space and the mod retries. Reward chests use temporary placements that restore the original terrain when cleaned up. Logging out, changing dimensions, or moving far away restarts an existing presentation near you. Pending rewards and discovery history survive world saves and server restarts.

**Repeating a discovered death does not offer another reward.** Unsupported deaths also give no reward. After respawning, you see **“find a different death...”** for five seconds. Use a reset command if you want to earn the same reward again. Dying again before claiming an offer can replace it with the new death's outcome, so claim your reward before experimenting further.

## All death types and rewards

| Die from… | Command type | Reward | Item ID |
| --- | --- | --- | --- |
| Fall damage | `fall` | Fall Damage Boots | `fall_damage_boots` |
| Drowning | `drown` | Drowning Mask | `drowning_mask` |
| Suffocating inside blocks | `suffocate` | Suffocation Boulder | `suffocation_boulder` |
| A skeleton's arrow | `skeleton` | SkeletonBowSkeletonBowSkeletonBow | `skeleton_bow` |
| An iron golem | `golem` | Mob Pickaxe | `mob_pickaxe` |
| Starvation | `starve` | Hunger Staff | `hunger_staff` |
| A falling anvil | `anvil` | Swarm of Anvils | `swarm_of_anvils` |
| Entity cramming | `cram` | Entity Crammer | `entity_crammer` |
| A cactus | `cactus` | Cactus Chestplate | `cactus_chestplate` |
| A primed TNT explosion | `tnt` | TNT Cannon | `tnt_cannon` |
| Direct lava damage | `lava` | Fire Pants | `fire_pants` |
| An exploding bed or respawn anchor | `bed` | Bedrock | `bedrock` |
| Ender pearl teleport damage | `pearl` | Ender Pearl Shield | `ender_pearl_shield` |
| The Wither status effect | `wither` | Soul Storm | `soul_storm` |

The final damage source determines the reward. Burning after leaving lava is different from direct lava damage; a creeper explosion does not count as TNT; and a Wither boss attack only counts for Soul Storm if the Wither effect itself kills you. Ender pearl landing damage is tracked separately from ordinary fall damage. First login and leaving the End through its exit portal are not deaths.

## Using your rewards

“Right-click” means your normal **Use Item** control, and “sneak” means your normal Sneak control. Most targeted abilities work on mobs, excluding players, tamed animals, the Ender Dragon, and the Wither.

### Fall Damage Boots

**Wear them in your feet slot.** They prevent fall damage. Normal landings create a 3×3 cushion in empty spaces, cycling through **water → slime → hay**. Each pair remembers its place in the cycle.

**Sneak while landing** to launch nearby breakable ground blocks as flying debris. These are real blocks removed from the ground; they shatter on landing without placing themselves again or dropping items. Existing blocks are not overwritten by the normal landing cushions.

### Drowning Mask

**Wear it in your head slot.** It keeps your air full and grants **Conduit Power I, Dolphin's Grace I, and Water Breathing I**, on land as well as underwater. Removing it removes its effects.

With **both hands empty**, press and hold right-click to form a glowing trident. Aim, then release to throw it. A mob it hits suffers a five-second drowning attack with bubbles and sounds, even on dry land. The summoned trident cannot be picked up. Start a fresh right-click after emptying your hands if you were already holding the button.

### Suffocation Boulder

**Right-click a block** to mark an area. Three seconds later, a spherical region becomes either sand or gravel. Each use chooses one material for the entire region.

**Right-click or hit a mob** to trap it in a matching cage that holds it still and suffocates it. Break one cage block to remove the rest and free the mob. The special sand and gravel can be broken with one hit and do not collapse just because you walk or jump nearby.

### Skeleton Bow

**Hold right-click** to fire an arrow every two ticks. It needs no ammunition, and its arrows cannot be picked up. Each arrow deals three damage before defenses. Its full inventory name is **SkeletonBowSkeletonBowSkeletonBow**.

### Mob Pickaxe

**Right-click** to toggle mob spawning. The colored action-bar message tells you whether it is on. While enabled, successfully mining a block spawns a random mob in that block's place. Mobs can appear in tight spaces, water, or midair, so choose where you mine carefully.

It mines at iron-pickaxe speed and deals **40 damage before armor** with a fully charged melee hit. Its 74-type mob pool excludes the Wither, Ender Dragon, and Warden; hostile mobs are excluded on Peaceful difficulty.

### Hunger Staff

**Right-click a mob** to drain it for five seconds. It glows red and shrinks each second, including its collision size. Food particles fly toward you and restore a total of **10 food points**: five normal hunger icons' worth.

Your normal hunger bar fills first. Overflow becomes an **extra hunger reserve**, displayed above your hearts. That reserve is used before your normal food bar falls and can hold up to **100 food points**. The target's smaller size persists.

### Swarm of Anvils

**Right-click a block or mob** to cover it in anvil strikes for 15 seconds. A targeted mob is held still during the effect.

At the end, a targeted block breaks and its dropped item receives **Infinity 255**. A targeted mob's loot receives random **level-255 enchantments**.

### Entity Crammer

**Right-click a mob** to build a narrow, open-topped glass cage. The cage fills with copies of that mob, including its equipment, until it exceeds the world's cramming limit. If cramming is disabled, the effect uses a limit of 25 mobs.

The cage bursts, throws the mobs outward, and kills them shortly afterward. Breaking its glass can trigger the burst early. Temporary glass restores the blocks it replaced; protected blocks and containers are respected.

### Cactus Chestplate

**Wear it in your chest slot** for **six armor points**. A mob that hits you is pierced by cactus spikes, pinned in place, and damaged every half-second. This retaliation activates automatically.

### TNT Cannon

**Right-click** to fire primed TNT in the direction you are looking. It follows an arc and explodes on landing or after three seconds. You can fire every **0.6 seconds**.

Shoot downward to launch yourself upward. You are protected from your own cannon shot exploding close beneath you. Other TNT and subsequent fall damage can still hurt you; Fall Damage Boots help with the landing. The explosions can damage terrain.

### Fire Pants

**Wear them in your leg slot** for **five armor points** and continuous **Fire Resistance I**. Sprint along the ground to build speed over three seconds, up to **150% extra movement speed**.

At full speed, you step up one-block obstacles and turn ordinary ground beneath your feet into obsidian. That ground becomes **real lava 1.5 seconds later**. The pants also let you cross lava on temporary obsidian that returns to lava after you leave. Your trail can change the world permanently.

### Bedrock

**Place it like a normal two-block bed, then right-click it.** In any dimension, it creates a powerful, fiery explosion while remaining in place for reuse. Its cooldown is **1.5 seconds**.

The player activating it takes no damage or knockback from that explosion. Nearby terrain can be destroyed. The item itself survives fire and lava.

### Ender Pearl Shield

**Hold it in either hand and right-click** to throw a special ender pearl and teleport without pearl damage. Holding the shield also protects you from normal ender pearl damage.

When a mob hits you while you hold it, **60 pearls** burst from that mob. Their impacts repeatedly teleport and damage it until the final strike kills it. If you also wear the Cactus Chestplate, the shield's retaliation takes priority.

### Soul Storm

**Sneak and hold right-click** to gather souls from visible eligible mobs within **50 blocks**, affecting up to 24 at a time. The targets take damage and shrink while white souls flow toward you. Release right-click or stop sneaking to stop gathering.

Arriving souls increase the count shown above your hotbar while you hold the item. Souls are stored **in the item**, so they survive saving, reconnecting, and handing it to another player. Dropping the item, dying, leaving, or changing dimensions cancels souls still in flight.

**Right-click without sneaking** to fire a black-and-purple soul attack. It spends one soul for each four blocks traveled, up to **96 blocks**, and explodes on impact or when its range or souls run out. A nearby hit costs at least one soul; you cannot fire with zero. A direct hit adds **24 damage** to a power-four explosion. Shots have a **0.6-second** cooldown.

These attacks can break obsidian, including End towers. You are protected from your own soul explosion's damage and knockback.

## Keeping your death protection

Resetting a discovery does not remove an item's protection. To repeat a natural death after a reset, remove the relevant item or use `/deathsareitems die <type>` to bypass immunity.

| Protection is active when… | Items and protection |
| --- | --- |
| Worn in the correct armor slot | Fall Damage Boots: fall damage. Drowning Mask: drowning. Cactus Chestplate: cactus damage. Fire Pants: fire resistance. |
| Carried anywhere in your inventory | Suffocation Boulder: suffocation. Skeleton Bow: skeleton arrows. Mob Pickaxe: iron golems. Hunger Staff: starvation. Swarm of Anvils: falling anvils. Entity Crammer: cramming. Bedrock: exploding beds and anchors. Soul Storm: Wither damage. |
| Held in either hand | Ender Pearl Shield: pearl teleport damage and its reactive ability. |
| Your own cannon shot explodes close beneath you | TNT Cannon: protection for that launch; it does not grant general TNT immunity. |

## The End and your final life

**Entering the End for the first time starts your personal final life.** This remains active after leaving the End, saving, or reconnecting.

Your next actual death gives no death reward. When you respawn, you become a **spectator**, and reconnecting does not restore survival. This applies individually to each player; it does not change the world's global Hardcore setting. Leaving through the End exit portal is not a death.

The discovery reset commands only reopen death rewards; they do not clear this final-life state. The `die` command is a real death and follows this rule too.

## World effects and settings

Several abilities change terrain or spawn mobs. Use them where you are comfortable with their effects: sneak landings remove blocks, explosions destroy terrain, and Fire Pants leave lava behind. Terrain abilities check world boundaries, spawn protection, Forge break/place permissions, and protected blocks or containers as appropriate. Adventure mode prevents terrain editing.

The common configuration file is `config/deathsareitems-common.toml`:

| Setting | Default | Effect |
| --- | --- | --- |
| `landingCushions` | `true` | Enables the boots' water, slime, and hay landing cushions. |
| `launchTerrain` | `true` | Enables terrain launching on sneak landings. |
| `maxLaunchedBlocks` | `9` | Maximum launched blocks per landing; allowed range: 0–18. |

English and Turkish game text are included. Ability controls follow your Minecraft key bindings.

## Building and testing

With Java 17 installed, use the included Gradle wrapper:

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

The build writes the mod JAR to `build/libs/`. On Windows, use `gradlew.bat`. Resource generation is available with `./gradlew runData`.

The Forge GameTests cover death matching, repeat discoveries, reset commands and persistence, reward ownership and delivery, equipment protection, mob abilities, terrain effects, hunger reserves, soul collection, and the End final-life rule. Source code is organized under `reward/`, `ability/`, `command/`, `client/`, and `item/custom/`. Soul Storm asset notes are in [docs/soul-storm-assets.md](docs/soul-storm-assets.md), and asset generation scripts are in `tools/`.
