# 故障排查

## 插件无法加载

1. 确认服务端是 Paper 1.21.11、26.1.2、26.2 或 26.3。
2. Paper 1.21.11 使用 Java 21+；Paper 26 使用 Java 25+。
3. 移除旧 IPPlus 和 IEP jar，只保留一个 Infinite Parkour Reborn jar。
4. 查看控制台中的第一条异常，而不只是最后的“插件已禁用”。

## 修改配置后没有效果

- 检查 YAML 缩进与引号。
- 确认材料、粒子和音效存在于当前 Paper 版本。
- 核心文件使用 `/ip reload`，多人文件使用 `/ipp reload`。
- 世界、模式注册、存储或鞘翅设置请重启服务器。
- 先备份，再与新生成的默认文件对比。

## 玩家无法打开菜单或加入

检查 `plugins/IP/config.yml` 中的 `permissions.enabled`、对应的 `default-values` 菜单开关以及玩家权限。鞘翅权限使用 `plugins/IP/elytra/config.yml` 中独立的 `permissions` 开关。

需要允许玩家加入方块跑酷时，还要确认核心配置为 `joining: true`。

## 背包没有恢复

让受影响玩家保持在线并执行：

```text
/ip recoverinventory <玩家>
```

该命令需要 `ip.admin`。恢复前不要反复加入和退出，以免新保存的数据覆盖需要找回的内容；排查期间请保留 `plugins/IP/inventories/`。

## 迁移后的文件缺失

迁移只会在目标文件不存在时复制。请对比：

- `plugins/IPPlus/` 与 `plugins/IP/plus/`
- `plugins/IEP/` 与 `plugins/IP/elytra/`

手动复制前必须关闭服务器，并保留两份备份。

## 报告问题

请提交 [GitHub Issue](https://github.com/LostUmbrella58/IP-Reborn/issues) 或加入 [Discord](https://discord.gg/WxfBtuAsv6)，并附上：

- 准确的 Paper 与 Java 版本；
- 插件版本；
- 启动日志和完整异常；
- 已隐藏密码的相关配置；
- 可以稳定复现问题的步骤。

请勿在评价区报告问题，评价区不适合进行完整排查。
