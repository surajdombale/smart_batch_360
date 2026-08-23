package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Builds a dynamic query from whichever Batch Reports filters were actually supplied. */
final class BatchSpecifications {

    private BatchSpecifications() {
    }

    static Specification<Batch> matching(BatchSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(criteria.batchNumberFrom())) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("batchNumber"), criteria.batchNumberFrom().trim()));
            }
            if (hasText(criteria.batchNumberTo())) {
                predicates.add(cb.lessThanOrEqualTo(root.get("batchNumber"), criteria.batchNumberTo().trim()));
            }
            if (criteria.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("cycleDateTime"), startOfDay(criteria.dateFrom())));
            }
            if (criteria.dateTo() != null) {
                predicates.add(cb.lessThan(root.get("cycleDateTime"), startOfDay(criteria.dateTo().plusDays(1))));
            }
            if (criteria.clientId() != null) {
                predicates.add(cb.equal(root.get("client").get("id"), criteria.clientId()));
            }
            if (criteria.siteId() != null) {
                predicates.add(cb.equal(root.get("site").get("id"), criteria.siteId()));
            }
            if (criteria.vehicleId() != null) {
                predicates.add(cb.equal(root.get("vehicle").get("id"), criteria.vehicleId()));
            }
            if (criteria.driverId() != null) {
                predicates.add(cb.equal(root.get("driver").get("id"), criteria.driverId()));
            }
            if (criteria.recipeId() != null) {
                predicates.add(cb.equal(root.get("recipe").get("id"), criteria.recipeId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
