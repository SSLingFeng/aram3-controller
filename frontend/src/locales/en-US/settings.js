export default {
  title: 'System Settings',
  saveSettings: 'Save Settings',
  alertTop: 'Changes take effect immediately after saving. Please use absolute paths (e.g. D:\\Aram3\\...). Fill in the four categories below — Game Server / Mods / Per-instance / App Data — and avoid mixing them.',
  cards: {
    gameServer: '1. Game Server (installed by SteamCMD, shared by instances)',
    mods: '2. Mods (where Workshop downloads are stored)',
    instances: '3. Per-instance Configuration (subfolders named after each instance)',
    app: '4. This Application (logs and database, not game files)',
    loginSecurity: '5. Login Security',
    readonly: 'Read-only Information'
  },
  gameServerDir: 'Game Server Install Directory',
  steamcmdPath: 'SteamCMD Executable Path',
  modsDir: 'Mods Storage Directory',
  instancesDir: 'Instance Config Root Directory',
  logsDir: 'Logs Directory',
  dataDir: 'Data Directory',
  maxLoginFailures: 'Failed Logins Before Lockout',
  mpmissionsDir: 'Actual Maps Directory',
  steamAppId: 'Game AppID',
  workshopAppId: 'Workshop AppID',
  placeholders: {
    gameServerDir: 'e.g. D:\\Aram3\\game-server',
    steamcmdPath: 'e.g. D:\\steamcmd\\steamcmd.exe',
    modsDir: 'e.g. D:\\Aram3\\mods',
    instancesDir: 'e.g. D:\\Aram3\\instances',
    logsDir: 'e.g. D:\\Aram3\\logs',
    dataDir: 'e.g. D:\\Aram3\\data'
  },
  hints: {
    gameServerDir: 'Directory where SteamCMD installs the Arma3 dedicated server; it contains arma3server_x64.exe. Maps are placed automatically under mpmissions\\ in this directory',
    steamcmdPath: 'Path to steamcmd.exe itself, used to download the game and update mods (a real Steam account is required)',
    modsDir: 'Workshop mods are downloaded here; the mod library also scans this directory to detect local downloads. This is the same directory used for Workshop downloads',
    instancesDir: 'One subdirectory per instance (e.g. instances\\main-ops\\) holding server.cfg, basic.cfg, profiles, etc. New instances create their folder here',
    logsDir: 'Startup logs and SteamCMD task logs produced by this controller, not the game server\'s own logs',
    dataDir: 'Stores the SQLite database files (users, instances, mod records, etc.); back up this directory first',
    maxLoginFailures: 'Non-admin accounts are locked after this many consecutive failed logins; a super admin must unlock and reset the password on the Users page. The admin account has no failed-login limit (local login only)',
    mpmissionsDir: 'Fixed = game server install directory + \\mpmissions; uploaded .pbo files are placed here'
  },
  msg: {
    saved: 'Settings saved'
  }
}
