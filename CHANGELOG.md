<!--
  更新日志（维护者注释）：
  顶部标题固定为中文；每个版本条目下按「中文 / English / Русский」三段撰写，三段内容必须逐条对应。
  发布新版本时复制一段空模板，勿删改历史版本记录。
-->

# 新增双层权限体系（全局角色权限码 + 实例级 ACL）

> 0.5.0 更新日志（2026-11-19）· [中文](CHANGELOG.md) | [English](CHANGELOG.md#english) | [Русский](CHANGELOG.md#русский)

### 中文

#### 新增
- **双层权限体系**（此前数据库权限模型 95% 为死代码，本次全部接线生效）：
  - **全局角色权限码**（9 码）：`user:manage` / `system:settings` / `job:manage` / `map:manage` / `mod:manage` / `steamcmd:use` / `audit:view` + 特殊权限 `instance:create`（新建实例）/ `instance:grant`（实例授权）；加载进 Spring Security，`@PreAuthorize` 在后端强制校验，前端控制菜单与页面路由
  - **实例级 ACL**：用户×实例矩阵授权 10 项开关（查看 / 启停 / 核心配置 / 实例配置 / 地图 / 模组 / 文件传输 / 日志 / RCON / 定时任务），在「用户管理 → 实例权限」批量配置
  - **「角色与权限」页**：内置 4 角色可改权限不可删；自定义角色增删（被用户引用的角色禁删）；权限码按「系统权限 / 特殊权限」分组
  - **超管语义**：超管仅由用户 `is_super_admin` 标志决定，绕过一切校验；`admin` 角色不再等同超管
  - **即时生效**：角色 / 权限变更下一个请求即生效，无需重新登录
  - **RCON 密码保护**：仅对持有「核心配置」权限的用户下发；RCON 连接由后端完成
  - 实例级按钮显隐：实例列表 / 配置文件 / 模组配置 / 控制台各操作按 `myPerms` 逐项控制
- 删除实例时连带删除磁盘实例目录（rootPath 必须位于实例根目录内才删，防误删；不跟随符号链接），确认弹窗显示将删路径
- 登录 / `me()` 响应新增 `perms` 数组；各写端点无权限时返回 403

#### 修复
- **提权漏洞**：原 `GET/PUT /instances/{id}/permissions` 无任何权限校验，任何登录用户可给自己加权限；该端点已删除，授权统一收敛到用户侧矩阵（需 `instance:grant`）
- `InstanceService.requirePerm` 空壳问题：任何登录用户可启停 / 创建 / 修改 / 删除任意实例 → 现按 ACL 强制校验
- 删除曾创建过任务的用户报 500（`job_task.created_by` 等外键无 CASCADE）→ 删除用户前先清理引用

#### 文档
- 新增 `docs/permission-system-plan.md`（改造计划、权限校验矩阵、API 变更、端到端验证记录）
- 三份 README 功能列表增加权限体系描述

### English

#### Added
- **Two-layer permission system** (the DB permission model was ~95% dead code; now fully wired):
  - **Global role permission codes** (9): `user:manage` / `system:settings` / `job:manage` / `map:manage` / `mod:manage` / `steamcmd:use` / `audit:view` + special `instance:create` / `instance:grant`; loaded into Spring Security, enforced server-side via `@PreAuthorize`, driving menus and routes in the frontend
  - **Instance-level ACL**: per user×instance matrix with 10 switches (view / start-stop / core config / instance config / maps / mods / file transfer / logs / RCON / scheduled jobs), managed in "Users → Instance Perms"
  - **"Roles & Permissions" page**: 4 built-in roles editable but not deletable; custom roles can be created/deleted (referenced roles protected); codes grouped into system / special (instance)
  - **Super admin semantics**: super admin is decided solely by the `is_super_admin` user flag and bypasses all checks; the `admin` role no longer implies super admin
  - **Immediate effect**: role/permission changes apply to the very next request, no re-login needed
  - **RCON password protection**: only sent to users with the "Core Config" permission; RCON connections are made by the backend
  - Per-instance button visibility across instance list / config files / mod config / console based on `myPerms`
- Deleting an instance now also removes its disk directory (rootPath must be inside the instances root; symlinks not followed); the confirm dialog shows the path
- Login / `me()` responses now include a `perms` array; write endpoints return 403 without permission

#### Fixed
- **Privilege escalation hole**: the old `GET/PUT /instances/{id}/permissions` endpoints had no permission checks, letting any logged-in user grant themselves permissions; removed and replaced by the user-side matrix (requires `instance:grant`)
- Hollow `InstanceService.requirePerm`: any logged-in user could start/stop/create/modify/delete any instance → now enforced per ACL
- Deleting a user who had created jobs failed with 500 (FK on `job_task.created_by` etc. without CASCADE) → references are cleaned up first

#### Docs
- Added `docs/permission-system-plan.md` (plan, permission matrix, API changes, E2E verification log)
- Permission system section added to all three READMEs

### Русский

#### Добавлено
- **Двухуровневая система прав** (ранее модель прав в БД была мёртвым кодом на ~95%; теперь полностью подключена):
  - **Глобальные права ролей** (9): `user:manage` / `system:settings` / `job:manage` / `map:manage` / `mod:manage` / `steamcmd:use` / `audit:view` + особые `instance:create` / `instance:grant`; загружаются в Spring Security, проверяются на бэкенде через `@PreAuthorize` и управляют меню и маршрутами фронтенда
  - **Права на уровне экземпляров**: матрица «пользователь × экземпляр» с 10 переключателями (просмотр / запуск-остановка / основная конфигурация / конфигурация экземпляра / карты / моды / передача файлов / журналы / RCON / расписания), настраивается в «Пользователи → Права на экземпляры»
  - **Страница «Роли и права»**: 4 встроенные роли можно изменять, но нельзя удалять; пользовательские роли можно создавать и удалять (используемые защищены); права сгруппированы на системные и особые (экземпляры)
  - **Семантика суперадминистратора**: суперадмин определяется только флагом `is_super_admin` и обходит все проверки; роль `admin` больше не означает суперадмина
  - **Мгновенное применение**: изменения ролей и прав действуют со следующего запроса, без повторного входа
  - **Защита пароля RCON**: отправляется только пользователям с правом «Основная конфигурация»; RCON-соединения выполняет бэкенд
  - Видимость кнопок по каждому экземпляру (список экземпляров / конфиги / моды / консоль) на основе `myPerms`
- Удаление экземпляра теперь удаляет и его каталог на диске (rootPath должен находиться внутри корня экземпляров; символические ссылки не разыменовываются); диалог подтверждения показывает путь
- Ответы входа / `me()` включают массив `perms`; записи без прав возвращают 403

#### Исправлено
- **Уязвимость повышения прав**: старые эндпоинты `GET/PUT /instances/{id}/permissions` не проверяли права — любой вошедший пользователь мог выдать их себе; удалены и заменены матрицей на стороне пользователя (требуется `instance:grant`)
- Пустая проверка `InstanceService.requirePerm`: любой вошедший пользователь мог запускать/останавливать/создавать/изменять/удалять любые экземпляры → теперь проверяется по ACL
- Удаление пользователя, создававшего задачи, завершалось ошибкой 500 (внешний ключ `job_task.created_by` и др. без CASCADE) → ссылки очищаются перед удалением

#### Документация
- Добавлен `docs/permission-system-plan.md` (план, матрица прав, изменения API, журнал сквозной проверки)
- Описание системы прав добавлено во все три README

---

# 新增 RCON 实例控制台、登录安全策略与中英俄三语界面

> 0.4.0 更新日志（2026-10-30）· [中文](CHANGELOG.md) | [English](CHANGELOG.md#english) | [Русский](CHANGELOG.md#русский)

### 中文

#### 新增
- **实例控制台（RCON）**：新增独立页面（实例列表「控制台」入口），包括——
  - 玩家：在线列表（编号 / GUID / IP / 延迟 / 大厅状态，10 秒自动刷新）、踢出、封禁（时长 + 理由）、私聊、复制 GUID
  - 公告：全服广播（say -1）与单人私聊
  - 封禁管理：`bans.txt` 列表与解封（解封后自动 loadBans）、按 GUID/IP 直接封禁
  - 服务器控制：锁服 / 解锁、最大人数、定时重启（restarttime）、reassign、restart / restartServer / shutdown（超管）
  - 任务与事件：任务列表（missions）、切换任务（mission）、事件查询（events）
  - 热重载：loadBans / loadScripts / loadEvents / loadRPT；RPT 日志尾部查看
  - RCON 控制台：原始命令执行（超管），含命令与响应记录
- **登录安全**：
  - `admin` 仅允许服务器本机登录（无失败次数限制）
  - 其他账户连续登录失败自动锁定（阈值可配置，默认 3 次，范围 1-20，见「系统设置 → 登录安全」）
  - 超管在「用户管理」一键解锁并重置密码（16 位随机大小写字母 + 数字，仅显示一次）
  - 锁定后已登录 token 与 refresh token 立即失效
- **多语言界面**：中文 / English / Русский 三语切换（登录页与主界面右上角地球图标），选择持久化，Element Plus 组件文案随动
- 系统设置新增「登录失败锁定次数」配置项

#### 修复
- 重写 BattlEye RCON 协议实现：原封包格式与官方协议不符（缺 CRC32 校验 / 序号 / 分片处理），无法与真实服务器通信；现按官方协议（CRC32 小端校验 + 0xFF + 类型 + 序号）实现，支持多包响应与服务端消息 ACK

#### 文档
- README 增加三语互链与顶部三语一句话简介，新增俄文版（README.ru.md），补充结构注释与发布前隐私检查清单

### English

#### Added
- **Instance console (RCON)**: new standalone page (entry: "Console" button on the instance list) —
  - Players: live list (#, GUID, IP, ping, lobby state; auto-refresh every 10s), kick, ban (duration + reason), private message, copy GUID
  - Announcements: global broadcast (say -1) and per-player private messages
  - Ban management: list & unban from `bans.txt` (auto loadBans after unban), ban by GUID/IP
  - Server control: lock / unlock, max players, timed restart (restarttime), reassign, restart / restartServer / shutdown (super admin)
  - Missions & events: mission list (missions), switch mission (mission), event query (events)
  - Hot reload: loadBans / loadScripts / loadEvents / loadRPT; RPT log tail viewer
  - RCON console: raw command execution (super admin) with command/response history
- **Login security**:
  - `admin` may only sign in from the server machine (no failure limit)
  - Other accounts lock after consecutive failed logins (configurable threshold, default 3, range 1–20; Settings → Login security)
  - Super admin can unlock and reset the password in one click (16-char random upper/lower/digit, shown once)
  - Locking immediately invalidates existing access and refresh tokens
- **Multilingual UI**: switchable Chinese / English / Русский (globe icon at top-right of login page and main layout), choice persisted, Element Plus component copy follows
- New system setting: login failure lockout threshold

#### Fixed
- Rewrote the BattlEye RCON protocol implementation: the previous packet format did not match the official protocol (missing CRC32 checksum / sequence numbers / multi-packet handling) and could not talk to real servers; now implemented per the official protocol (little-endian CRC32 + 0xFF + type + sequence), with multi-packet responses and server-message ACKs

#### Docs
- README now has a tri-lingual header (one-line summary in three languages) and cross-language links; added a Russian version (README.ru.md); added structural comments and a pre-publish privacy checklist

### Русский

#### Добавлено
- **Консоль экземпляра (RCON)**: новая отдельная страница (кнопка «Консоль» в списке экземпляров) —
  - Игроки: живой список (#, GUID, IP, пинг, лобби; автообновление каждые 10 с), кик, бан (срок + причина), личное сообщение, копирование GUID
  - Объявления: глобальная рассылка (say -1) и личные сообщения игрокам
  - Управление банами: список и разбан из `bans.txt` (автоматический loadBans после разбана), бан по GUID/IP
  - Управление сервером: блокировка / разблокировка, максимум игроков, таймер перезапуска (restarttime), reassign, restart / restartServer / shutdown (суперадмин)
  - Миссии и события: список миссий (missions), смена миссии (mission), запрос событий (events)
  - Горячая перезагрузка: loadBans / loadScripts / loadEvents / loadRPT; просмотр хвоста журнала RPT
  - Консоль RCON: выполнение сырых команд (суперадмин) с историей команд и ответов
- **Безопасность входа**:
  - `admin` может входить только с сервера (без ограничения числа попыток)
  - Остальные аккаунты блокируются после нескольких неудачных попыток (порог настраивается, по умолчанию 3, диапазон 1–20; «Настройки → Безопасность входа»)
  - Суперадмин может разблокировать аккаунт и сбросить пароль одним нажатием (16 символов: буквы верхнего/нижнего регистра + цифры, показывается один раз)
  - Блокировка мгновенно отзывает выданные access- и refresh-токены
- **Многоязычный интерфейс**: переключение 中文 / English / Русский (иконка глобуса в правом верхнем углу страницы входа и основного интерфейса), выбор сохраняется, тексты Element Plus переключаются вместе с языком
- Новая системная настройка: порог блокировки при ошибках входа

#### Исправлено
- Переписана реализация протокола BattlEye RCON: прежний формат пакетов не соответствовал официальному протоколу (отсутствовали контрольная сумма CRC32 / номера последовательностей / сборка фрагментов) и не мог работать с реальными серверами; теперь реализация следует официальному протоколу (CRC32 little-endian + 0xFF + тип + последовательность), поддерживаются многочастные ответы и ACK служебных сообщений

#### Документация
- В README добавлены трёхъязычный заголовок (краткое описание на трёх языках) и ссылки между языками; добавлена русская версия (README.ru.md); добавлены структурные комментарии и чек-лист конфиденциальности перед публикацией
