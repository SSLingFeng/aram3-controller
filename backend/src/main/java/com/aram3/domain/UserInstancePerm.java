package com.aram3.domain;

public record UserInstancePerm(
        Long id,
        Long userId,
        Long instanceId,
        int canView,
        int canStartStop,
        int canEditCore,
        int canEditConfig,
        int canManageMap,
        int canManageMod,
        int canTransferFiles,
        int canViewLogs,
        int canRcon,
        int canSchedule
) {
    public boolean hasView() { return canView == 1; }
    public boolean hasStartStop() { return canStartStop == 1; }
    public boolean hasEditCore() { return canEditCore == 1; }
    public boolean hasEditConfig() { return canEditConfig == 1; }
    public boolean hasManageMap() { return canManageMap == 1; }
    public boolean hasManageMod() { return canManageMod == 1; }
    public boolean hasTransferFiles() { return canTransferFiles == 1; }
    public boolean hasViewLogs() { return canViewLogs == 1; }
    public boolean hasRcon() { return canRcon == 1; }
    public boolean hasSchedule() { return canSchedule == 1; }
}
