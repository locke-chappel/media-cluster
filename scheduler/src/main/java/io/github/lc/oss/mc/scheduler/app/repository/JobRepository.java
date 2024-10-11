package io.github.lc.oss.mc.scheduler.app.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    Set<JobTypes> MUX_BLOCKING_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            JobTypes.Audio, //
            JobTypes.Video, //
            JobTypes.Merge)));
    Set<JobTypes> MERGE_BLOCKING_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            JobTypes.Video)));
    Set<Status> NONBLOCKING_STATUSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            Status.Complete, //
            Status.Finished //
    )));

    long countBySource(String source);

    long countBySourceAndStatusIn(String source, Status... statuses);

    boolean existsBySourceIgnoreCase(String source);

    default boolean areAnyJobsAvailable() {
        return this.existsByStatusIn(Status.Available);
    }

    boolean existsByStatusIn(Status... status);

    boolean existsBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
            String source, //
            Collection<JobTypes> types, //
            Collection<Status> statuses);

    void deleteBySourceIgnoreCase(String source);

    default List<Job> findMergeBlockingJobs(String source) {
        return this.findBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
                source, //
                JobRepository.MERGE_BLOCKING_TYPES, //
                JobRepository.NONBLOCKING_STATUSES);
    }

    default List<Job> findMuxBlockingJobs(String source) {
        return this.findBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
                source, //
                JobRepository.MUX_BLOCKING_TYPES, //
                JobRepository.NONBLOCKING_STATUSES);
    }

    List<Job> findBySourceIgnoreCase(String source);

    List<Job> findBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
            String source, //
            Collection<JobTypes> types, //
            Collection<Status> statuses);

    Job findBySourceIgnoreCaseAndTypeAndStatus(String source, JobTypes type, Status status);

    /*
     * Pass-through method to help readability
     */
    @Transactional(readOnly = true)
    default String findJobId(String source, JobTypes type, Status status) {
        Job job = this.findBySourceIgnoreCaseAndTypeAndStatus(source, type, status);
        if (job == null) {
            return null;
        }
        return job.getId();
    }

    default boolean hasMergeBlockingJobs(String source) {
        return this.existsBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
                source, //
                JobRepository.MERGE_BLOCKING_TYPES, //
                JobRepository.NONBLOCKING_STATUSES);
    }

    default boolean hasMuxBlockingJobs(String source) {
        return this.existsBySourceIgnoreCaseAndTypeInAndStatusNotIn( //
                source, //
                JobRepository.MUX_BLOCKING_TYPES, //
                JobRepository.NONBLOCKING_STATUSES);
    }
}
