# 权限体系改造计划与进度（0.5.0）

> 本文档由 ZCode 维护，记录「全面权限控制体系」改造的整体计划、详细修改条目与实时进度。
> 状态标记：✅ 已完成 · ⏳ 进行中 · ⬜ 未开始

## 1. 背景与目标

当前权限模型在数据库层基本齐备（`user_instance_perm` 9 个开关、`sys_role_permission` 权限码），但 **95% 是死代码**：

- 全后端只有 RCON 的 `can_rcon` 真正生效（`RconController.requireRcon`）
- `InstanceService.requirePerm` 是空壳——任何登录用户可启停/创建/修改/删除任意实例
- 权限码从未加载进 Spring Security，`instance:create` 等纯摆设；operator/mod_admin/viewer 角色无任何运行时效果
- `PUT /instances/{id}/permissions` 无任何权限校验（存在自我提权漏洞）
- 删除实例只删 DB 行，磁盘目录残留；`create` 的 `createdBy` 恒为 null

**目标：双层权限模型**

```
┌─────────────────────────────────────────────────────────┐
│ 超管（用户 is_super_admin 标志）→ 绕过一切                │
├─────────────────────────────────────────────────────────┤
│ 第一层：全局角色权限码（sys_role_permission）              │
│   user:manage / system:settings / job:manage /           │
│   map:manage / mod:manage / steamcmd:use / audit:view    │
│   特殊权限（实例）：instance:create / instance:grant      │
│   → 加载进 Spring Security authorities，@PreAuthorize 校验 │
│   → 控制功能页菜单、新建实例、实例授权等                    │
├─────────────────────────────────────────────────────────┤
│ 第二层：实例级 ACL（user_instance_perm，10 个开关）        │
│   查看 / 启停 / 核心配置 / 实例配置 / 地图 / 模组 /        │
│   日志 / RCON / 定时任务 / 文件传输                        │
│   → 按 用户×实例 编程式校验（Controller/Service 层）        │
└─────────────────────────────────────────────────────────┘
```

## 2. 已确认的设计决策

| # | 决策 | 内容 |
|---|---|---|
| 1 | 删除实例 | DB 记录 + 磁盘实例目录（弹窗显示将删路径；rootPath 必须位于 instancesDir 内才删磁盘，防误删任意目录；不跟随符号链接） |
| 2 | 角色管理 | 侧边栏新增独立「角色与权限」页；内置 4 角色可改权限不可删，自定义角色可增删（被引用的角色禁删） |
| 3 | 实例级权限 | 完整 10 项：查看/启停/核心配置(新增列)/实例配置(server.cfg·basic.cfg)/地图/模组/日志/RCON/定时任务/文件传输 |
| 4 | 特殊权限 | 「新建实例 instance:create」「实例授权 instance:grant」在角色编辑器单独分组展示 |
| 5 | 超管语义 | `admin` 角色**不再等同超管**；超管仅由 `is_super_admin` 用户标志决定（ROLE_SUPER_ADMIN） |
| 6 | 权限生效 | `JwtAuthFilter` 每请求重新加载用户 → 角色/权限变更即时生效，无需重新登录 |
| 7 | RCON 密码 | 仅对持有「核心配置」权限的用户下发（RCON 连接由后端完成，前端无需明文） |
| 8 | 授权入口 | 实例授权从「实例侧」移到「用户侧」（用户管理 → 实例权限矩阵）；原 `PUT /instances/{id}/permissions` 提权漏洞端点删除 |

## 3. 权限校验矩阵（后端实际执行）

### 3.1 全局权限码 → 端点

| 权限码 | 端点 |
|---|---|
| `user:manage` | /users 全部管理端点、/roles 全部 |
| `system:settings` | /system/settings GET/PUT |
| `job:manage` | /jobs、/jobs/{id} |
| `map:manage` | POST/DELETE /maps |
| `mod:manage` | POST/DELETE /mods*、/mods/actions/*、/keys 写操作 |
| `steamcmd:use` | /steamcmd/* 全部 |
| `instance:create` | POST /instances |
| `instance:grant` | GET/PUT /users/{id}/instance-perms |
| 超管专属 | /users/{id}/unlock、/instances/{id}/rcon/exec、rcon shutdown |

### 3.2 实例级 ACL → 端点

| 开关 | 端点 |
|---|---|
| can_view | GET /instances（列表过滤）、GET /instances/{id}、preview、GET config-files |
| can_start_stop | start/stop |
| can_edit_core | PUT/DELETE /instances/{id}（核心配置=实例编辑对话框字段） |
| can_edit_config | PUT/POST reset config-files（server.cfg/basic.cfg） |
| can_manage_map | POST /instances/{id}/maps/{mapId} |
| can_manage_mod | GET/PUT/POST/DELETE /instances/{id}/mods* |
| can_rcon | /instances/{id}/rcon/*（原有逻辑保留） |

## 4. 详细修改清单与进度

### 4.1 后端（backend/src/main/java/com/aram3）

| # | 修改项 | 文件 | 状态 |
|---|---|---|---|
| B1 | 权限码目录枚举（9 码，SYSTEM/INSTANCE 分组） | `security/PermCode.java`（新） | ✅ |
| B2 | 携带 uid/superAdmin 的 Principal | `security/AppPrincipal.java`（新） | ✅ |
| B3 | 加载角色+权限码进 authorities；超管附加全部码 | `security/UserDetailsServiceImpl.java` | ✅ |
| B4 | currentUserId()/hasAuthority()；isSuperAdmin 只认 ROLE_SUPER_ADMIN | `security/AuthFacade.java` | ✅ |
| B5 | Role 领域记录 | `domain/Role.java`（新） | ✅ |
| B6 | UserInstancePerm 增 canEditCore + 全部 has* 辅助方法 | `domain/UserInstancePerm.java` | ✅ |
| B7 | user_instance_perm 增 can_edit_core 列（新装） | `resources/schema.sql` | ✅ |
| B8 | upsert/RowMapper 增 can_edit_core | `repository/PermRepository.java` | ✅ |
| B9 | 角色仓储：CRUD/权限码/引用计数/replacePerms | `repository/RoleRepository.java`（新） | ✅ |
| B10 | listVisible 真过滤；requirePerm 真校验 + 6 个 require* 公开方法；effectivePerm；delete 连带删磁盘目录（安全校验+不跟随符号链接）；create/update 审计带 userId；移除 grantPerm/listPerms/getPerm/listAll | `service/InstanceService.java` | ✅ |
| B11 | InstanceView/MyPerms 响应结构；rconPassword 按 canEditCore 下发；@PreAuthorize instance:create；requireEditCore/View；删除 GET/PUT /{id}/permissions | `web/InstanceController.java` | ✅ |
| B12 | GET→requireView；PUT/reset→requireEditConfig | `web/InstanceConfigController.java` | ✅ |
| B13 | 地图/模组库写操作 map:manage/mod:manage；实例绑定地图→canManageMap；实例模组→canManageMod | `web/AssetController.java` | ✅ |
| B14 | 类级 steamcmd:use；修复 userId() 空桩→AuthFacade | `web/SteamCmdController.java` | ✅ |
| B15 | /jobs*→job:manage | `web/JobController.java` | ✅ |
| B16 | 类级 system:settings | `web/SettingController.java` | ✅ |
| B17 | /keys 写操作→mod:manage | `web/KeyController.java` | ✅ |
| B18 | 角色管理服务（校验/审计/目录） | `service/RoleAdminService.java`（新） | ✅ |
| B19 | /roles CRUD + /perm-catalog（user:manage） | `web/RoleController.java`（新） | ✅ |
| B20 | listInstancePerms/saveInstancePerms（矩阵；目标是超管则拒绝） | `service/UserAdminService.java` | ✅ |
| B21 | 8 处 @PreAuthorize→hasAuthority('user:manage')；新增 GET/PUT /{id}/instance-perms（instance:grant） | `web/UserController.java` | ✅ |
| B22 | publicUser()/me() 增 perms 数组（超管=全部码） | `service/AuthService.java` | ✅ |
| B23 | BusinessException code 合法时映射为 HTTP 状态（403 权限拒绝） | `common/GlobalExceptionHandler.java` | ✅ |
| B24 | ensureCanEditCoreColumn 补列；权限播种改为仅表空时（防覆盖管理员编辑）；sys_setting 一次性迁移补 admin 角色新码 | `config/DataInitializer.java` | ✅ |
| B25 | 后端编译验证（IDEA 捆绑 Maven：`D:\Software\idea\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd`） | — | ✅ |
| B26 | 删除用户前清理无 CASCADE 的外键引用（audit_log.user_id、各表 created_by 置 NULL；子表显式删除）——V2 验证暴露的缺陷：viewer 创建过 job_task 后删除用户 500 | `repository/UserRepository.java` | ✅ |

### 4.2 前端（frontend/src）

| # | 修改项 | 文件 | 状态 |
|---|---|---|---|
| F1 | hasPerm(code) getter（superAdmin→true） | `stores/auth.js` | ✅ |
| F2 | /roles 路由；各页面 meta.perms + beforeEach 校验 | `router/index.js` | ✅ |
| F3 | 菜单按 hasPerm 显隐；新增「角色与权限」菜单 | `views/LayoutView.vue` | ✅ |
| F4 | 角色列表 + 权限编辑对话框（系统权限/特殊权限分组）+ 自定义角色增删 | `views/RolesView.vue`（新） | ✅ |
| F5 | 每行「实例权限」按钮 → 实例×10 开关矩阵对话框（超管行禁用） | `views/UsersView.vue` | ✅ |
| F6 | 新建按钮 hasPerm(instance:create)；行按钮按 row.myPerms 显隐；删除确认显示磁盘目录路径；rconPassword 字段按 canEditCore | `views/InstancesView.vue` | ✅ |
| F7 | 保存/重置按钮按 myPerms.canEditConfig 禁用 | `views/InstanceConfigView.vue` | ✅ |
| F8 | 保存按 myPerms.canManageMod 禁用 | `views/InstanceModsView.vue` | ✅ |
| F9 | 按 myPerms.canRcon 控制（保留超管 shutdown/exec） | `views/InstanceControlView.vue` | ✅ |

### 4.3 i18n（三语同步：zh-CN / en-US / ru-RU）

| # | 修改项 | 状态 |
|---|---|---|
| I1 | 新模块 `roles.js`（角色页文案）×3 | ✅ |
| I2 | 新模块 `perms.js`（权限码 + 实例开关标签，角色页与用户矩阵共用）×3 | ✅ |
| I3 | `users.js` 增实例权限矩阵 key ×3 | ✅ |
| I4 | `instances.js` 增权限按钮/删除警告 key ×3 | ✅ |
| I5 | `layout.js` 增 menu.roles/titles.roles ×3 | ✅ |
| I6 | 三份 `index.js` 注册 roles/perms 模块 | ✅ |

### 4.4 文档与验证

| # | 修改项 | 状态 |
|---|---|---|
| D1 | 本计划文档 | ✅ |
| D2 | 三份 README 功能列表加权限体系描述 | ✅ |
| V1 | 前端 npm run build 通过 | ✅ |
| V2 | 重启后 curl 端到端验证（见 §5） | ✅ |

## 5. 端到端验证清单（V2）

1. admin 登录 → `user.perms` 含全部 9 码；`GET /instances` → myPerms 全 true、含 rconPassword
2. 建 viewer 测试用户（无实例授权）→ 登录 → 实例列表为空；`POST /instances` 403；启停 403
3. `PUT /users/{id}/instance-perms` 授权（查看+启停）→ 列表可见、启停放行、核心配置编辑仍 403
4. 非授权用户调 `PUT /users/{id}/instance-perms` → 403（提权漏洞关闭）
5. 角色增删改、权限码增减 → 下一个请求即生效（无需重登录）
6. 删除测试实例 → DB 行消失 + 磁盘目录消失
7. 前端抽查：角色页矩阵、用户实例权限矩阵、按钮显隐、三语切换

## 6. API 变更摘要

### 新增
- `GET /api/v1/roles` — 角色列表（含权限码）
- `GET /api/v1/roles/perm-catalog` — 权限码目录（含分组）
- `POST /api/v1/roles` / `PUT /api/v1/roles/{id}` / `DELETE /api/v1/roles/{id}` — 角色 CRUD
- `GET /api/v1/users/{id}/instance-perms` — 用户×实例权限矩阵
- `PUT /api/v1/users/{id}/instance-perms` — 批量保存矩阵

### 变更
- `GET /api/v1/instances`、`GET /api/v1/instances/{id}`：返回 `InstanceView`（增 `myPerms`；非超管仅见 canView 实例；`rconPassword` 仅 canEditCore 用户可见）
- `DELETE /api/v1/instances/{id}`：连带删除磁盘实例目录，返回 `{deletedRootPath}`
- 登录/`me()` 响应 user 对象增 `perms` 数组
- 各写端点按 §3 矩阵返回 403（无权限）/ 要求对应权限

### 删除
- `GET/PUT /api/v1/instances/{id}/permissions`（提权漏洞，被用户侧矩阵取代）

## 7. 变更记录

| 日期 | 记录 |
|---|---|
| 2026-11-19 | 创建文档；后端 B1-B24 全部代码完成，待编译验证（B25） |
| 2026-11-19 | **后端全部完成**：B25 编译通过（mvn -q compile 无错误，target/classes 全部新类生成：PermCode/AppPrincipal/RoleController/InstanceView$MyPerms 等）。开始前端 F1-F9 |
| 2026-11-19 | **前端 + i18n + 文档全部完成**：F1 hasPerm；F2 路由 meta.perms+守卫；F3 菜单显隐+角色菜单；F4 RolesView（角色列表/权限分组编辑/自定义增删）；F5 用户×实例 10 开关矩阵；F6-F9 各页按 myPerms/hasPerm 适配（编辑无核心权限时 rconPassword 传 null 防覆盖）；I1-I6 三语模块（roles/perms/common.noPerm）；D2 三份 README 加权限体系描述；V1 npm run build 通过（RolesView chunk 生成）。**剩余：V2 需重启后端后按 §5 端到端验证** |
| 2026-11-19 | **V2 端到端验证通过（31/31）**：①admin 9 码 + myPerms 全 true + rconPassword 下发；②viewer 无授权列表空/建实例 403/启停 403/改核心 403/查看 403；③授权后列表可见、仅 view/startStop 放行、rconPassword 不下发；④viewer 调授权端点 403（提权漏洞关闭）；⑤角色增删改同一 token 即时生效（授予即 200、清空即 403）；⑥删实例 DB 行+磁盘目录均消失；⑦SPA/三语构建产物抽查通过。**验证暴露并修复 B26**：删除曾创建过任务的用户因 job_task.created_by 外键无 CASCADE 而 500，已修（UserRepository.delete 先置 NULL 再删）；测试数据（viewer_e2e 用户、测试实例、临时角色）已全部清理。**至此 0.5.0 权限体系改造全部完成** |
