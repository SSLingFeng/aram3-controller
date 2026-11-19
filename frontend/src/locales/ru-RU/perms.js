export default {
  groups: {
    SYSTEM: 'Системные права',
    INSTANCE: 'Специальные права (экземпляры)'
  },
  codes: {
    'user:manage': 'Управление пользователями',
    'system:settings': 'Настройки системы',
    'job:manage': 'Управление задачами',
    'map:manage': 'Управление картами',
    'mod:manage': 'Управление модами',
    'steamcmd:use': 'Использование SteamCMD',
    'audit:view': 'Просмотр аудита',
    'instance:create': 'Создание экземпляров',
    'instance:grant': 'Выдача прав на экземпляры'
  },
  instance: {
    canView: 'Просмотр',
    canStartStop: 'Запуск/остановка',
    canEditCore: 'Основная конфигурация',
    canEditConfig: 'Конфигурация экземпляра',
    canManageMap: 'Карты',
    canManageMod: 'Моды',
    canTransferFiles: 'Передача файлов',
    canViewLogs: 'Журналы',
    canRcon: 'RCON',
    canSchedule: 'Расписания'
  }
}
