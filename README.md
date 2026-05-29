# Meka Living Extension

允许 MekaSuit 的伤害吸收功能对非玩家实体生效的 Minecraft NeoForge Mod。

## 背景

Mekanism 模组中的 MekaSuit 是一套高级动力装甲，能够为穿戴的玩家吸收伤害。然而，原版 Mekanism 的伤害吸收逻辑仅在玩家（`Player`）实体上生效——如果其他实体（如 TouhouLittleMaid 模组中的女仆）穿上了 MekaSuit，则无法获得任何伤害吸收保护。

本模组通过 Mixin 技术修改 Mekanism 的伤害处理逻辑，使 MekaSuit 的伤害吸收能力扩展到所有穿戴它的生物实体。

## 环境要求

| 依赖 | 版本要求 |
|------|---------|
| Minecraft | 1.21.1 |
| NeoForge | ≥ 21.1.231 |
| Mekanism | ≥ 10.7 |
| Java | 21 |

## 工作原理

本模组通过两个 Mixin 类拦截 Mekanism 原有的伤害处理逻辑，并替换为对非玩家实体同样生效的逻辑：

### 1. CommonPlayerTickHandlerMixin

拦截 Mekanism 的 `CommonPlayerTickHandler` 中的 `onEntityAttacked` 事件处理方法：

- **`onEntityAttacked`（LivingIncomingDamageEvent）**：在实体受到伤害时，通过 `DamageContainer` 获取伤害数据，判断是否穿戴 MekaSuit。若是非玩家实体且穿着 MekaSuit，则计算伤害吸收比例，通过 `damageContainer.setNewDamage()` 设置剩余伤害；若伤害被完全吸收则取消事件。

方法在入口处判断实体是否为 `Player`，若是玩家则直接返回，交由 Mekanism 原有逻辑处理，确保不影响原版行为。

### 2. ItemMekaSuitArmorMixin

通过 `@Accessor` 访问器 Mixin 暴露 `ItemMekaSuitArmor` 的私有字段 `absorption`，使辅助类能够读取每件 MekaSuit 护甲的基础吸收比例（头盔 0.15、胸甲 0.4、护腿 0.3、靴子 0.15）。

### 3. MekaSuitDamageHelper

核心伤害计算辅助类，从 Mekanism 原版逻辑中提取并适配，负责：

- 遍历实体穿戴的所有 MekaSuit 护甲
- 计算各护甲模块（Module）提供的伤害吸收比例
- 通过 Data Map 系统查询伤害类型吸收比率
- 根据可用能量计算实际可吸收的伤害量
- 扣除相应能量

## 1.20.1 → 1.21.1 迁移变更

从 1.20.1 (Forge) 迁移到 1.21.1 (NeoForge) 涉及以下重大改动：

### 模组加载器

| 项目 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| 加载器 | Forge | NeoForge |
| Gradle 插件 | `net.minecraftforge.gradle` (ForgeGradle) | `net.neoforged.moddev` (ModDevGradle) |
| Java 版本 | 17 | 21 |
| 模组描述文件 | `mods.toml` | `neoforge.mods.toml` |

### 事件系统重构

1.20.1 中 Mekanism 的 `CommonPlayerTickHandler` 处理两个事件：

- `LivingAttackEvent`（攻击事件）→ `tryAbsorbAll()` 尝试完全吸收
- `LivingHurtEvent`（受伤事件）→ `getDamageAbsorbed()` 计算吸收比例

1.21.1 中合并为一个事件：

- `LivingIncomingDamageEvent` + `DamageContainer` → 统一计算吸收比例，通过 `setNewDamage()` 设置剩余伤害

**Mixin 目标从两个方法简化为一个方法**，逻辑更加清晰。

### 能量系统

| 项目 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| 能量类型 | `FloatingLong`（Mekanism 自定义高精度浮点） | `long`（Java 原生） |
| 能量消耗计算 | `energyCost.get().multiply(toAbsorb)` | `MathUtils.ceilToLong(energyCost.getAsLong() * toAbsorb)` |
| 能量比较 | `greaterOrEqual()` / `isZero()` | `>=` / `== 0L` |
| 能量扣减 | 回调列表延迟扣减 | `FoundArmorDetails.drainEnergy()` 直接扣减 |

### 伤害类型比率查找

| 项目 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| 查找方式 | `MekanismConfig.gear.mekaSuitDamageRatios`（Map 配置） | `IMekanismDataMapTypes.INSTANCE.getMekaSuitAbsorption()`（Data Map 系统） |
| 未指定类型回退 | `mekaSuitUnspecifiedDamageRatio.getAsFloat()` | `mekaSuitUnspecifiedDamageRatio.get()` |
| Tags 路径 | `mekanism.common.tags.MekanismTags` | `mekanism.api.MekanismAPITags` |
| 技术伤害检查 | 无 | 新增 `Tags.DamageTypes.IS_TECHNICAL` 检查 |

### 其他变更

- `MekaSuitDamageHelper` 从 `mixin` 包移至根包，不再作为 Mixin 类
- 移除了 `tryAbsorbAll()` 方法（1.21.1 事件合并后不再需要）
- `@Mod` 注解导入从 `net.minecraftforge` → `net.neoforged`
- Mixin 配置 `compatibilityLevel` 从 `JAVA_17` → `JAVA_21`

## 构建与开发

```bash
# 构建项目
./gradlew build

# 启动客户端
./gradlew runClient

# 启动服务端
./gradlew runServer
```

构建产物位于 `build/libs/` 目录下。

## 许可证

MIT License
