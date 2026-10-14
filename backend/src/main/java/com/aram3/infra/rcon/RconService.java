package com.aram3.infra.rcon;

import com.aram3.common.BusinessException;
import com.aram3.domain.GameInstance;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal BattlEye RCON client (UDP).
 * Covers: login, command, player list / kick / say.
 */
@Service
public class RconService {
    private static final byte[] MAGIC = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final AtomicInteger SEQ = new AtomicInteger();

    public List<String> players(GameInstance inst) {
        String resp = command(inst, "players");
        return parsePlayers(resp);
    }

    public void kick(GameInstance inst, int playerId, String reason) {
        String msg = reason == null || reason.isBlank() ? "kicked by admin" : reason;
        command(inst, "kick " + playerId + " " + msg);
    }

    public void say(GameInstance inst, int playerId, String message) {
        // -1 = global
        command(inst, "say " + playerId + " " + message);
    }

    public String command(GameInstance inst, String command) {
        if (inst.rconHost() == null || inst.rconPort() == null || inst.rconPassword() == null) {
            throw new BusinessException("实例未配置 RCON");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(inst.rconHost(), inst.rconPort()), 5000);
            socket.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            byte[] login = packet((byte) 0x00, inst.rconPassword());
            out.write(login);
            out.flush();
            byte[] loginResp = readPacket(in);
            if (loginResp.length < 5 || loginResp[4] != 0x01) {
                throw new BusinessException("RCON 登录失败");
            }

            int seq = SEQ.incrementAndGet() & 0xFF;
            out.write(packet((byte) 0x01, command));
            out.flush();
            byte[] resp = readPacket(in);
            return extractPayload(resp);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("RCON 通信失败: " + e.getMessage());
        }
    }

    private byte[] packet(byte type, String payload) {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[7 + body.length];
        packet[0] = 'B';
        packet[1] = 'E';
        packet[2] = 0;
        packet[3] = 0;
        packet[4] = 0;
        packet[5] = 0;
        packet[6] = type;
        System.arraycopy(body, 0, packet, 7, body.length);
        return packet;
    }

    private byte[] readPacket(DataInputStream in) throws IOException {
        byte[] header = new byte[7];
        in.readFully(header);
        int size = ((header[2] & 0xFF)) | ((header[3] & 0xFF) << 8)
                | ((header[4] & 0xFF) << 16) | ((header[5] & 0xFF) << 24);
        int payloadLen = Math.max(0, size - 1);
        byte[] payload = new byte[payloadLen];
        if (payloadLen > 0) {
            in.readFully(payload);
        }
        byte[] result = new byte[payload.length + 1];
        result[0] = header[6];
        System.arraycopy(payload, 0, result, 1, payload.length);
        return result;
    }

    private String extractPayload(byte[] packet) {
        if (packet.length <= 1) {
            return "";
        }
        return new String(packet, 1, packet.length - 1, StandardCharsets.UTF_8);
    }

    private List<String> parsePlayers(String resp) {
        List<String> players = new ArrayList<>();
        if (resp == null || resp.isBlank()) {
            return players;
        }
        for (String line : resp.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("Players") || trimmed.startsWith("#")
                    || trimmed.contains("GUID") && trimmed.contains("Name")) {
                continue;
            }
            players.add(trimmed);
        }
        return players;
    }
}
