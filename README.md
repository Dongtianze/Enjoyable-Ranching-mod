# Enjoyable Animal Husbanding

A Minecraft 1.20.1 Forge mod that brings gender mechanics and realistic breeding to animals.

by **brodong**

---

## Features

### Gender System
- Every animal (`Animal`) entity is assigned a random gender — **Male** or **Female** — upon spawning.
- Gender data is persisted via NBT, so it survives world reloads.
- Only opposite-gender pairs can breed. Same-gender animals will not mate.

### Breeding AI Filtering
- Animals will only seek out partners of the opposite gender during breeding AI (`BreedGoal`).
- This works alongside the `canMate()` check for a double-layer guarantee.

### Check Stick
- A new item: **Check Stick**.
- Right-click any animal with the Check Stick to display its gender in chat.
- Crafting recipe: **1 Stick + 1 Diamond** (shapeless).
- Found in the Creative Mode **Tools & Utilities** tab.

---

## Installation

1. Install **Minecraft Forge 1.20.1** (version 47.x or higher).
2. Download the mod `.jar` file from [Releases](https://github.com/brodong/enjoyable-animal-husbanding/releases).
3. Place the `.jar` file into your Minecraft `mods/` folder.
4. Launch the game with the Forge profile.

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
- Feed two animals of **opposite gender** with their breeding item (e.g., wheat for cows).
- Same-gender animals will not breed. Use the Check Stick to verify their genders if unsure.

### Check Stick
- Craft: **1 Stick + 1 Diamond** (any arrangement in the crafting grid).
- Hold the Check Stick and **right-click** an animal to see its gender in the chat window.

### Configuration
The mod's configuration file is located at `config/enjoyable_animal_husbanding-common.toml`. Currently includes example debug options.

---

## Roadmap

- [x] Gender system with Male/Female/None
- [x] Opposite-gender breeding restriction
- [x] Breeding AI partner filtering
- [x] Check Stick item
- [ ] Pregnancy system for live-bearing animals
- [ ] Egg incubation for egg-laying animals
- [ ] Genetic traits and inheritance
- [ ] Satiety / hunger system for animals
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
