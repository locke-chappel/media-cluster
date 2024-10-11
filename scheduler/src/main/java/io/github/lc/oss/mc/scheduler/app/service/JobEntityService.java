package io.github.lc.oss.mc.scheduler.app.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class JobEntityService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Job findNextJobForCluster(String clusterName, Collection<JobTypes> jobTypes) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Job> query = cb.createQuery(Job.class);
        Root<Job> job = query.from(Job.class);

        List<Predicate> wheres = new ArrayList<>();
        List<Order> orders = new ArrayList<>();

        wheres.add(cb.equal(job.get("clusterName"), clusterName));
        wheres.add(cb.isNotNull(job.get("index")));
        wheres.add(cb.equal(job.get("status"), Status.Available));
        if (!jobTypes.isEmpty()) {
            wheres.add(job.get("type").in(jobTypes));
        }

        orders.add(cb.asc(job.get("index")));

        query.where(cb.and(wheres.toArray(new Predicate[wheres.size()])));
        query.orderBy(orders);

        TypedQuery<Job> typedQuery = this.entityManager.createQuery(query);
        typedQuery.setMaxResults(1);

        List<Job> jobs = typedQuery.getResultList();
        if (jobs.isEmpty()) {
            return null;
        }
        return jobs.iterator().next();
    }

    @Transactional(readOnly = true)
    public int findNextJobIndexForCluster(String clusterName) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> query = cb.createQuery(Integer.class);
        Root<Job> job = query.from(Job.class);

        List<Predicate> wheres = new ArrayList<>();
        List<Order> orders = new ArrayList<>();

        wheres.add(cb.equal(job.get("clusterName"), clusterName));
        wheres.add(cb.isNotNull(job.get("index")));
        orders.add(cb.asc(job.get("index")));

        query.where(cb.and(wheres.toArray(new Predicate[wheres.size()])));
        query.orderBy(orders);
        query.select(job.get("index"));

        TypedQuery<Integer> typedQuery = this.entityManager.createQuery(query);
        typedQuery.setMaxResults(1);

        List<Integer> jobs = typedQuery.getResultList();
        if (jobs.isEmpty()) {
            return 0;
        }
        return jobs.iterator().next();
    }
}
