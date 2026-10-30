package com.aram3.infra.rcon;

import com.aram3.common.BusinessException;
import com.aram3.domain.GameInstance;
import com.aram3.service.SettingService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * BattlEye 本地文件管理：bans.txt 列表/解封（协议文档 2.4 节）与 RPT 日志尾部读取。
 * 读写按字节保留原文件内容（仅整行删除），避免编码破坏。
 */
@Service
public class BanFileService {
    private static final Pattern GUID = Pattern.compile("^[0-9a-fA-F]{32}$|^\\d{17}$");
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final SettingService settingService;

    public BanFileService(SettingService settingService) {
        this.settingService = settingService;
    }

    /** 定位 bans.txt：依次检查实例目录与游戏服务端目录下的 battleye/。 */
    public Path findBansFile(GameInstance inst) {
        List<Path> candidates = new ArrayList<>();
        if (inst.rootPath() != null && !inst.rootPath().isBlank()) {
            Path root = Path.of(inst.rootPath());
            candidates.add(root.resolve("battleye").resolve("bans.txt"));
            candidates.add(root.resolve("BattlEye").resolve("bans.txt"));
        }
        Path gameDir = Path.of(settingService.gameServerDir());
        candidates.add(gameDir.resolve("battleye").resolve("bans.txt"));
        candidates.add(gameDir.resolve("BattlEye").resolve("bans.txt"));
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    /** 解析 bans.txt 全部行。lineNo 为 0 起始，供解封定位。 */
    public List<BanEntry> listBans(GameInstance inst) {
        Path file = findBansFile(inst);
        if (file == null) {
            return List.of();
        }
        List<byte[]> lines = readLines(file);
        List<BanEntry> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String text = decode(lines.get(i)).trim();
            if (text.isEmpty()) {
                continue;
            }
            out.add(parseBanLine(i, text));
        }
        return out;
    }

    /** 删除一行封禁（按 listBans 返回的 lineNo），返回删除后的文件路径。 */
    public Path removeBan(GameInstance inst, int lineNo) {
        Path file = findBansFile(inst);
        if (file == null) {
            throw new BusinessException("未找到 bans.txt，无法解封");
        }
        List<byte[]> lines = readLines(file);
        if (lineNo < 0 || lineNo >= lines.size()) {
            throw new BusinessException("封禁条目不存在（可能已被删除）");
        }
        lines.remove(lineNo);
        try {
            byte[] out = new byte[lines.stream().mapToInt(b -> b.length).sum()];
            int off = 0;
            for (byte[] line : lines) {
                System.arraycopy(line, 0, out, off, line.length);
                off += line.length;
            }
            Files.write(file, out);
        } catch (IOException e) {
            throw new BusinessException("写入 bans.txt 失败：" + e.getMessage());
        }
        return file;
    }

    /** 读取最新 RPT 日志尾部。 */
    public RptTail rptTail(GameInstance inst, int maxLines) {
        Path rpt = findLatestRpt(inst);
        if (rpt == null) {
            throw new BusinessException("未找到 RPT 日志文件（已查找实例目录与服务端目录）");
        }
        int limit = Math.max(1, Math.min(maxLines, 1000));
        List<byte[]> lines = readLines(rpt);
        List<String> tail = new ArrayList<>();
        for (int i = Math.max(0, lines.size() - limit); i < lines.size(); i++) {
            tail.add(decode(lines.get(i)));
        }
        return new RptTail(rpt.toString(), lines.size(), String.join("", tail));
    }

    private Path findLatestRpt(GameInstance inst) {
        List<Path> roots = new ArrayList<>();
        if (inst.rootPath() != null && !inst.rootPath().isBlank()) {
            roots.add(Path.of(inst.rootPath()));
        }
        roots.add(Path.of(settingService.gameServerDir()));
        Path best = null;
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root, 3)) {
                Path latest = walk
                        .filter(p -> Files.isRegularFile(p))
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpt"))
                        .max(Comparator.comparingLong(BanFileService::lastModified))
                        .orElse(null);
                if (latest != null && (best == null || lastModified(latest) > lastModified(best))) {
                    best = latest;
                }
            } catch (IOException e) {
                // 单个目录不可读时跳过
            }
        }
        return best == null ? null : best.toAbsolutePath().normalize();
    }

    private static long lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * bans.txt 行解析（宽松）：[序号] 目标（GUID/IP）[perm|时长] [:] 理由。
     * 形如 {@code 0 <guid> 0 : reason}、{@code <guid> perm : reason}、{@code <ip> 60 : reason}。
     */
    static BanEntry parseBanLine(int lineNo, String text) {
        String[] tokens = text.split("\\s+");
        String target = null;
        String kind = "OTHER";
        int targetIdx = -1;
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.isEmpty() || t.equals(":")) {
                continue;
            }
            if (target == null && GUID.matcher(t).matches()) {
                target = t;
                kind = "GUID";
                targetIdx = i;
            } else if (target == null && IPV4.matcher(t).matches()) {
                target = t;
                kind = "IP";
                targetIdx = i;
            }
        }
        List<String> rest = new ArrayList<>();
        for (int i = targetIdx + 1; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.isEmpty() || t.equals(":")) {
                continue;
            }
            // 目标后的第一个字段是 perm / 时长数字，不算理由
            if (rest.isEmpty() && (t.equalsIgnoreCase("perm") || t.matches("\\d+"))) {
                continue;
            }
            rest.add(t);
        }
        String reason = String.join(" ", rest).replaceFirst("^:\\s*", "").trim();
        return new BanEntry(lineNo, text, target, kind, reason);
    }

    /** 按字节读行，保留行尾（\r\n 或 \n），保证写回时不改动其余内容。 */
    private static List<byte[]> readLines(Path file) {
        byte[] data;
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + file + " — " + e.getMessage());
        }
        List<byte[]> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                byte[] line = new byte[i + 1 - start];
                System.arraycopy(data, start, line, 0, line.length);
                lines.add(line);
                start = i + 1;
            }
        }
        if (start < data.length) {
            byte[] line = new byte[data.length - start];
            System.arraycopy(data, start, line, 0, line.length);
            lines.add(line);
        }
        return lines;
    }

    /** 展示用解码：优先 UTF-8，失败回退 GBK，再回退 latin1（字节保留）。 */
    static String decode(byte[] bytes) {
        try {
            CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return utf8.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            try {
                return new String(bytes, java.nio.charset.Charset.forName("GBK"));
            } catch (Exception e2) {
                return new String(bytes, StandardCharsets.ISO_8859_1);
            }
        }
    }

    /** RPT 尾部结果。 */
    public record RptTail(String path, int totalLines, String content) {
    }
}
