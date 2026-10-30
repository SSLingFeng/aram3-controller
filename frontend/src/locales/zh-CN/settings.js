export default {
  title: '系统设置',
  saveSettings: '保存设置',
  alertTop: '保存后立即生效。路径请填绝对路径（例如 D:\\Aram3\\...）。下面按「游戏本体 / 模组 / 每个服务器实例 / 程序数据」四类填写，避免混用。',
  cards: {
    gameServer: '一、游戏服务器（SteamCMD 下载的本体，多个实例共用）',
    mods: '二、模组（Workshop 下载后存放的地方）',
    instances: '三、每个实例自己的配置（按实例名分子文件夹）',
    app: '四、本程序自身（日志与数据库，不是游戏文件）',
    loginSecurity: '五、登录安全',
    readonly: '只读信息'
  },
  gameServerDir: '游戏服务器安装目录',
  steamcmdPath: 'SteamCMD 程序路径',
  modsDir: '模组存储目录',
  instancesDir: '实例配置根目录',
  logsDir: '日志目录',
  dataDir: '数据目录',
  maxLoginFailures: '登录失败锁定次数',
  mpmissionsDir: '地图实际目录',
  steamAppId: '游戏本体 AppID',
  workshopAppId: 'Workshop AppID',
  placeholders: {
    gameServerDir: '例如 D:\\Aram3\\game-server',
    steamcmdPath: '例如 D:\\steamcmd\\steamcmd.exe',
    modsDir: '例如 D:\\Aram3\\mods',
    instancesDir: '例如 D:\\Aram3\\instances',
    logsDir: '例如 D:\\Aram3\\logs',
    dataDir: '例如 D:\\Aram3\\data'
  },
  hints: {
    gameServerDir: 'SteamCMD 安装 Arma3 专用服的目录，里面有 arma3server_x64.exe；地图自动放在该目录下的 mpmissions\\',
    steamcmdPath: '指向 steamcmd.exe 本体，用于下载游戏、更新模组（需要真实 Steam 账号）',
    modsDir: 'Workshop 模组下载后落在这里，模组库扫描本地是否已下载也看这个目录；与 Workshop 下载是同一目录',
    instancesDir: '每个实例一个子目录（如 instances\\main-ops\\），放 server.cfg、basic.cfg、profiles 等；新建实例会在这里建文件夹',
    logsDir: '控制器产生的启动日志、SteamCMD 任务日志，不是游戏服务器自己的日志',
    dataDir: '存 SQLite 数据库文件（用户、实例、模组记录等）；备份主要备这里',
    maxLoginFailures: '非 admin 账户连续登录失败达到该次数即锁定，需超管在「用户管理」解锁并重置密码；admin 无失败次数限制（仅限本机登录）',
    mpmissionsDir: '固定 = 游戏服务器安装目录 + \\mpmissions，上传的 .pbo 会放这里'
  },
  msg: {
    saved: '设置已保存'
  }
}
