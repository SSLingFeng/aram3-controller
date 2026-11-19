export default {
  groups: {
    SYSTEM: 'System Permissions',
    INSTANCE: 'Special Permissions (Instance)'
  },
  codes: {
    'user:manage': 'User Management',
    'system:settings': 'System Settings',
    'job:manage': 'Jobs Management',
    'map:manage': 'Maps Management',
    'mod:manage': 'Mods Management',
    'steamcmd:use': 'SteamCMD Usage',
    'audit:view': 'Audit Viewing',
    'instance:create': 'Create Instance',
    'instance:grant': 'Instance Granting'
  },
  instance: {
    canView: 'View',
    canStartStop: 'Start/Stop',
    canEditCore: 'Core Config',
    canEditConfig: 'Instance Config',
    canManageMap: 'Maps',
    canManageMod: 'Mods',
    canTransferFiles: 'File Transfer',
    canViewLogs: 'Logs',
    canRcon: 'RCON',
    canSchedule: 'Scheduled Jobs'
  }
}
