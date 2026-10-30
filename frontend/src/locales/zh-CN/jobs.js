export default {
  title: '任务中心',
  columns: {
    type: '类型',
    target: '目标',
    targetId: '目标ID',
    progress: '进度',
    error: '错误',
    createdAt: '创建时间'
  },
  status: {
    PENDING: '等待中',
    QUEUED: '排队中',
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    CANCELLED: '已取消',
    TIMEOUT: '超时',
    STOPPED: '已停止'
  }
}
