package com.aram3.infra.rcon;

/**
 * 一条在线玩家记录（来自 RCON players 输出）。
 * 格式：{@code <id> <ip>:<port> <ping> <guid>(OK|?) <name>[ (Lobby)]}
 */
public record PlayerInfo(
        int num,
        String name,
        String guid,
        String ip,
        String ping,
        boolean verified,
        boolean lobby,
        String raw) {
}
