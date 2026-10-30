<!--
  README language navigation (maintainer comment, not rendered):
  This is the English version. When adding a new language, keep the "one-line summary"
  and "language links" blocks at the top in sync across README.md / README.en.md / README.ru.md.
-->

> **中文**：Arma 3 专用服务器网页管理面板：实例、模组、地图与 RCON 一体化管理。
> **English**: Web control panel for Arma 3 dedicated servers — instances, mods, maps and RCON in one place.
> **Русский**: Веб-панель управления выделенными серверами Arma 3 — экземпляры, моды, карты и RCON в одном месте.

[中文](README.md) | [English](README.en.md) | [Русский](README.ru.md)

<!-- ====== Project intro ====== -->

This project’s source code was developed entirely with **MiMo V2.6 Pro**, using natural-language-driven development.

# Aram3 Web Server Controller

A multi-instance web control panel for Arma 3 (AppID 233780), built with **JDK 21 + Spring Boot + Vue 3 + Element Plus + SQLite**.

**Single deployable**: Vue production build is packed into Spring Boot `static/`; one jar serves both UI and API.

<!-- ====== Feature list: update all three README versions when adding features ====== -->

## Features (current)

- JWT login; first boot prints a random admin password to backend logs
- **User management**: list / create / edit (display name, email, roles, super admin) / reset password / enable-disable / delete
- **System settings**: game server install dir, SteamCMD path, mods/Workshop dir, instance root, logs/data dirs (hot-applied)
- Instance CRUD, start/stop, start-command preview (absolute `server.cfg` / `basic.cfg`, `-ip`, custom args)
- Map PBO upload (auto `mapKey`) / delete; files go to game `mpmissions`
- Workshop mods: HTML preset import, update (Steam account + optional guard code), live SteamCMD console
- Mod local folder scan status; **Move Keys** to `keys` folder; key management (source, delete, upload)
- Per-instance mod load list (client+server / server-only)
- Job center
- **Instance console (RCON)**: live player list & details (GUID / IP / ping / lobby), kick / ban (duration + reason), global announcements & private messages, ban management (`bans.txt` unban, ban by GUID/IP), lock / unlock, max players, timed restart, mission switching (missions / mission), hot reload (loadBans / loadScripts / loadEvents / loadRPT), RPT log tail, raw RCON command console
- **Login security**: `admin` may only sign in from the server machine (no failure limit); other accounts lock after consecutive failed logins (threshold configurable, default 3); super admin can unlock and reset a 16-char random password (upper+lower+digit) in user management; locked accounts invalidate existing tokens immediately
- **Multilingual UI**: Chinese / English / Русский, switchable via the globe icon at the top-right of the login page and the main layout; choice is persisted
- PowerShell auto-detect (pwsh 7 → powershell 5)
- SQLite auto-init; Nginx reverse-proxy friendly (`VITE_BASE` / `VITE_API_BASE`)

<!-- ====== Layout: keep this section in sync with the repo root ====== -->

## Layout

```
WebFroAramaServer/
├─ backend/     Spring Boot (includes static/ frontend build)
├─ frontend/    Vue3 source
├─ docs/        Design notes
├─ scripts/     Build scripts, Nginx sample
├─ LICENSE      License (no third-party commercial use)
├─ README.md      中文 (default)
├─ README.en.md   English
└─ README.ru.md   Русский
```

<!-- ====== Build & run: commands verified on Windows PowerShell ====== -->

## Build & run (all-in-one)

### Opening in IDEA

| Open path | Spring Boot | Frontend build run configs |
|---|---|---|
| Repo root | Aggregator `pom.xml` (reload Maven to import `backend`) | Yes |
| `backend/` | Detected as Spring Boot | Yes |

If Spring Boot is not detected from root: right-click `backend/pom.xml` → Add as Maven Project.

### Run configurations

| Name | Purpose |
|---|---|
| **Aram3Application** | Start Spring Boot (http://127.0.0.1:8080) |
| **构建前端** | `scripts\build-frontend.cmd` |
| **构建前端(npm)** | `npm run build` |
| **打包前后端** | Frontend build + Maven package |

Suggested order: **build frontend** → **Aram3Application**.

### Manual

```powershell
# Step 1: install dependencies and build the frontend; output goes to backend/src/main/resources/static
cd frontend
npm install
npm run build

# Step 2: start the backend (use your Maven path; in IDEA just run Aram3Application)
cd ..\backend
mvn spring-boot:run
```

Open **http://127.0.0.1:8080**.  
On **first start**, the backend log prints a random `admin` password (6 chars: upper + lower + digit + symbol). Change it after login.

Optional dev mode (separate frontend dev server with hot reload):

```powershell
# Starts only the frontend dev server (point VITE_API_BASE at the backend)
cd frontend
npm run dev
```

<!-- ====== System settings: keep the table aligned with the Settings page ====== -->

## System settings

| Item | Meaning |
|---|---|
| Game server install dir | Contains `arma3server_x64.exe`; maps under `mpmissions\` |
| SteamCMD path | `steamcmd.exe` |
| Mods/Workshop dir | Shared folder for Workshop downloads and mod library |
| Instance config root | One subfolder per instance (cfg / profiles) |
| Logs dir | Controller / job logs |
| Data dir | SQLite etc. (back this up) |
| Login failure lockout threshold | Consecutive failed logins before non-`admin` accounts lock (1–20, default 3; see Settings → Login security) |

<!-- ====== Privacy checklist: verify before publishing to GitHub ====== -->

## Before publishing to GitHub (privacy)

Do **not** commit:

- `data/` (SQLite, `server.cfg`, RCON-related)
- `*.log` (may contain Steam account / SteamID)
- Absolute local paths, real public IPs, Steam accounts, guard codes, RCON passwords

Production must set: `ARAM3_JWT_SECRET`.

<!-- ====== License ====== -->

## License

See [LICENSE](LICENSE).  
**Allowed**: personal, educational, and other non-commercial use and modification.  
**Not allowed**: third-party commercial use (selling, paid SaaS, embedding in commercial products, etc.) without written permission from the copyright holder.

Other languages: [中文](README.md) · [Русский](README.ru.md)

<!-- ====== Versions: update when upgrading frameworks ====== -->

## Versions

- Java 21; Spring Boot 3.5.x as the current compile baseline
- Spring Boot 4: change the parent version
