package com.aram3.infra.rcon;

/** One line of BattlEye bans.txt. `kind`: GUID | IP | OTHER. */
public record BanEntry(int lineNo, String raw, String target, String kind, String reason) {
}
