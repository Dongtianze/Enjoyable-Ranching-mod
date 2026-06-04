# 牧场之趣 (Enjoyable Animal Husbanding)

一款为 Minecraft 1.20.1 Forge 开发的模组，为游戏中的动物添加性别机制、饱食度系统与更真实的养殖体验。

作者：**brodong**

---

## 已实现功能

### 性别系统
- 每只动物在生成时随机获得 **雄性（Male）** 或 **雌性（Female）** 性别。
- 性别数据通过 `PersistentData`（NBT）持久化，重新加载存档后保持不变。
- 通过自定义网络包（`SimpleChannel`）同步到客户端，确保渲染指示器与服务器数据始终一致。
- 只有异性动物之间才能繁殖，同性别配对在 `BabyEntitySpawnEvent` 中被拦截取消。

### 性别指示器
- 在动物头顶渲染 ♂（蓝色）或 ♀（粉色）符号。
- 使用 `RenderLivingEvent.Post`（与原版命名牌相同阶段）渲染，确保始终正对玩家且位置正确。
- 幼年动物和隐身动物不显示。
- 可通过配置文件开关（`showGenderIndicator`）。

### 饱食度系统
- 每只动物拥有 0~20 的饱食度值。
- **自然衰减**：每 tick 有 1/20 概率减少 1 点。
- **饥饿扣血**：饱食度为 0 时每秒扣除 1 点生命值。
- **饱腹回血**：饱食度 > 15 时每 3 秒回复 1 点生命值。
- **进食检测**：
  - 玩家喂食（繁殖物品）→ 检测 `inLoveTime` 跳变 → 饱食度回满。
  - 羊吃草 → 检测 `Sheep.isSheared()` 从无毛到长毛 → 饱食度回满。
- 饱食度数据全程存于 `PersistentData`，世界重载后保留。

### 检查棒
- 新道具：**检查棒**。合成：**1 木棍 + 1 钻石**（无序合成）。
- **右键动物** → 聊天栏显示性别 + 饱食度（如 `牛 | 性别：雌性 | 饱食度：15/20`）。
- **右键玩家** → 显示原版饥饿值（如 `Steve | 饥饿值：18/20`）。
- 在创造模式的 **工具** 一栏可找到。

---

## 安装

1. 安装 **Minecraft Forge 1.20.1**（版本 47.x 及以上）。
2. 从 [Releases](https://github.com/brodong/enjoyable-animal-husbanding/releases) 下载模组 `.jar` 文件。
3. 将 `.jar` 文件放入 Minecraft 的 `mods/` 文件夹。
4. 使用 Forge 配置文件启动游戏。

### 从源码构建

```bash
git clone https://github.com/brodong/enjoyable-animal-husbanding.git
cd enjoyable-animal-husbanding
./gradlew build
```

编译好的 jar 位于 `build/libs/` 目录。

---

## 使用说明

### 繁殖
- 用对应的繁殖物品（如小麦喂牛）喂养两只**异性**动物即可。
- 同性别动物无法繁殖。不确定动物性别时，可使用**检查棒**右键查看。

### 检查棒
- 合成：**1 木棍 + 1 钻石**（工作台任意位置）。
- **右键动物** — 显示性别与饱食度。
- **右键玩家** — 显示饥饿值。

### 饱食度管理
- 保持动物饱食度 > 15 可持续回血。
- 饱食度为 0 的动物会持续扣血直至死亡。
- 及时喂食或让羊在草地上自行进食来恢复饱食度。

### 配置文件
配置文件：`config/enjoyable_animal_husbanding-common.toml`
| 选项 | 默认值 | 说明 |
|---|---|---|
| `showGenderIndicator` | `true` | 是否在动物头顶显示 ♂/♀ 符号 |

---

## 技术架构

本项目完全不依赖 Mixin，全部功能基于纯 Forge API 实现：

| 功能 | Forge 机制 |
|---|---|
| 性别存储 | `Entity#getPersistentData()`（NBT） |
| 性别同步 | `SimpleChannel` 网络包 + 客户端 UUID 缓存 |
| 性别指示器渲染 | `RenderLivingEvent.Post`（与命名牌同阶段） |
| 饱食度存储与计时 | `PersistentData` + `LivingEvent.LivingTickEvent` |
| 同性别繁殖拦截 | `BabyEntitySpawnEvent` 事件取消 |
| 喂食检测 | `inLoveTime` 变化跟踪 + `Sheep.isSheared()` 变化跟踪 |
| 实体生成初始化 | `EntityJoinLevelEvent` |

---

## 开发目标

### 性别与繁殖
- [x] 实现性别属性与异性繁殖
- [ ] 胎生动物怀孕状态
- [ ] 卵生动物孵蛋机制

### 基因与性状
- [ ] 基因序列、性状遗传
- [ ] 后代组合亲代基因
- [ ] 随机基因突变

### 饲养与生命
- [x] 饱食度系统（衰减、饥饿扣血、饱腹回血、进食检测）
- [ ] 饲槽方块
- [ ] 饱食度驱动寻偶（替代原版 inLove 逻辑）
- [ ] 动物最高寿命

### 群居与生活
- [ ] 群体行为机制
- [ ] 鞭子放牧工具
- [ ] 狗牧羊 AI

### 更多细节
- [ ] 动物粪便与骨粉机制
- [ ] ……（待头脑风暴）

---

## 许可协议

本项目使用 **MIT** 协议开源。

---

## 致谢

- **作者**：brodong
- 基于 [Minecraft Forge](https://minecraftforge.net/) 构建，Minecraft 版本 1.20.1
