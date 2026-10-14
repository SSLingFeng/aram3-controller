package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.domain.JobTask;
import com.aram3.infra.process.ProcessService;
import com.aram3.service.JobService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class JobController {
    private final JobService jobService;
    private final ProcessService processService;

    public JobController(JobService jobService, ProcessService processService) {
        this.jobService = jobService;
        this.processService = processService;
    }

    @GetMapping("/jobs")
    public ApiResponse<List<JobTask>> jobs() {
        return ApiResponse.ok(jobService.recent());
    }

    @GetMapping("/jobs/{id}")
    public ApiResponse<JobTask> job(@PathVariable long id) {
        return ApiResponse.ok(jobService.get(id));
    }

    @GetMapping("/system/runtime")
    public ApiResponse<Map<String, Object>> runtime() {
        return ApiResponse.ok(Map.of(
                "powershell", processService.resolvePowerShell(),
                "powershellVersion", processService.powershellVersionLine()
        ));
    }
}
