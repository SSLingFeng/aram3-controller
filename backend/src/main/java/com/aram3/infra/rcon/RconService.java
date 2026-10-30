package com.aram3.infra.rcon;

import com.aram3.common.BusinessException;
import com.aram3.domain.GameInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * BattlEye RCON client (UDP)。
 *
 * <p>包格式（与官方 BERConProtocol 及 battle-node / arcon.js 等生产实现一致）：
 * <pre>
 *   'B' 'E' + CRC32(payload, 小端 4 字节) + payload
 *   payload = 0xFF + type(0 登录 / 1 命令 / 2 服务端消息) + [seq(命令与消息)] + data
 * </pre>
 *
 * @see <a href="https://www.battleye.com/downloads/BERConProtocol.pdf">BERConProtocol.pdf</a>
 */
@Service
public class RconService {
    private static final Logger log = LoggerFactory.getLogger(RconService.class);

    private static final byte TYPE_LOGIN = 0x00;
    private static final byte TYPE_COMMAND = 0x01;
    private static final byte TYPE_MESSAGE = 0x02;

    private static final int LOGIN_TIMEOUT_MS = 3000;
    private static final int RESPONSE_TIMEOUT_MS = 5000;
    private static final int MAX_PACKETS = 128;

    /** Arma 3 BE players 行：id ip:port ping guid(OK|?) name[ (Lobby)] */
    private static final Pattern PLAYER_ARMA3 = Pattern.compile(
            "^(\\d+)\\s+([\\d.]+):\\d+\\s+(-?\\d+)\\s+([a-z0-9]{32}|-)(?:\\((\\?|OK)\\)|)\\s+(.+?)(?: \\((Lobby)\\)|)\\s*$",
            Pattern.CASE_INSENSITIVE);
    /** 通用回退：num : name : guid : ip : ping */
    private static final Pattern PLAYER_GENERIC = Pattern.compile("^(\\d+)\\s*[:\\s]+(.*)$");
    private static final Pattern GUID = Pattern.compile("^[0-9a-fA-F]{32}$|^\\d{17}$");
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d{1,5})?$");

    // ---------------------------------------------------------------- 协议层

    /** 执行任意 RCON 命令并返回响应文本（自动处理序号、分片、服务端消息 ACK）。 */
    public String exec(GameInstance inst, String command) {
        String cmd = sanitizeCommand(command);
        try (DatagramSocket socket = connect(inst)) {
            byte sequence = nextSequence();
            send(socket, commandPayload(sequence, cmd));
            return collectResponse(socket, sequence);
        } catch (SocketTimeoutException e) {
            throw new BusinessException("RCON 命令超时：" + cmd);
        } catch (IOException e) {
            throw new BusinessException("RCON 通信失败：" + e.getMessage());
        }
    }

    private DatagramSocket connect(GameInstance inst) throws IOException {
        if (inst.rconHost() == null || inst.rconHost().isBlank()
                || inst.rconPort() == null || inst.rconPort() <= 0
                || inst.rconPassword() == null || inst.rconPassword().isBlank()) {
            throw new BusinessException("该实例未配置 RCON（地址/端口/密码），请先在实例设置中填写");
        }
        DatagramSocket socket = new DatagramSocket();
        socket.connect(new InetSocketAddress(inst.rconHost(), inst.rconPort()));
        login(socket, inst.rconPassword());
        return socket;
    }

    private void login(DatagramSocket socket, String password) throws IOException {
        byte[] payload = new byte[2 + password.getBytes(StandardCharsets.UTF_8).length];
        payload[0] = (byte) 0xFF;
        payload[1] = TYPE_LOGIN;
        byte[] pwd = password.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(pwd, 0, payload, 2, pwd.length);
        send(socket, payload);

        try {
            socket.setSoTimeout(LOGIN_TIMEOUT_MS);
            while (true) {
                byte[] reply = parsePacket(receiveDatagram(socket));
                if (reply.length < 3 || reply[1] != TYPE_LOGIN) {
                    continue;
                }
                if (reply[2] == 1) {
                    return;
                }
                throw new BusinessException("RCON 登录失败：密码错误");
            }
        } catch (SocketTimeoutException e) {
            throw new BusinessException("RCON 登录失败：服务器无响应（密码错误或地址/端口不对）");
        } finally {
            socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
        }
    }

    /** 收集指定序号的命令响应；顺带 ACK 服务端消息（type 2）。 */
    private String collectResponse(DatagramSocket socket, byte sequence) throws IOException {
        StringBuilder text = new StringBuilder();
        int expectedParts = -1;
        List<String> parts = new ArrayList<>();
        int received = 0;
        for (int i = 0; i < MAX_PACKETS; i++) {
            byte[] payload = parsePacket(receiveDatagram(socket));
            if (payload.length < 3) {
                continue;
            }
            byte type = payload[1];
            if (type == TYPE_MESSAGE) {
                send(socket, ackPayload(payload[2])); // ACK 服务端消息，保持会话正常
                continue;
            }
            if (type == TYPE_LOGIN) {
                throw new BusinessException("RCON 会话已失效，请重试");
            }
            if (payload[2] != sequence) {
                continue;
            }
            if (payload.length == 3) {
                received++;
                break; // 空响应（如 keepalive 应答）
            }
            if (payload.length > 5 && payload[3] == 0) {
                // 分片：[3]=0x00 [4]=总片数 [5]=片序号 [6..]=内容
                expectedParts = payload[4] & 0xFF;
                int index = payload[5] & 0xFF;
                String chunk = new String(payload, 6, payload.length - 6, StandardCharsets.UTF_8);
                while (parts.size() <= index) {
                    parts.add("");
                }
                parts.set(index, chunk);
                received++;
                if (parts.size() == expectedParts) {
                    for (String p : parts) {
                        text.append(p);
                    }
                    break;
                }
            } else {
                text.append(new String(payload, 3, payload.length - 3, StandardCharsets.UTF_8));
                received++;
                break;
            }
        }
        String result = text.toString();
        if (received == 0) {
            throw new BusinessException("RCON 命令无响应");
        }
        String lower = result.toLowerCase(Locale.ROOT);
        if (lower.contains("not authenticated")) {
            throw new BusinessException("RCON 未认证（密码错误）");
        }
        return result;
    }

    private static byte[] receiveDatagram(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[8192];
        DatagramPacket datagram = new DatagramPacket(buf, buf.length);
        socket.receive(datagram);
        byte[] data = new byte[datagram.getLength()];
        System.arraycopy(datagram.getData(), datagram.getOffset(), data, 0, datagram.getLength());
        return data;
    }

    /** 校验 CRC 并返回 payload（0xFF 起始）。 */
    private static byte[] parsePacket(byte[] data) throws IOException {
        if (data.length < 7 || data[0] != 'B' || data[1] != 'E') {
            throw new IOException("非法 RCON 响应包");
        }
        int declared = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8) | ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 24);
        byte[] payload = new byte[data.length - 6];
        System.arraycopy(data, 6, payload, 0, payload.length);
        CRC32 crc = new CRC32();
        crc.update(payload);
        if (((int) crc.getValue()) != declared) {
            throw new IOException("RCON 响应包 CRC 校验失败");
        }
        return payload;
    }

    private void send(DatagramSocket socket, byte[] payload) throws IOException {
        byte[] packet = new byte[6 + payload.length];
        packet[0] = 'B';
        packet[1] = 'E';
        CRC32 crc = new CRC32();
        crc.update(payload);
        int value = (int) crc.getValue();
        packet[2] = (byte) (value & 0xFF);
        packet[3] = (byte) ((value >> 8) & 0xFF);
        packet[4] = (byte) ((value >> 16) & 0xFF);
        packet[5] = (byte) ((value >> 24) & 0xFF);
        System.arraycopy(payload, 0, packet, 6, payload.length);
        socket.send(new DatagramPacket(packet, packet.length));
    }

    private byte[] commandPayload(byte sequence, String command) {
        byte[] cmd = command.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[3 + cmd.length];
        payload[0] = (byte) 0xFF;
        payload[1] = TYPE_COMMAND;
        payload[2] = sequence;
        System.arraycopy(cmd, 0, payload, 3, cmd.length);
        return payload;
    }

    private byte[] ackPayload(byte sequence) {
        return new byte[]{(byte) 0xFF, TYPE_MESSAGE, sequence};
    }

    private static byte sequenceCounter = 0;

    private static synchronized byte nextSequence() {
        sequenceCounter = (byte) ((sequenceCounter + 1) & 0xFF);
        return sequenceCounter;
    }

    // ---------------------------------------------------------------- 命令封装

    /** 在线玩家列表。 */
    public List<PlayerInfo> players(GameInstance inst) {
        return parsePlayers(exec(inst, "players"));
    }

    /** 踢出玩家（playerNum 为 players 编号）。 */
    public String kick(GameInstance inst, int playerNum, String reason) {
        return exec(inst, "kick " + playerNum + optionalReason(reason));
    }

    /** 封禁玩家；minutes &gt; 0 为临时封禁（分钟），否则永久。 */
    public String ban(GameInstance inst, int playerNum, int minutes, String reason) {
        String cmd = "ban " + playerNum;
        if (minutes > 0) {
            cmd += " " + minutes;
        }
        return exec(inst, cmd + optionalReason(reason));
    }

    /** 按 GUID/IP/名称目标封禁（banPlayer）。 */
    public String banPlayer(GameInstance inst, String target, int minutes, String reason) {
        String cmd = "banPlayer " + sanitizeToken(target);
        if (minutes > 0) {
            cmd += " " + minutes;
        }
        return exec(inst, cmd + optionalReason(reason));
    }

    /** 解除单个封禁（removeBan）。 */
    public String removeBan(GameInstance inst, String banId) {
        return exec(inst, "removeBan " + sanitizeToken(banId));
    }

    /** 封禁列表（bans 命令，部分 BEServer 支持）。 */
    public String bans(GameInstance inst) {
        return exec(inst, "bans");
    }

    /** 全服公告：say -1 &lt;message&gt;。 */
    public String say(GameInstance inst, String message) {
        return exec(inst, "say -1 " + sanitizeMessage(message));
    }

    /** 私聊玩家：say &lt;num&gt; &lt;message&gt;。 */
    public String sayTo(GameInstance inst, int playerNum, String message) {
        return exec(inst, "say " + playerNum + " " + sanitizeMessage(message));
    }

    /** sayPlayer（协议文档命令，目标为玩家编号或名称）。 */
    public String sayPlayer(GameInstance inst, String target, String message) {
        return exec(inst, "sayPlayer " + sanitizeToken(target) + " " + sanitizeMessage(message));
    }

    public String lock(GameInstance inst) {
        return exec(inst, "lock");
    }

    public String unlock(GameInstance inst) {
        return exec(inst, "unlock");
    }

    public String maxPlayers(GameInstance inst, int max) {
        return exec(inst, "maxPlayers " + max);
    }

    public String reassign(GameInstance inst) {
        return exec(inst, "reassign");
    }

    public String restartServer(GameInstance inst) {
        return exec(inst, "restartServer");
    }

    public String restart(GameInstance inst) {
        return exec(inst, "restart");
    }

    /** 定时重启（restarttime 分 秒）。 */
    public String restartTime(GameInstance inst, int minutes, int seconds) {
        return exec(inst, "restarttime " + minutes + " " + seconds);
    }

    public String shutdown(GameInstance inst) {
        return exec(inst, "shutdown");
    }

    public String missions(GameInstance inst) {
        return exec(inst, "missions");
    }

    public String mission(GameInstance inst, String name) {
        return exec(inst, "mission " + sanitizeToken(name));
    }

    public String events(GameInstance inst) {
        return exec(inst, "events");
    }

    public String loadBans(GameInstance inst) {
        return exec(inst, "loadBans");
    }

    public String loadScripts(GameInstance inst) {
        return exec(inst, "loadScripts");
    }

    public String loadEvents(GameInstance inst) {
        return exec(inst, "loadEvents");
    }

    public String loadRPT(GameInstance inst) {
        return exec(inst, "loadRPT");
    }

    // ---------------------------------------------------------------- players 解析

    static List<PlayerInfo> parsePlayers(String text) {
        List<PlayerInfo> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String line : text.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || isHeader(trimmed)) {
                continue;
            }
            PlayerInfo info = parsePlayerLine(trimmed);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    private static boolean isHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("players on server") || lower.startsWith("rcon")
                || lower.startsWith("server #") || lower.equals("guid");
    }

    private static PlayerInfo parsePlayerLine(String line) {
        Matcher m = PLAYER_ARMA3.matcher(line);
        if (m.matches()) {
            return new PlayerInfo(
                    Integer.parseInt(m.group(1)),
                    m.group(6),
                    "-".equals(m.group(4)) ? null : m.group(4),
                    m.group(2),
                    m.group(3),
                    "OK".equals(m.group(5)),
                    "Lobby".equals(m.group(7)),
                    line);
        }
        Matcher g = PLAYER_GENERIC.matcher(line);
        if (!g.find()) {
            return null;
        }
        int num;
        try {
            num = Integer.parseInt(g.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        String name = null, guid = null, ip = null, ping = null;
        List<String> nameParts = new ArrayList<>();
        String rest = g.group(2).trim();
        String[] tokens = rest.contains(":") ? rest.split(":") : rest.split("\\s+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (guid == null && GUID.matcher(t).matches()) {
                guid = t;
            } else if (ip == null && IPV4.matcher(t).matches()) {
                ip = t;
            } else if (ping == null && t.matches("-?\\d{1,3}")) {
                ping = t;
            } else {
                nameParts.add(t);
            }
        }
        if (!nameParts.isEmpty()) {
            name = String.join(" ", nameParts);
        }
        return new PlayerInfo(num, name, guid, ip, ping, false, false, line);
    }

    // ---------------------------------------------------------------- 参数校验

    private String sanitizeCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new BusinessException("RCON 命令不能为空");
        }
        String cmd = command.replace("\r", " ").replace("\n", " ").trim();
        if (cmd.length() > 1000) {
            throw new BusinessException("RCON 命令过长");
        }
        return cmd;
    }

    private String optionalReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " " + sanitizeMessage(reason);
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        String m = message.replace("\r", " ").replace("\n", " ").trim();
        if (m.length() > 500) {
            throw new BusinessException("消息过长（最多 500 字符）");
        }
        return m;
    }

    private String sanitizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("参数不能为空");
        }
        String t = token.replace("\r", " ").replace("\n", " ").trim();
        if (t.contains(" ")) {
            throw new BusinessException("参数不能包含空格：" + t);
        }
        return t;
    }
}
