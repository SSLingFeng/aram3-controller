export default {
  title: 'Instance Management',
  createBtn: 'New Instance',
  columns: {
    code: 'Code',
    port: 'Ports',
    ip: 'IP'
  },
  actions: {
    console: 'Console',
    start: 'Start',
    stop: 'Stop'
  },
  editTitle: 'Edit Instance',
  createTitle: 'New Instance',
  form: {
    code: 'Code',
    bindIp: 'Bind IP',
    gamePort: 'Game Port',
    queryPort: 'Query Port',
    rconPort: 'RCON Port',
    rconPassword: 'RCON Password',
    profileName: 'Profile Name',
    extraArgs: 'Custom Launch Arguments',
    codePlaceholder: 'e.g. main-ops',
    bindIpPlaceholder: 'Optional, e.g. 192.0.2.1; leave empty to omit -ip',
    portFlag: '-port=',
    profileNamePlaceholder: '-name= (optional)',
    extraArgsPlaceholder: 'Appended to the command line, e.g. -filepatching -bandwidthalg=2 -loadmissiontomemory -cpucount=6 -limitfps=75',
    extraArgsHint: 'Split by spaces and appended; -mod/-serverMod are generated automatically from the instance mods'
  },
  msg: {
    saved: 'Saved',
    created: 'Instance created',
    startSubmitted: 'Start task #{id} submitted',
    stopSubmitted: 'Stop task #{id} submitted',
    deleted: 'Deleted'
  }
}
