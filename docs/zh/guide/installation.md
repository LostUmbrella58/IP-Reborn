# 安装

## 环境要求

| Paper 版本 | Java 要求 |
| --- | --- |
| 1.21.11 | Java 21+ |
| 26.1.2 | Java 25+ |
| 26.2 | Java 25+ |
| 26.3 | Java 25+ |

Infinite Parkour Reborn 6.0.1 新增了对 Paper 26.3 的支持。

插件不需要 `vilib`、PaperLib、VoidGen 或额外的虚空世界生成器。

## 全新安装

1. 关闭服务器。
2. 从 [GitHub Releases](https://github.com/LostUmbrella58/IP-Reborn/releases) 或 [Spigot 页面](https://www.spigotmc.org/resources/infinite-parkour-reborn.136046/)下载 jar。
3. 将 jar 放入服务器的 `plugins/` 目录。
4. 启动服务器并等待插件生成文件。
5. 在游戏内分别执行 `/parkour`、`/ipp` 和 `/iep`，确认菜单可以打开。
6. 修改 YAML 前关闭服务器，修改完毕后重新启动。

## 首次启动生成的文件

```text
plugins/IP/
├─ config.yml
├─ generation.yml
├─ rewards-v2.yml
├─ locales/
├─ schematics/
├─ plus/
│  ├─ config.yml
│  └─ locales/
└─ elytra/
   ├─ config.yml
   ├─ rewards.yml
   ├─ locales/
   └─ schematics/
```

PlaceholderAPI、Vault、Multiverse-Core、HolographicDisplays、floodgate 和 Chunky 均为可选依赖，详见[插件联动](./integrations)。

::: tip
大幅修改前请保留一份默认配置。YAML 缩进必须正确，材料、粒子和音效名称也必须存在于你使用的 Paper 版本中。
:::
