export default {
  title: 'Задачи',
  columns: {
    type: 'Тип',
    target: 'Объект',
    targetId: 'ID объекта',
    progress: 'Прогресс',
    error: 'Ошибка',
    createdAt: 'Создано'
  },
  status: {
    PENDING: 'Ожидание',
    QUEUED: 'В очереди',
    RUNNING: 'Выполняется',
    SUCCESS: 'Успешно',
    FAILED: 'Ошибка',
    CANCELLED: 'Отменено',
    TIMEOUT: 'Тайм-аут',
    STOPPED: 'Остановлено'
  }
}
