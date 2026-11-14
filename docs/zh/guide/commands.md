# 命令

`<尖括号>` 表示必填参数，`[方括号]` 表示可选参数。

## 方块跑酷

`/parkour`、`/ip` 和 `/witp` 是同一个命令。

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/parkour` | 打开主菜单；非玩家执行时显示帮助 | `ip.main` |
| `/parkour help` | 显示可用命令 | — |
| `/parkour join [模式/玩家]` | 加入默认模式、指定模式或其他玩家的会话 | `ip.join` |
| `/parkour leave` | 离开当前会话 | `ip.quit` |
| `/parkour menu` | 打开主菜单 | `ip.main` |
| `/parkour play` | 打开模式选择 | `ip.play` |
| `/parkour leaderboard [模式]` | 打开排行榜 | `ip.community.leaderboards` |
| `/ip reload` | 重载核心配置和语言文件 | `ip.admin` |
| `/ip reset <everyone/玩家/UUID>` | 永久清除方块跑酷成绩 | `ip.admin` |
| `/ip forcejoin <everyone/nearest/玩家>` | 强制玩家加入默认模式 | `ip.admin` |
| `/ip forceleave <everyone/玩家>` | 强制玩家离开跑酷 | `ip.admin` |
| `/ip recoverinventory <玩家>` | 恢复在线且已退出跑酷的玩家保存的背包 | `ip.admin` |

### 方块跑酷建筑模板

以下命令需要 `ip.admin`，并且必须由玩家执行。

| 命令 | 说明 |
| --- | --- |
| `/ip schematic wand` | 获取选区工具 |
| `/ip schematic pos1` | 在当前位置设置点 1 |
| `/ip schematic pos2` | 在当前位置设置点 2 |
| `/ip schematic save` | 用随机代码保存选区 |
| `/ip schematic paste <文件>` | 在当前位置粘贴已加载的模板 |

## 多人模式

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/ipp create` | 打开多人模式选择 | `ip.multiplayer` |
| `/ipp lobbies` | 查看正在运行的多人房间 | `ip.active` |
| `/ipp invite` | 打开邀请菜单 | `ip.invite` |
| `/ipp reload` | 重载多人配置 | `ip.admin` |
| `/ipp lobbygm pos1` | 设置大厅模式选区点 1 | `ip.admin` |
| `/ipp lobbygm pos2` | 设置大厅模式选区点 2 | `ip.admin` |
| `/ipp lobbygm save` | 保存大厅模式选区 | `ip.admin` |

`/ipp multiplayer` 等同于 `/ipp create`；`/ipp lobby` 等同于 `/ipp lobbies`。

## 鞘翅跑酷

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/iep play` | 打开游玩菜单 | `iep.play` |
| `/iep leaderboards` | 打开鞘翅排行榜 | `iep.leaderboard` |
| `/iep settings` | 打开鞘翅设置 | `iep.setting` |
| `/iep leave` | 离开鞘翅跑酷 | `iep.leave` |
| `/iep seed <种子>` | 设置当前跑酷的非负种子 | `iep.setting.seed` |
| `/iep schematic <x,y,z> <x,y,z>` | 将长方体选区保存为鞘翅模板 | OP |
| `/iep reset <玩家/UUID> [模式]` | 清除全部或指定模式成绩 | OP |
