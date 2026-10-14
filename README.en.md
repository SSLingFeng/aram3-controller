This project’s source code was developed entirely with **MiMo V2.6 Pro**, using natural-language-driven development.

# Aram3 Web Server Controller

A multi-instance web control panel for Arma 3 (AppID 233780), built with **JDK 21 + Spring Boot + Vue 3 + Element Plus + SQLite**.

**Single deployable**: Vue production build is packed into Spring Boot `static/`; one jar serves both UI and API.

## Features (current)

- JWT login; first boot prints a random admin password to backend logs
- **User management**: list / create / edit (display name, email, roles, super admin) / reset password / enable-disable / delete
- **System settings**: game server install dir, SteamCMD path, mods/Workshop dir, instance root, logs/data dirs (hot-applied)
- Instance CRUD, start/stop, start-command preview (absolute `server.cfg` / `basic.cfg`, `-ip`, custom args)
- Map PBO upload (auto `mapKey`) / delete; files go to game `mpmissions`
- Workshop mods: HTML preset import, update (Steam account + optional guard code), live SteamCMD console
- Mod local folder scan status; **Move Keys** to `keys` folder; key management (source, delete, upload)
- Per-instance mod load list (client+server / server-only)
- Job center, RCON (players / kick / broadcast)
- PowerShell auto-detect (pwsh 7 → powershell 5)
- SQLite auto-init; Nginx reverse-proxy friendly (`VITE_BASE` / `VITE_API_BASE`)

## Layout

```
WebFroAramaServer/
├─ backend/     Spring Boot (includes static/ frontend build)
├─ frontend/    Vue3 source
├─ docs/        Design notes
├─ scripts/     Build scripts, Nginx sample
├─ LICENSE      License (no third-party commercial use)
├─ README.md
└─ README.en.md
```

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
cd frontend
npm install
npm run build

cd ..\backend
mvn spring-boot:run   # use your Maven path
```

Open **http://127.0.0.1:8080**.  
On **first start**, the backend log prints a random `admin` password (6 chars: upper + lower + digit + symbol). Change it after login.

Optional dev mode:

```powershell
cd frontend
npm run dev
```

## System settings

| Item | Meaning |
|---|---|
| Game server install dir | Contains `arma3server_x64.exe`; maps under `mpmissions\` |
| SteamCMD path | `steamcmd.exe` |
| Mods/Workshop dir | Shared folder for Workshop downloads and mod library |
| Instance config root | One subfolder per instance (cfg / profiles) |
| Logs dir | Controller / job logs |
| Data dir | SQLite etc. (back this up) |

## License

See [LICENSE](LICENSE).  
**Allowed**: personal, educational, and other non-commercial use and modification.  
**Not allowed**: third-party commercial use (selling, paid SaaS, embedding in commercial products, etc.) without written permission from the copyright holder.

Chinese README: [README.md](README.md).

## Before publishing to GitHub (privacy)

Do **not** commit:

- `data/` (SQLite, `server.cfg`, RCON-related)
- `*.log` (may contain Steam account / SteamID)
- Absolute local paths, real public IPs, Steam accounts, guard codes, RCON passwords

Production must set: `ARAM3_JWT_SECRET`.

## Versions

- Java 21; Spring Boot 3.5.x as the current compile baseline
- Spring Boot 4: change the parent version
