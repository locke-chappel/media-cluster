package io.github.lc.oss.mc.scheduler.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.jpa.AbstractQueryBuilder;
import io.github.lc.oss.commons.jpa.QueryInfo;
import io.github.lc.oss.commons.jpa.SearchCriteria;
import io.github.lc.oss.commons.jpa.SearchTerm;
import io.github.lc.oss.commons.jpa.SortDirection;
import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.PagedResult;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.security.Authorities;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
@PreAuthorize(Authorities.USER)
public class JobSearchService extends AbstractSearchService {
    public enum JobSearchTerms implements SearchTerm {
        Source("source", true, true), Type("type", true, true), Status("status", true, true);

        private final String property;
        private final boolean queryable;
        private final boolean sortable;

        private JobSearchTerms(String property, boolean queryable, boolean sortable) {
            this.property = property;
            this.queryable = queryable;
            this.sortable = sortable;
        }

        @Override
        public String getProperty() {
            return this.property;
        }

        @Override
        public boolean isQueryable() {
            return this.queryable;
        }

        @Override
        public boolean isSortable() {
            return this.sortable;
        }
    }

    private AbstractQueryBuilder<io.github.lc.oss.mc.api.Job> QUERY_BUILDER = new AbstractQueryBuilder<>() {
        @Override
        protected <T> QueryInfo build(SearchCriteria criteria, CriteriaBuilder cb, boolean forCount) {
            CriteriaQuery<?> query;
            if (forCount) {
                query = cb.createQuery(Long.class);
            } else {
                query = cb.createQuery(io.github.lc.oss.mc.api.Job.class);
            }

            Root<Job> job = query.from(Job.class);

            List<Predicate> wheres = new ArrayList<>();
            List<Order> orders = new ArrayList<>();

            for (Term term : criteria.getSearchTerms()) {
                Path<String> path = job.get(term.getProperty());
                if (term.getValue() != null) {
                    wheres.add(JobSearchService.this.iLikeWild(path, (String) term.getValue()));
                }

                if (term.getSort() != null) {
                    orders.add(JobSearchService.this.iOrderBy(path, term.getSort()));
                }
            }

            /* tie breaker */
            orders.add(JobSearchService.this.orderBy(job.get("id"), SortDirection.Asc));

            if (wheres.size() > 0) {
                query.where(cb.and(cb.equal(job.get("type"), JobTypes.Scan),
                        cb.or(wheres.toArray(new Predicate[wheres.size()]))));
            } else {
                query.where(cb.equal(job.get("type"), JobTypes.Scan));
            }

            query.orderBy(orders);

            QueryInfo info = new QueryInfo();
            info.setQuery(query);
            if (forCount) {
                info.setSelection(cb.count(job));
            } else {
                info.setSelection(cb.construct( //
                        io.github.lc.oss.mc.api.Job.class, //
                        job.get("id"), //
                        job.get("type"), //
                        job.get("status"), //
                        job.get("source")));
            }
            return info;
        }
    };

    @Transactional(readOnly = true)
    public PagedResult<io.github.lc.oss.mc.api.Job> search(SearchCriteria criteria) {
        return this.getPage(this.QUERY_BUILDER, criteria);
    }
}
