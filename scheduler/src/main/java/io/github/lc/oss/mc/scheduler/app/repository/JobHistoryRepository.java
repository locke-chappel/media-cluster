package io.github.lc.oss.mc.scheduler.app.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.lc.oss.mc.scheduler.app.entity.JobHistory;

@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, String> {
    List<JobHistory> findByCreatedLessThan(Date date);
}
