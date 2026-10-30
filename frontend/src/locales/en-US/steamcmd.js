export default {
  title: 'SteamCMD Console',
  stopUpdate: 'Stop Update',
  alertStarting: 'SteamCMD is starting… Logs will keep streaming once the process is created, please wait (SteamCMD itself may take a while to start).',
  alertRunning: 'SteamCMD is running. You can send commands; click "Stop Update" to end the process.',
  alertStopped: 'SteamCMD has ended and command input is locked. You can review the full log and the post-run local check results below.',
  session: 'Session',
  pid: 'PID',
  mode: 'Mode',
  account: 'Account',
  logLines: 'Log Lines',
  inputPlaceholder: 'Enter a SteamCMD command, e.g. status / quit',
  inputLocked: 'SteamCMD is not running, input is locked',
  sendCommand: 'Send Command',
  confirmStop: 'Stop SteamCMD?',
  stopRequested: 'Stop requested',
  status: {
    STARTING: 'Starting',
    RUNNING: 'Running',
    STOPPED: 'Stopped',
    FAILED: 'Failed'
  },
  modes: {
    DIRECT: 'Direct Download',
    REDOWNLOAD: 'Redownload'
  }
}
