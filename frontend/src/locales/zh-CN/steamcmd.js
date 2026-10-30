export default {
  title: 'SteamCMD 控制台',
  stopUpdate: '停止更新',
  alertStarting: 'SteamCMD 启动中… 进程创建后会持续输出日志，请稍候（SteamCMD 自身启动可能较慢）。',
  alertRunning: 'SteamCMD 运行中。可发送指令；点击「停止更新」可结束进程。',
  alertStopped: 'SteamCMD 已结束，指令输入已锁定。下方可查看完整日志与结束后的本地检测结果。',
  session: '会话',
  pid: 'PID',
  mode: '模式',
  account: '账号',
  logLines: '日志行',
  inputPlaceholder: '输入 SteamCMD 指令，例如 status / quit',
  inputLocked: 'SteamCMD 未运行，输入已锁定',
  sendCommand: '发送指令',
  confirmStop: '确认停止 SteamCMD？',
  stopRequested: '已请求停止',
  status: {
    STARTING: '启动中',
    RUNNING: '运行中',
    STOPPED: '已停止',
    FAILED: '失败'
  },
  modes: {
    DIRECT: '直接下载',
    REDOWNLOAD: '重新下载'
  }
}
