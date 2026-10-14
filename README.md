本项目全部代码使用 **MiMo V2.6 Pro**，以自然语言描述的方式进行开发。

# Aram3 Web Server Controller

基于 **JDK 21 + Spring Boot + Vue 3 + Element Plus + SQLite** 的 Arma 3（AppID 233780）多实例网页控制器。

**前后端一体**：Vue 构建产物打包进 Spring Boot `static`，单个 jar 同时提供页面与 API。

## 功能（当前版本）

- 登录鉴权（JWT），首次启动后台打印随机管理员密码
- **用户管理**：列表 / 创建 / 编辑（显示名、邮箱、角色、超管）/ 重置密码 / 启停 / 删除
- **系统设置**：游戏服务器安装目录、SteamCMD 路径、模组/Workshop 目录、实例根目录、日志/数据目录（热生效）
- 实例 CRUD、启停、启动命令预览（含绝对路径 `server.cfg` / `basic.cfg`、`-ip`、自定义参数）
- 地图 PBO 上传（自动 `mapKey`）/ 删除；写入游戏 `mpmissions`
- Workshop 模组：HTML 预设导入、更新（Steam 账号 + 可选令牌）、SteamCMD 实时日志控制台
- 模组「本地搜索」状态；「移动 Key」到 `keys` 目录；Key 管理（来源、删除、上传）
- 实例模组加载配置（双端 / 仅服务端）
- 任务中心、RCON（玩家 / 踢人 / 广播）
- PowerShell 自动探测（pwsh 7 → powershell 5）
- SQLite 自动初始化；Nginx 反代友好（`VITE_BASE` / `VITE_API_BASE`）

## 目录

```
WebFroAramaServer/
├─ backend/     Spring Boot（含 static/ 前端产物）
├─ frontend/    Vue3 源码
├─ docs/        方案文档
├─ scripts/     构建脚本、Nginx 示例
├─ LICENSE      开源协议（禁止第三方商用）
├─ README.md
└─ README.en.md
```

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
cd frontend
npm install
npm run build

cd ..\backend
# 将 mvn 换成你本机的 Maven
mvn spring-boot:run
```

打开 **http://127.0.0.1:8080**。  
**首次启动**会在后端日志打印随机 `admin` 密码（6 位：大小写 + 数字 + 符号），登录后请立即修改。

开发时前后端分离（可选）：

```powershell
cd frontend
npm run dev
```

## 系统设置说明

| 设置项 | 作用 |
|---|---|
| 游戏服务器安装目录 | 含 `arma3server_x64.exe`；地图在其 `mpmissions\` |
| SteamCMD 路径 | `steamcmd.exe` |
| 模组/Workshop 目录 | Workshop 下载与模组库同一目录 |
| 实例配置根目录 | 每实例一个子目录（cfg / profiles） |
| 日志目录 | 控制器 / 任务日志 |
| 数据目录 | SQLite 等（备份主要备这里） |

## 开源协议

见 [LICENSE](LICENSE)。  
**允许**个人、学习、非商业使用与修改；**禁止第三方商用**（含售卖、收费 SaaS、并入商业产品等），除非获得版权方书面授权。详见 LICENSE 全文。

英文说明见 [README.en.md](README.en.md)。

## 上架 GitHub 前检查（隐私）

请勿提交：

- `data/`（SQLite、`server.cfg`、RCON 相关）
- `*.log`（可能含 Steam 账号 / SteamID）
- 本机绝对路径、真实公网 IP、Steam 账号、令牌、RCON 密码

生产环境必须设置：`ARAM3_JWT_SECRET`。

## 版本说明

- Java 21，Spring Boot 3.5.x 为可编译基线
- 升 Spring Boot 4 只需改 parent 版本
