package com.aram3.repository;

// TimeMapper is same package

import com.aram3.domain.JobTask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JobRepository {
    private final JdbcTemplate jdbc;

    public JobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<JobTask> MAPPER = (rs, i) -> new JobTask(
            rs.getLong("id"),
            rs.getString("job_type"),
            rs.getString("target_type"),
            DbTypes.getLong(rs, "target_id"),
            rs.getString("payload_json"),
            rs.getString("status"),
            rs.getInt("progress"),
            rs.getString("log_path"),
            rs.getString("error_message"),
            DbTypes.getLong(rs, "created_by"),
            TimeMapper.getInstant(rs, "created_at"),
            TimeMapper.getInstant(rs, "started_at"),
            TimeMapper.getInstant(rs, "finished_at")
    );


    public long create(JobTask job) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO job_task(job_type, target_type, target_id, payload_json, status, progress,
                      log_path, error_message, created_by, created_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, job.jobType());
            ps.setString(2, job.targetType());
            ps.setObject(3, job.targetId());
            ps.setString(4, job.payloadJson());
            ps.setString(5, job.status() == null ? "PENDING" : job.status());
            ps.setInt(6, job.progress() <= 0 ? 0 : job.progress());
            ps.setString(7, job.logPath());
            ps.setString(8, job.errorMessage());
            ps.setObject(9, job.createdBy());
            ps.setString(10, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Optional<JobTask> findById(long id) {
        return jdbc.query("SELECT * FROM job_task WHERE id=?", MAPPER, id).stream().findFirst();
    }

    public List<JobTask> findRecent(int limit) {
        return jdbc.query("SELECT * FROM job_task ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }

    public void markRunning(long id, String logPath) {
        jdbc.update("UPDATE job_task SET status='RUNNING', progress=1, started_at=?, log_path=? WHERE id=?",
                Instant.now().toString(), logPath, id);
    }

    public void updateProgress(long id, int progress) {
        jdbc.update("UPDATE job_task SET progress=? WHERE id=?", Math.max(0, Math.min(100, progress)), id);
    }

    public void markFinished(long id, String status, int progress, String error) {
        jdbc.update("UPDATE job_task SET status=?, progress=?, error_message=?, finished_at=? WHERE id=?",
                status, progress, error, Instant.now().toString(), id);
    }
}


