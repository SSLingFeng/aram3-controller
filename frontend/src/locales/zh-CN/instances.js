export default {
  title: '实例管理',
  createBtn: '新建实例',
  columns: {
    code: 'Code',
    port: '端口',
    ip: 'IP'
  },
  actions: {
    console: '控制台',
    start: '启动',
    stop: '停止'
  },
  editTitle: '编辑实例',
  createTitle: '新建实例',
  form: {
    code: 'Code',
    bindIp: '绑定 IP',
    gamePort: '游戏端口',
    queryPort: '查询端口',
    rconPort: 'RCON 端口',
    rconPassword: 'RCON 密码',
    profileName: 'Profile 名',
    extraArgs: '自定义启动参数',
    codePlaceholder: '如 main-ops',
    bindIpPlaceholder: '非必填，如 192.0.2.1；不填则不加 -ip',
    portFlag: '-port=',
    profileNamePlaceholder: '-name= ；可选',
    extraArgsPlaceholder: '追加到命令行，例如：-filepatching -bandwidthalg=2 -loadmissiontomemory -cpucount=6 -limitfps=75',
    extraArgsHint: '按空格拆分追加；-mod/-serverMod 由系统按实例模组自动生成'
  },
  msg: {
    saved: '已保存',
    created: '实例已创建',
    startSubmitted: '已提交启动任务 #{id}',
    stopSubmitted: '已提交停止任务 #{id}',
    deleted: '已删除'
  }
}
