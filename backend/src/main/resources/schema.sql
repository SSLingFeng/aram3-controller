-- Aram3 Controller schema (SQLite)
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS sys_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name TEXT,
  email TEXT,
  status INTEGER NOT NULL DEFAULT 1,
  is_super_admin INTEGER NOT NULL DEFAULT 0,
  login_fail_count INTEGER NOT NULL DEFAULT 0,
  locked INTEGER NOT NULL DEFAULT 0,
  last_login_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_role (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  description TEXT,
  is_system INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id INTEGER NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
  role_id INTEGER NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id INTEGER NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
  perm_code TEXT NOT NULL,
  PRIMARY KEY (role_id, perm_code)
);

CREATE TABLE IF NOT EXISTS game_instance (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  description TEXT,
  root_path TEXT NOT NULL,
  game_port INTEGER,
  steam_query_port INTEGER,
  bind_ip TEXT,
  rcon_host TEXT,
  rcon_port INTEGER,
  rcon_password TEXT,
  max_players INTEGER,
  profile_name TEXT,
  start_script TEXT,
  extra_args TEXT,
  status TEXT NOT NULL DEFAULT 'STOPPED',
  enabled INTEGER NOT NULL DEFAULT 1,
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_instance_perm (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
  instance_id INTEGER NOT NULL REFERENCES game_instance(id) ON DELETE CASCADE,
  can_view INTEGER NOT NULL DEFAULT 1,
  can_start_stop INTEGER NOT NULL DEFAULT 0,
  can_edit_config INTEGER NOT NULL DEFAULT 0,
  can_manage_map INTEGER NOT NULL DEFAULT 0,
  can_manage_mod INTEGER NOT NULL DEFAULT 0,
  can_transfer_files INTEGER NOT NULL DEFAULT 0,
  can_view_logs INTEGER NOT NULL DEFAULT 1,
  can_rcon INTEGER NOT NULL DEFAULT 0,
  can_schedule INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE (user_id, instance_id)
);

CREATE TABLE IF NOT EXISTS map_catalog (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  map_key TEXT,
  file_name TEXT NOT NULL,
  file_path TEXT NOT NULL,
  file_size INTEGER,
  checksum TEXT,
  source_type TEXT NOT NULL DEFAULT 'upload',
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS instance_map (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  instance_id INTEGER NOT NULL REFERENCES game_instance(id) ON DELETE CASCADE,
  map_id INTEGER NOT NULL REFERENCES map_catalog(id) ON DELETE CASCADE,
  is_active INTEGER NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  UNIQUE (instance_id, map_id)
);

CREATE TABLE IF NOT EXISTS mod_catalog (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  workshop_id TEXT NOT NULL UNIQUE,
  author TEXT,
  version TEXT,
  file_name TEXT,
  file_path TEXT,
  file_size INTEGER,
  checksum TEXT,
  default_load_type TEXT NOT NULL DEFAULT 'BOTH',
  download_status TEXT NOT NULL DEFAULT 'PENDING',
  description TEXT,
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS instance_mod (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  instance_id INTEGER NOT NULL REFERENCES game_instance(id) ON DELETE CASCADE,
  mod_id INTEGER NOT NULL REFERENCES mod_catalog(id) ON DELETE CASCADE,
  load_type TEXT NOT NULL CHECK (load_type IN ('SERVER_ONLY', 'BOTH')),
  enabled INTEGER NOT NULL DEFAULT 1,
  load_order INTEGER NOT NULL DEFAULT 0,
  custom_args TEXT,
  installed_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE (instance_id, mod_id)
);

CREATE TABLE IF NOT EXISTS steam_account (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password_enc TEXT NOT NULL,
  guard_token_enc TEXT,
  is_default INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'OK',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS game_server_install (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  install_path TEXT NOT NULL,
  steamcmd_path TEXT NOT NULL,
  app_id INTEGER NOT NULL DEFAULT 233780,
  workshop_app_id INTEGER NOT NULL DEFAULT 107410,
  steam_account_id INTEGER NOT NULL REFERENCES steam_account(id),
  version TEXT,
  build_id TEXT,
  status TEXT NOT NULL DEFAULT 'OK',
  last_update_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS instance_schedule (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  instance_id INTEGER NOT NULL REFERENCES game_instance(id) ON DELETE CASCADE,
  action TEXT NOT NULL CHECK (action IN ('START', 'STOP', 'RESTART')),
  cron_expr TEXT NOT NULL,
  timezone TEXT NOT NULL DEFAULT 'Asia/Shanghai',
  enabled INTEGER NOT NULL DEFAULT 1,
  next_run_at TEXT,
  last_run_at TEXT,
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS job_task (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  job_type TEXT NOT NULL,
  target_type TEXT,
  target_id INTEGER,
  payload_json TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING',
  progress INTEGER NOT NULL DEFAULT 0,
  log_path TEXT,
  error_message TEXT,
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL,
  started_at TEXT,
  finished_at TEXT
);

CREATE TABLE IF NOT EXISTS backup_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  file_path TEXT NOT NULL,
  file_size INTEGER,
  checksum TEXT,
  status TEXT NOT NULL DEFAULT 'SUCCESS',
  created_by INTEGER REFERENCES sys_user(id),
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER REFERENCES sys_user(id),
  action TEXT NOT NULL,
  target_type TEXT,
  target_id INTEGER,
  detail_json TEXT,
  ip TEXT,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_setting (
  key TEXT PRIMARY KEY,
  value TEXT,
  description TEXT,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_token (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  revoked INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_status ON job_task(status, created_at);
CREATE INDEX IF NOT EXISTS idx_instance_mod_inst ON instance_mod(instance_id, enabled, load_order);
CREATE INDEX IF NOT EXISTS idx_schedule_inst ON instance_schedule(instance_id, enabled);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_user_inst_perm ON user_instance_perm(user_id, instance_id);
