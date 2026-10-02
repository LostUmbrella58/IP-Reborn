# 迁移

版本 6 只使用一个插件 jar。不要让旧 IPPlus 或 IEP jar 与它同时运行。

从 Infinite Parkour Reborn 6.0.0 升级到 6.0.1 时，关闭服务器、替换插件 jar，并保留 `plugins/IP/`。运行 Paper 26.3 需要 Java 25 或更高版本。数据目录与旧插件迁移流程保持不变。

## 升级前

1. 关闭服务器。
2. 备份 `plugins/IP/`、`plugins/IPPlus/` 和 `plugins/IEP/`。
3. 从 `plugins/` 移除旧插件 jar。
4. 放入 Infinite Parkour Reborn jar 并启动服务器。

## 自动迁移目录

启动时，插件会复制目标位置尚不存在的文件：

| 原位置 | 新位置 |
| --- | --- |
| `plugins/IPPlus/` | `plugins/IP/plus/` |
| `plugins/IEP/` | `plugins/IP/elytra/` |

复制过程不会删除原目录，也不会覆盖目标位置已有的文件；即使启动中断，也可以再次安全执行。

## 检查清单

- 打开 `/parkour`、`/ipp` 和 `/iep`。
- 检查玩家分数与排行榜。
- 测试自定义风格和建筑模板。
- 如果启用了 MySQL，检查数据库连接设置。
- 分别加入并离开三类跑酷，确认背包正确恢复。
- 检查控制台是否存在配置或材料名称警告。

全部数据和玩法确认无误后，再手动归档旧数据目录；在此之前请一直保留备份。
