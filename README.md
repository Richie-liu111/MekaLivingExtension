# Meka Living Extension

允许 MekaSuit（梅卡套装）的伤害吸收功能对非玩家实体生效的 Minecraft Forge Mod。

## 背景

Mekanism 模组中的 MekaSuit 是一套高级动力装甲，能够为穿戴的玩家吸收伤害。然而，原版 Mekanism 的伤害吸收逻辑仅在玩家（`Player`）实体上生效——如果其他实体（如 TouhouLittleMaid 模组中的女仆）穿上了 MekaSuit，则无法获得任何伤害吸收保护。

本模组通过 Mixin 技术修改 Mekanism 的伤害处理逻辑，使 MekaSuit 的伤害吸收能力扩展到所有穿戴它的生物实体。

## 环境要求

| 依赖 | 版本要求 |
|------|---------|
| Minecraft | 1.20.1 |
| Forge | ≥ 47 |
| Mekanism | ≥ 10.4 |
| Java | 17 |

## 工作原理

本模组通过两个 Mixin 类拦截 Mekanism 原有的伤害处理事件，并替换为对非玩家实体同样生效的逻辑：

### 1. CommonPlayerTickHandlerMixin

拦截 Mekanism 的 `CommonPlayerTickHandler` 中的两个事件处理方法：

- **`onEntityAttacked`（LivingAttackEvent）**：在实体受到攻击时，判断是否穿戴 MekaSuit。若是非玩家实体且穿着 MekaSuit，则尝试完全吸收伤害；成功则取消事件。
- **`onLivingHurt`（LivingHurtEvent）**：在实体实际受伤时，计算 MekaSuit 的伤害吸收比例，将剩余伤害应用于实体；若伤害被完全吸收则取消事件。

两个方法均会在入口处判断实体是否为 `Player`，若是玩家则直接返回，交由 Mekanism 原有逻辑处理，确保不影响原版行为。

### 2. ItemMekaSuitArmorMixin

通过 `@Accessor` 访问器 Mixin 暴露 `ItemMekaSuitArmor` 的私有字段 `absorption`，使辅助类能够读取每件 MekaSuit 护甲的基础吸收比例。

### 3. MekaSuitDamageHelper

核心伤害计算辅助类，从 Mekanism 原版逻辑中提取并适配，负责：

- 遍历实体穿戴的所有 MekaSuit 护甲
- 计算各护甲模块（Module）提供的伤害吸收比例
- 处理护甲基础吸收比例与 Mekanism 配置的伤害类型比率
- 根据可用能量计算实际可吸收的伤害量
- 扣除相应能量

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