# 牧场之趣 (Enjoyable Ranching)

一款为 Minecraft 1.20.1 Forge 开发的模组，为游戏中的动物添加性别机制、饱食度系统、真实繁殖逻辑与养鸡玩法。

作者：**brodong**

---

## 已实现功能

### 性别系统
- 每只动物在生成时随机获得 **雄性（Male）** 或 **雌性（Female）** 性别。
- 性别数据通过 `PersistentData`（NBT）持久化，重新加载存档后保持不变。
- 通过自定义网络包（`SimpleChannel`）同步到客户端，渲染指示器与服务器数据始终一致。
- 只有异性动物之间才能繁殖，同性别配对在 `BabyEntitySpawnEvent` 中被拦截取消。

### 性别指示器
- 在动物头顶渲染 ♂（蓝色）或 ♀（粉色）符号。
- 使用 `RenderLivingEvent.Post`（与命名牌相同阶段）渲染，始终正对玩家且位置正确。
- 幼年动物和隐身动物不显示。可通过配置关闭（`showGenderIndicator`）。

### 饱食度系统
- 每只动物拥有 0~20 的饱食度值。
- **自然衰减**：随时间缓慢降低。
- **饥饿扣血**：饱食度为 0 时每秒扣 1 点生命。
- **饱腹回血**：饱食度 > 15 时每 3 秒回复 1 点生命。
- **进食**：玩家喂食繁殖物品 → 饱食度回满；羊吃草 → 饱食度回满。
- 饱食度数据存于 `PersistentData`，世界重载后保留。

### 喂食与寻偶逻辑重构
- 喂食**不再直接触发 inLove**（寻偶状态），而是填充饱食度。
- 饱食度 > 15 时每 tick 有 1/64 概率进入寻偶状态。
- **保留原版 `canFallInLove` 冷却机制**——刚交配过的动物需等待冷却结束才能再次寻偶。

### 养鸡玩法
- **替代原版下蛋**：鸡不再掉落原版鸡蛋物品。
  - **母鸡（雌性）**：1/8 概率生成**鸡蛋方块**，7/8 掉落 1~2 根羽毛。
  - **公鸡（雄性）**：100% 掉落 1~2 根羽毛（不生蛋）。
- **鸡蛋方块**：类似海龟蛋机制，随时间裂纹孵化出幼年鸡（孵化速度快于海龟蛋），右键可收获 1~3 个原版鸡蛋。
- **鸡繁殖产出经验**：异性鸡配对不产生幼崽，改为掉落 1~7 点经验。鸡只能通过鸡蛋方块孵化繁殖。

### 检查棒
- 新道具：**检查棒**。合成：**1 木棍 + 1 钻石**（无序）。
- **右键动物** → 显示性别 + 饱食度（如 `牛 | 性别：雌性 | 饱食度：15/20`）。
- **右键玩家** → 显示原版饥饿值。
- 在创造模式 **工具** 一栏可找到。

---

## 安装

1. 安装 **Minecraft Forge 1.20.1**（版本 47.x 及以上）。
2. 从 [Releases](https://github.com/brodong/enjoyable-ranching/releases) 下载模组 `.jar`。
3. 将 `.jar` 放入 Minecraft 的 `mods/` 文件夹。
4. 使用 Forge 配置文件启动游戏。

### 从源码构建

```bash
git clone https://github.com/brodong/enjoyable-ranching.git
cd enjoyable-ranching
./gradlew build
```

编译好的 jar 位于 `build/libs/` 目录。

---

## 使用说明

### 繁殖
- 饱食度 > 15 时动物随机进入寻偶状态（每 tick 1/64 概率）。
- 喂食繁殖物品可**提升饱食度**。异性动物同时寻偶即可繁殖。
- 鸡配对不产生幼崽，仅掉落经验值。鸡通过鸡蛋方块孵化繁殖。

### 检查棒
- 合成：**1 木棍 + 1 钻石**。
- **右键动物** — 显示性别与饱食度。
- **右键玩家** — 显示饥饿值。

### 饱食度管理
- 保持动物饱食度 > 15 可回血并触发寻偶。
- 饱食度为 0 的动物会持续扣血致死。
- 及时喂食或让羊在草地上自行进食。

### 鸡蛋方块
- 鸡蛋以**方块形式**生成在地上，外观类似海龟蛋。
- 随时间裂纹加深，最终孵化出幼年鸡。
- 右键收获 1~3 个原版 `minecraft:egg`。

### 配置文件
`config/enjoyable_ranching-common.toml`
| 选项 | 默认值 | 说明 |
|---|---|---|
| `showGenderIndicator` | `true` | 是否显示 ♂/♀ 符号 |

---

## 技术架构

完全不依赖 Mixin，全部基于纯 Forge API：

| 功能 | Forge 机制 |
|---|---|
| 性别存储 | `Entity#getPersistentData()`（NBT） |
| 性别同步 | `SimpleChannel` 网络包 + 客户端 UUID 缓存 |
| 性别渲染 | `RenderLivingEvent.Post`（与命名牌同阶段） |
| 饱食度存储与计时 | `PersistentData` + `LivingEvent.LivingTickEvent` |
| 同性别繁殖拦截 | `BabyEntitySpawnEvent` 取消 |
| 鸡繁殖→经验 | `BabyEntitySpawnEvent` 取消 + `ExperienceOrb` |
| 喂食→饱食度 | `PlayerInteractEvent.EntityInteract` 取消原版 |
| 饱食度→寻偶 | `LivingTickEvent` 中随机掷骰，`canFallInLove()` 把关 |
| 羊吃草检测 | `Sheep.isSheared()` 变化跟踪 |
| 鸡下蛋替代 | 锁定 `eggTime` + 自定义 `PersistentData` 计时器 |
| 鸡蛋方块 | 继承 `TurtleEggBlock`，自定义孵化逻辑 |

---

## 开发目标

### 性别与繁殖
- [x] 性别属性与异性繁殖
- [x] 饱食度驱动寻偶（替代原版喂食即 inLove）
- [x] 鸡配对仅产经验

### 养鸡玩法
- [x] 鸡蛋方块（海龟蛋风格孵化）
- [x] 公鸡/母鸡区别产出

### 基因与性状
- [ ] 基因序列、性状遗传
- [ ] 后代组合亲代基因
- [ ] 随机基因突变

### 饲养与生命
- [x] 饱食度系统
- [ ] 饲槽方块
- [ ] 动物最高寿命

### 群居与生活
- [ ] 群体行为机制
- [ ] 鞭子放牧工具
- [ ] 狗牧羊 AI

### 更多细节
- [ ] 动物粪便与骨粉
- [ ] ……（待头脑风暴）

---

## 许可协议

MIT License.

---

## 致谢

- **作者**：brodong
- 基于 [Minecraft Forge](https://minecraftforge.net/) 构建，Minecraft 版本 1.20.1
