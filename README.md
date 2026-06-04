# Enjoyable Animal Husbanding

A Minecraft 1.20.1 Forge mod that adds gender mechanics, a satiety system, realistic breeding, and enhanced chicken husbandry.

by **brodong**

---

## Features

### Gender System
- Every animal is assigned a random gender — **Male** or **Female** — upon spawning.
- Gender data is persisted via `PersistentData` (auto-saved with the entity), surviving world reloads.
- Server-to-client sync via a custom network packet, ensuring the gender indicator always matches the server.
- Only opposite-gender pairs can breed. Same-gender breeding is blocked via `BabyEntitySpawnEvent`.

### Gender Indicator
- A ♂ (blue) or ♀ (pink) symbol rendered above each animal's head.
- Rendered at the same pipeline stage as vanilla name tags (`RenderLivingEvent.Post`), guaranteeing correct billboarding.
- Baby and invisible entities are skipped. Can be toggled via config (`showGenderIndicator`).

### Satiety System
- Each animal has a satiety value from 0 to 20.
- **Decay**: slow natural depletion over time.
- **Starvation**: at 0 satiety, the animal takes 1 damage per second.
- **Regeneration**: above 15 satiety, the animal heals 1 HP every 3 seconds.
- **Feeding**: player right-click with breeding food fills satiety to max. Sheep regenerate satiety by grazing grass.
- All satiety data persists across world reloads.

### Feeding & Love Mode Rework
- Feeding an animal **no longer directly triggers inLove** (breeding mode).
- Instead, feeding fills satiety. When satiety > 15, the animal has a 1/64 chance per tick to enter love mode.
- **Vanilla breeding cooldown (`canFallInLove`) is preserved** — animals won't re-enter love mode until cooldown expires.

### Chicken Husbandry
- **Chicken eggs replaced**: chickens no longer lay vanilla egg items. Instead:
  - **Hens (female)**: 1/8 chance to place a **Chicken Egg block**, 7/8 chance to drop 1–2 feathers.
  - **Roosters (male)**: always drop 1–2 feathers (no eggs).
- **Chicken Egg block**: functions like turtle eggs — hatches into a baby chicken over time (faster than turtle eggs). Right-click to harvest 1–3 vanilla eggs.
- **Chicken breeding blocked**: paired chickens produce **only XP orbs** (1–7 XP) instead of baby chickens. Chickens reproduce exclusively through egg hatching.

### Check Stick
- New item: **Check Stick**. Crafting: **1 Stick + 1 Diamond** (shapeless).
- Right-click an **animal** → displays gender and satiety.
- Right-click a **player** → displays vanilla food level.
- Found in the Creative Mode **Tools & Utilities** tab.

---

## Installation

1. Install **Minecraft Forge 1.20.1** (version 47.x or higher).
2. Download the mod `.jar` from [Releases](https://github.com/brodong/enjoyable-animal-husbanding/releases).
3. Place the `.jar` into your Minecraft `mods/` folder.
4. Launch with the Forge profile.

### Building from Source

```bash
git clone https://github.com/brodong/enjoyable-animal-husbanding.git
cd enjoyable-animal-husbanding
./gradlew build
```

The compiled jar will be in `build/libs/`.

---

## Usage

### Breeding
- Animals enter love mode randomly when **satiety > 15** (1/64 chance/tick).
- Feed animals their breeding food to **raise satiety**. Two animals of opposite gender in love mode will breed.
- Chickens do not produce babies through breeding — they only give XP.

### Check Stick
- Craft: **1 Stick + 1 Diamond** (any arrangement).
- **Right-click animal** → `Cow | Gender: Female | Satiety: 15/20`
- **Right-click player** → `Steve | Food: 18/20`

### Satiety Management
- Keep animals well-fed (satiety > 15) to enable health regen and love mode.
- Starving animals (satiety = 0) will slowly die.
- Feed animals their breeding food item, or let sheep graze on grass.

### Chicken Eggs
- Eggs are laid as **blocks** on the ground (not items). They look like turtle eggs.
- Egg blocks slowly crack and hatch into baby chicks (faster than turtle eggs).
- Right-click an egg block to collect 1–3 vanilla `minecraft:egg` items.

### Configuration
Config file: `config/enjoyable_animal_husbanding-common.toml`
| Option | Default | Description |
|---|---|---|
| `showGenderIndicator` | `true` | Toggle the ♂/♀ symbol above animals |

---

## Technical Architecture

This mod uses **zero Mixin** — all functionality is built on pure Forge APIs:

| Feature | Forge Mechanism |
|---|---|
| Gender storage | `Entity#getPersistentData()` (NBT) |
| Gender sync | `SimpleChannel` network packet + client UUID cache |
| Gender rendering | `RenderLivingEvent.Post` (same stage as name tags) |
| Satiety storage & timing | `PersistentData` + `LivingEvent.LivingTickEvent` |
| Same-gender breeding block | `BabyEntitySpawnEvent` cancellation |
| Chicken breeding → XP | `BabyEntitySpawnEvent` cancellation + `ExperienceOrb` |
| Feeding → satiety (not inLove) | `PlayerInteractEvent.EntityInteract` cancellation |
| Satiety → inLove | Random dice roll in `LivingTickEvent`, gated by `canFallInLove()` |
| Sheep grazing detection | `Sheep.isSheared()` change tracking |
| Chicken egg laying override | `eggTime` lock + custom `PersistentData` timer |
| Chicken Egg block | `TurtleEggBlock` subclass with custom hatching |

---

## Roadmap

- [x] Gender system (Male/Female)
- [x] Opposite-gender breeding restriction
- [x] Gender indicator rendering
- [x] Check Stick item (gender + satiety + player food)
- [x] Satiety system (decay, starvation, regen, feeding detection)
- [x] Feeding → satiety rework (no direct inLove)
- [x] Satiety-driven love mode (preserving vanilla cooldown)
- [x] Chicken egg block (turtle egg style, hatching)
- [x] Chicken breeding → XP only
- [x] Gender-based chicken output (hens lay eggs, roosters only feathers)
- [ ] Pregnancy system for live-bearing animals
- [ ] Genetic traits and inheritance
- [ ] Feeding trough block
- [ ] Maximum lifespan for domestic animals
- [ ] Herding behavior and group mechanics
- [ ] Whip tool for herding
- [ ] Sheepdog AI for dogs
- [ ] Manure and composting system

---

## License

MIT License.

---

## Credits

- **Author**: brodong
- Built with [Minecraft Forge](https://minecraftforge.net/) for Minecraft 1.20.1
