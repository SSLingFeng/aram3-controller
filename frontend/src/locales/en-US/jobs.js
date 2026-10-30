export default {
  title: 'Jobs',
  columns: {
    type: 'Type',
    target: 'Target',
    targetId: 'Target ID',
    progress: 'Progress',
    error: 'Error',
    createdAt: 'Created At'
  },
  status: {
    PENDING: 'Pending',
    QUEUED: 'Queued',
    RUNNING: 'Running',
    SUCCESS: 'Success',
    FAILED: 'Failed',
    CANCELLED: 'Cancelled',
    TIMEOUT: 'Timed Out',
    STOPPED: 'Stopped'
  }
}
