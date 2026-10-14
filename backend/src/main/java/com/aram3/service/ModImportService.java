package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.ModRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Arma 3 Launcher preset HTML (or similar) into mod entries:
 * DisplayName + Steam workshop id from sharedfiles/filedetails/?id=
 */
@Service
public class ModImportService {

    private static final Pattern ROW = Pattern.compile(
            "(?is)<tr[^>]*data-type=[\"']ModContainer[\"'][^>]*>(.*?)</tr>");
    private static final Pattern NAME = Pattern.compile(
            "(?is)<td[^>]*data-type=[\"']DisplayName[\"'][^>]*>(.*?)</td>");
    private static final Pattern WORKSHOP = Pattern.compile(
            "(?i)filedetails/\\?id=(\\d{5,12})");

    private final ModRepository modRepository;
    private final AuditRepository auditRepository;

    public ModImportService(ModRepository modRepository, AuditRepository auditRepository) {
        this.modRepository = modRepository;
        this.auditRepository = auditRepository;
    }

    public Map<String, Object> importHtml(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 HTML 文件");
        }
        String html;
        try {
            html = new String(file.getBytes(), StandardCharsets.UTF_8);
            // also try GBK if UTF-8 looks like replacement garbage for CJK names
            if (html.indexOf('\uFFFD') >= 0) {
                html = new String(file.getBytes(), "GBK");
            }
        } catch (Exception e) {
            throw new BusinessException("读取文件失败: " + e.getMessage());
        }

        List<Map<String, String>> parsed = parse(html);
        if (parsed.isEmpty()) {
            throw new BusinessException("未识别到模组，请确认是 Arma 3 Launcher 导出的 HTML 预设文件");
        }

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Map<String, String> item : parsed) {
            String workshopId = item.get("workshopId");
            String name = item.get("name");
            if (modRepository.findModByWorkshopId(workshopId).isPresent()) {
                skipped.add(name + " (" + workshopId + ")");
                continue;
            }
            long id = modRepository.createMod(new ModCatalog(
                    null, name, workshopId,
                    null, null, null, null, null, null,
                    "BOTH", "PENDING", "imported from HTML preset",
                    null, null, null
            ));
            created.add(name + " (" + workshopId + ")");
        }

        auditRepository.log(null, "mod.import_html", "mod", null,
                "created=" + created.size() + ",skipped=" + skipped.size(), null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", parsed.size());
        result.put("createdCount", created.size());
        result.put("skippedCount", skipped.size());
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("items", parsed);
        return result;
    }

    public List<Map<String, String>> parse(String html) {
        List<Map<String, String>> list = new ArrayList<>();
        Matcher rowMatcher = ROW.matcher(html);
        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            Matcher nameMatcher = NAME.matcher(row);
            Matcher idMatcher = WORKSHOP.matcher(row);
            String name = nameMatcher.find() ? stripTags(nameMatcher.group(1)).trim() : null;
            String workshopId = idMatcher.find() ? idMatcher.group(1) : null;
            if (workshopId == null) {
                continue;
            }
            if (name == null || name.isBlank()) {
                name = "Workshop " + workshopId;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("workshopId", workshopId);
            list.add(item);
        }
        return list;
    }

    private String stripTags(String html) {
        return html.replaceAll("(?s)<[^>]+>", "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").trim();
    }
}
