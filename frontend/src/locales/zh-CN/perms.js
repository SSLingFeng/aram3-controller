export default {
  groups: {
    SYSTEM: '系统权限',
    INSTANCE: '特殊权限（实例）'
  },
  codes: {
    'user:manage': '用户管理',
    'system:settings': '系统设置',
    'job:manage': '任务中心管理',
    'map:manage': '地图库管理',
    'mod:manage': '模组库管理',
    'steamcmd:use': 'SteamCMD 使用',
    'audit:view': '审计查看',
    'instance:create': '新建实例',
    'instance:grant': '实例授权'
  },
  instance: {
    canView: '查看',
    canStartStop: '启停',
    canEditCore: '核心配置',
    canEditConfig: '实例配置',
    canManageMap: '地图',
    canManageMod: '模组',
    canTransferFiles: '文件传输',
    canViewLogs: '日志',
    canRcon: 'RCON',
    canSchedule: '定时任务'
  }
}
