# Enjoyable Animal Husbanding

A Minecraft 1.20.1 Forge mod that adds gender mechanics, a satiety system, and more realistic animal husbandry.

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
- Baby and invisible entities are skipped.
- Can be toggled via config (`showGenderIndicator`).

### Satiety System
- Each animal has a satiety value from 0 to 20.
- **Decay**: 1/20 chance per tick to decrease by 1.
- **Starvation**: At 0 satiety, the animal takes 1 damage per second.
- **Regeneration**: Above 15 satiety, the animal heals 1 HP every 3 seconds.
- **Feeding**: Automatically refills to max when an animal is fed (detected via `inLoveTime`) or when a sheep eats grass (detected via `Sheep.isSheared()` change).
- All satiety data is stored in `PersistentData` and persists across world reloads.

### Check Stick
- New item: **Check Stick**. Crafting: **1 Stick + 1 Diamond** (shapeless).
- Right-click an **animal** → displays gender and satiety in chat.
- Right-click a **player** → displays their vanilla food level.
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
- Feed two animals of **opposite gender** with their breeding item (e.g. wheat for cows).
- Same-gender animals will not breed. Use the **Check Stick** to verify genders.

### Check Stick
- Craft: **1 Stick + 1 Diamond** (any arrangement).
- **Right-click an animal** → shows gender + satiety (e.g. `Cow | Gender: Female | Satiety: 15/20`).
- **Right-click a player** → shows vanilla food level (e.g. `Steve | Food: 18/20`).
- Crafting recipe and creative tab location included.

### Satiety Management
- Keep animals well-fed to maintain health regeneration (satiety > 15).
- Starving animals (satiety = 0) will slowly die.
- Feed animals their breeding item or let sheep graze on grass to refill satiety.

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
| Feeding detection | `inLoveTime` change tracking + `Sheep.isSheared()` change tracking |
| Entity spawn init | `EntityJoinLevelEvent` |
| Render layer registration | (Removed — replaced by `RenderLivingEvent.Post`) |

---

## Roadmap

- [x] Gender system (Male/Female)
- [x] Opposite-gender breeding restriction
- [x] Gender indicator rendering
- [x] Check Stick item (gender + satiety + player food)
- [x] Satiety system (decay, starvation, regen, feeding detection)
- [ ] Pregnancy system for live-bearing animals
- [ ] Egg incubation for egg-laying animals
- [ ] Genetic traits and inheritance
- [ ] Feeding trough block
- [ ] Maximum lifespan for domestic animals
- [ ] Herding behavior and group mechanics
- [ ] Whip tool for herding
- [ ] Sheepdog AI for dogs
- [ ] Manure and composting system

---

## License

This project is licensed under the **MIT** License.

---

## Credits

- **Author**: brodong
- Built with [Minecraft Forge](https://minecraftforge.net/) for Minecraft 1.20.1
