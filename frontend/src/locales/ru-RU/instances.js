export default {
  title: 'Управление экземплярами',
  createBtn: 'Новый экземпляр',
  columns: {
    code: 'Код',
    port: 'Порты',
    ip: 'IP'
  },
  actions: {
    console: 'Консоль',
    start: 'Запустить',
    stop: 'Остановить'
  },
  editTitle: 'Редактирование экземпляра',
  createTitle: 'Новый экземпляр',
  form: {
    code: 'Код',
    bindIp: 'Привязка IP',
    gamePort: 'Игровой порт',
    queryPort: 'Порт запросов',
    rconPort: 'Порт RCON',
    rconPassword: 'Пароль RCON',
    profileName: 'Имя профиля',
    extraArgs: 'Дополнительные аргументы запуска',
    codePlaceholder: 'например, main-ops',
    bindIpPlaceholder: 'Необязательно, например 192.0.2.1; если не указано, параметр -ip не добавляется',
    portFlag: '-port=',
    profileNamePlaceholder: '-name= (необязательно)',
    extraArgsPlaceholder: 'Добавляется к командной строке, например: -filepatching -bandwidthalg=2 -loadmissiontomemory -cpucount=6 -limitfps=75',
    extraArgsHint: 'Разделяется пробелами и добавляется к командной строке; -mod/-serverMod формируются автоматически на основе модов экземпляра'
  },
  msg: {
    saved: 'Сохранено',
    created: 'Экземпляр создан',
    startSubmitted: 'Отправлена задача запуска #{id}',
    stopSubmitted: 'Отправлена задача остановки #{id}',
    deleted: 'Удалено'
  }
}
