<!--
  README 语言导航说明（维护者注释，不会在渲染后显示）：
  本文件是中文默认版。新增语言版本时，请同步修改三个 README 顶部的「一句话简介」与「语言互链」两块，
  保证三份文件的链接集合一致（README.md / README.en.md / README.ru.md）。
-->

> **中文**：Arma 3 专用服务器网页管理面板：实例、模组、地图与 RCON 一体化管理。
> **English**: Web control panel for Arma 3 dedicated servers — instances, mods, maps and RCON in one place.
> **Русский**: Веб-панель управления выделенными серверами Arma 3 — экземпляры, моды, карты и RCON в одном месте.

[中文](README.md) | [English](README.en.md) | [Русский](README.ru.md)

<!-- ====== 项目简介 ====== -->

本项目全部代码使用 **MiMo V2.6 Pro**，以自然语言描述的方式进行开发。

# Aram3 Web Server Controller

基于 **JDK 21 + Spring Boot + Vue 3 + Element Plus + SQLite** 的 Arma 3（AppID 233780）多实例网页控制器。

**前后端一体**：Vue 构建产物打包进 Spring Boot `static`，单个 jar 同时提供页面与 API。

<!-- ====== 功能列表：新增功能时请同步更新 zh / en / ru 三个版本 ====== -->

## 功能（当前版本）

- 登录鉴权（JWT），首次启动后台打印随机管理员密码
- **用户管理**：列表 / 创建 / 编辑（显示名、邮箱、角色、超管）/ 重置密码 / 启停 / 删除
- **系统设置**：游戏服务器安装目录、SteamCMD 路径、模组/Workshop 目录、实例根目录、日志/数据目录（热生效）
- 实例 CRUD、启停、启动命令预览（含绝对路径 `server.cfg` / `basic.cfg`、`-ip`、自定义参数）
- 地图 PBO 上传（自动 `mapKey`）/ 删除；写入游戏 `mpmissions`
- Workshop 模组：HTML 预设导入、更新（Steam 账号 + 可选令牌）、SteamCMD 实时日志控制台
- 模组「本地搜索」状态；「移动 Key」到 `keys` 目录；Key 管理（来源、删除、上传）
- 实例模组加载配置（双端 / 仅服务端）
- 任务中心
- **实例控制台（RCON）**：在线玩家列表与信息（GUID / IP / 延迟 / 大厅状态）、踢出 / 封禁（时长 + 理由）、全服公告与私聊、封禁管理（`bans.txt` 解封、按 GUID/IP 封禁）、锁服 / 解锁、最大人数、定时重启、任务切换（missions / mission）、热重载（loadBans / loadScripts / loadEvents / loadRPT）、RPT 日志尾部、原始 RCON 命令控制台
- **登录安全**：`admin` 仅允许服务器本机登录（无失败次数限制）；其他账户连续登录失败自动锁定（阈值可配置，默认 3 次）；超管在用户管理一键解锁并重置 16 位随机密码（大小写 + 数字），锁定后已登录 token 立即失效
- **多语言界面**：中文 / English / Русский 三语切换，登录页与主界面右上角地球图标切换，选择持久化
- PowerShell 自动探测（pwsh 7 → powershell 5）
- SQLite 自动初始化；Nginx 反代友好（`VITE_BASE` / `VITE_API_BASE`）

<!-- ====== 目录结构：新增顶层文件/目录时同步维护本节 ====== -->

## 目录

```
WebFroAramaServer/
├─ backend/     Spring Boot（含 static/ 前端产物）
├─ frontend/    Vue3 源码
├─ docs/        方案文档
├─ scripts/     构建脚本、Nginx 示例
├─ LICENSE      开源协议（禁止第三方商用）
├─ README.md      中文版（默认）
├─ README.en.md   English
└─ README.ru.md   Русский
```

<!-- ====== 构建与运行：命令均在 Windows PowerShell 下验证 ====== -->

## 构建与运行（一体）

### IDEA 打开方式

| 打开路径 | Spring Boot | 前端构建配置 |
|---|---|---|
| 仓库根目录 | 根目录有聚合 `pom.xml`，Maven 刷新后可发现 `backend` | 有 |
| `backend/` | 直接识别 Spring Boot | 有 |

**根目录打开时**：Maven 面板 → Reload All Maven Projects。  
若未识别：右键 `backend/pom.xml` → Add as Maven Project。

### 运行配置

| 配置名 | 说明 |
|---|---|
| **Aram3Application** | 启动 Spring Boot（http://127.0.0.1:8080） |
| **构建前端** | `scripts\build-frontend.cmd` |
| **构建前端(npm)** | `npm run build` |
| **打包前后端** | 前端 + Maven 打 jar |

建议顺序：**构建前端** → **Aram3Application**。

### 手动

```powershell
# 第一步：安装依赖并构建前端，产物输出到 backend/src/main/resources/static
cd frontend
npm install
npm run build

# 第二步：启动后端（将 mvn 换成你本机的 Maven；IDEA 用户可直接运行 Aram3Application）
cd ..\backend
mvn spring-boot:run
```

打开 **http://127.0.0.1:8080**。  
**首次启动**会在后端日志打印随机 `admin` 密码（6 位：大小写 + 数字 + 符号），登录后请立即修改。

开发时前后端分离（可选，前端改动免打包热更新）：

```powershell
# 仅启动前端开发服务器（API 由 VITE_API_BASE 指向后端）
cd frontend
npm run dev
```

<!-- ====== 系统设置：以「系统设置」页面实际字段为准，新增设置项请补表 ====== -->

## 系统设置说明

| 设置项 | 作用 |
|---|---|
| 游戏服务器安装目录 | 含 `arma3server_x64.exe`；地图在其 `mpmissions\` |
| SteamCMD 路径 | `steamcmd.exe` |
| 模组/Workshop 目录 | Workshop 下载与模组库同一目录 |
| 实例配置根目录 | 每实例一个子目录（cfg / profiles） |
| 日志目录 | 控制器 / 任务日志 |
| 数据目录 | SQLite 等（备份主要备这里） |
| 登录失败锁定次数 | 非 `admin` 账户连续失败自动锁定的阈值（1-20，默认 3，见「系统设置 → 登录安全」） |

<!-- ====== 发布前隐私检查：公开仓库前务必逐条核对 ====== -->

## 发布到 GitHub 前（隐私检查）

**不要**提交以下内容：

- `data/`（SQLite、`server.cfg`、RCON 相关配置）
- `*.log`（可能包含 Steam 账号 / SteamID）
- 本地绝对路径、真实公网 IP、Steam 账号、令牌、RCON 密码

生产部署必须设置环境变量 `ARAM3_JWT_SECRET`。

<!-- ====== 开源协议 ====== -->

## 开源协议

见 [LICENSE](LICENSE)。  
**允许**个人、学习、非商业使用与修改；**禁止第三方商用**（含售卖、收费 SaaS、并入商业产品等），除非获得版权方书面授权。详见 LICENSE 全文。

其他语言版本：[English](README.en.md) · [Русский](README.ru.md)

<!-- ====== 版本基线：升级框架时更新本节 ====== -->

## 版本说明

- Java 21，Spring Boot 3.5.x 为可编译基线
- 升 Spring Boot 4 只需改 parent 版本
