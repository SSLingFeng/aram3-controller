package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.JobTask;
import com.aram3.domain.GameInstance;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.JobRepository;
import com.aram3.repository.UserRepository;
import com.aram3.infra.process.ProcessService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final ProcessService processService;

    public JobService(JobRepository jobRepository,
                      UserRepository userRepository,
                      AuditRepository auditRepository,
                      ProcessService processService) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.processService = processService;
    }

    public List<JobTask> recent() {
        return jobRepository.findRecent(100);
    }

    public JobTask get(long id) {
        return jobRepository.findById(id).orElseThrow(() -> new BusinessException("任务不存在"));
    }

    public long createStartJob(GameInstance inst, String username) {
        Long userId = username == null ? null :
                userRepository.findByUsername(username).map(u -> u.id()).orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", inst.id());
        payload.put("code", inst.code());
        payload.put("trigger", "manual");
        long jobId = jobRepository.create(new JobTask(
                null, "SERVER_START", "instance", inst.id(),
                toJson(payload), "PENDING", 0, null, null, userId, null, null, null
        ));
        auditRepository.log(userId, "server.start", "instance", inst.id(), String.valueOf(jobId), null);
        processService.startInstanceAsync(jobId, inst);
        return jobId;
    }

    public long createStopJob(GameInstance inst, String username) {
        Long userId = username == null ? null :
                userRepository.findByUsername(username).map(u -> u.id()).orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", inst.id());
        payload.put("code", inst.code());
        long jobId = jobRepository.create(new JobTask(
                null, "SERVER_STOP", "instance", inst.id(),
                toJson(payload), "PENDING", 0, null, null, userId, null, null, null
        ));
        auditRepository.log(userId, "server.stop", "instance", inst.id(), String.valueOf(jobId), null);
        processService.stopInstanceAsync(jobId, inst);
        return jobId;
    }

    public long createWorkshopJob(long modId, String workshopId, String username) {
        return createWorkshopJob(modId, workshopId, username, "DIRECT");
    }

    public long createWorkshopJob(long modId, String workshopId, String username, String mode) {
        Long userId = username == null ? null :
                userRepository.findByUsername(username).map(u -> u.id()).orElse(null);
        long jobId = jobRepository.create(new JobTask(
                null, "WORKSHOP_DOWNLOAD", "mod", modId,
                "{\"workshopId\":\"" + workshopId + "\",\"mode\":\"" + (mode == null ? "DIRECT" : mode) + "\"}", "PENDING", 0, null, null, userId, null, null, null
        ));
        processService.downloadWorkshopAsync(jobId, workshopId, mode, modId);
        return jobId;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            first = false;
        }
        return sb.append('}').toString();
    }

    public void markRunning(long jobId, String logPath) {
        jobRepository.markRunning(jobId, logPath);
    }

    public void markFinished(long jobId, String status, int progress, String error) {
        jobRepository.markFinished(jobId, status, progress, error);
    }

    public void updateProgress(long jobId, int progress) {
        jobRepository.updateProgress(jobId, progress);
    }
}
