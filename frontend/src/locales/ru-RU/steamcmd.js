export default {
  title: 'Консоль SteamCMD',
  stopUpdate: 'Остановить обновление',
  alertStarting: 'SteamCMD запускается… После создания процесса журнал будет непрерывно выводиться, пожалуйста, подождите (запуск SteamCMD может занять некоторое время).',
  alertRunning: 'SteamCMD работает. Можно отправлять команды; нажмите «Остановить обновление», чтобы завершить процесс.',
  alertStopped: 'SteamCMD завершил работу, ввод команд заблокирован. Ниже доступны полный журнал и результаты локальной проверки после завершения.',
  session: 'Сеанс',
  pid: 'PID',
  mode: 'Режим',
  account: 'Учётная запись',
  logLines: 'Строк журнала',
  inputPlaceholder: 'Введите команду SteamCMD, например status / quit',
  inputLocked: 'SteamCMD не запущен, ввод заблокирован',
  sendCommand: 'Отправить команду',
  confirmStop: 'Остановить SteamCMD?',
  stopRequested: 'Запрошена остановка',
  status: {
    STARTING: 'Запускается',
    RUNNING: 'Работает',
    STOPPED: 'Остановлен',
    FAILED: 'Ошибка'
  },
  modes: {
    DIRECT: 'Прямое скачивание',
    REDOWNLOAD: 'Повторное скачивание'
  }
}
