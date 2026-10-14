package com.aram3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aram3")
public class Aram3Properties {
    private final Jwt jwt = new Jwt();
    private final Paths paths = new Paths();
    private final Steam steam = new Steam();
    private final Process process = new Process();

    public Jwt getJwt() { return jwt; }
    public Paths getPaths() { return paths; }
    public Steam getSteam() { return steam; }
    public Process getProcess() { return process; }

    public static class Jwt {
        private String secret;
        private long accessTtlMinutes = 60;
        private long refreshTtlDays = 7;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTtlMinutes() { return accessTtlMinutes; }
        public void setAccessTtlMinutes(long accessTtlMinutes) { this.accessTtlMinutes = accessTtlMinutes; }
        public long getRefreshTtlDays() { return refreshTtlDays; }
        public void setRefreshTtlDays(long refreshTtlDays) { this.refreshTtlDays = refreshTtlDays; }
    }

    public static class Paths {
        private String dataDir = "./data";
        private String gameServerDir;
        private String steamcmdPath;
        private String workshopDir;
        private String storageDir;
        private String instancesDir;
        private String logsDir;
        public String getDataDir() { return dataDir; }
        public void setDataDir(String dataDir) { this.dataDir = dataDir; }
        public String getGameServerDir() { return gameServerDir; }
        public void setGameServerDir(String gameServerDir) { this.gameServerDir = gameServerDir; }
        public String getSteamcmdPath() { return steamcmdPath; }
        public void setSteamcmdPath(String steamcmdPath) { this.steamcmdPath = steamcmdPath; }
        public String getWorkshopDir() { return workshopDir; }
        public void setWorkshopDir(String workshopDir) { this.workshopDir = workshopDir; }
        public String getStorageDir() { return storageDir; }
        public void setStorageDir(String storageDir) { this.storageDir = storageDir; }
        public String getInstancesDir() { return instancesDir; }
        public void setInstancesDir(String instancesDir) { this.instancesDir = instancesDir; }
        public String getLogsDir() { return logsDir; }
        public void setLogsDir(String logsDir) { this.logsDir = logsDir; }
    }

    public static class Steam {
        private int appId = 233780;
        private int workshopAppId = 107410;
        public int getAppId() { return appId; }
        public void setAppId(int appId) { this.appId = appId; }
        public int getWorkshopAppId() { return workshopAppId; }
        public void setWorkshopAppId(int workshopAppId) { this.workshopAppId = workshopAppId; }
    }

    public static class Process {
        private boolean psPreferPwsh = true;
        public boolean isPsPreferPwsh() { return psPreferPwsh; }
        public void setPsPreferPwsh(boolean psPreferPwsh) { this.psPreferPwsh = psPreferPwsh; }
    }
}
