package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client_;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.User_;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Specification<Product> build(ProductSearchCriteriaDTO criteria, LocalDate currentDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean hasAdvisor = criteria.advisorEmployeeId() != null && !criteria.advisorEmployeeId().isBlank();
            boolean hasClient = criteria.clientUid() != null && !criteria.clientUid().isBlank();

            if (hasAdvisor && hasClient) {
                String advisorId = criteria.advisorEmployeeId().trim();
                Predicate clientMatch = cb.equal(
                        root.get(Product_.client).get(Client_.clientUid),
                        criteria.clientUid().trim()
                );

                // Explicit LEFT JOIN prevents implicit INNER JOIN from dropping external products
                Join<Product, User> productAdvisorJoin = root.join(Product_.advisor, JoinType.LEFT);

                Predicate isProductAdvisor = cb.equal(
                        productAdvisorJoin.get(User_.employeeId),
                        advisorId
                );
                Predicate isClientAdvisor = cb.equal(
                        root.get(Product_.client).get(Client_.advisor).get(User_.employeeId),
                        advisorId
                );

                predicates.add(cb.and(clientMatch, cb.or(isProductAdvisor, isClientAdvisor)));

            } else if (hasAdvisor) {
                // General product search: strictly limited to products the advisor manages directly
                predicates.add(cb.equal(
                        root.get(Product_.advisor).get(User_.employeeId),
                        criteria.advisorEmployeeId().trim()
                ));

            } else if (hasClient) {
                // Admin search (no advisor filter): strictly match the client
                predicates.add(cb.equal(
                        root.get(Product_.client).get(Client_.clientUid),
                        criteria.clientUid().trim()
                ));
            }

            if (criteria.productName() != null && !criteria.productName().isBlank()) {
                String pattern = "%" + criteria.productName().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get(Product_.name)), pattern));
            }

            if (criteria.productTypeId() != null) {
                predicates.add(cb.equal(root.get(Product_.productType).get(ProductType_.id), criteria.productTypeId()));
            }

            if (criteria.productProviderId() != null) {
                predicates.add(cb.equal(root.get(Product_.provider).get(Provider_.id), criteria.productProviderId()));
            }

            if (criteria.productStatus() != null && criteria.productStatus() != ProductStatus.ALL) {
                switch (criteria.productStatus()) {
                    case ACTIVE -> {
                        Predicate startPastOrPresent = cb.lessThanOrEqualTo(root.get(Product_.startDate), currentDate);
                        Predicate endNull = cb.isNull(root.get(Product_.endDate));
                        Predicate endFutureOrPresent = cb.greaterThanOrEqualTo(root.get(Product_.endDate), currentDate);
                        predicates.add(cb.and(startPastOrPresent, cb.or(endNull, endFutureOrPresent)));
                    }
                    case EXPIRED -> {
                        Predicate startPast = cb.lessThan(root.get(Product_.startDate), currentDate);
                        Predicate endPast = cb.lessThan(root.get(Product_.endDate), currentDate);
                        predicates.add(cb.and(startPast, endPast));
                    }
                    case FUTURE -> {
                        Predicate startFuture = cb.greaterThan(root.get(Product_.startDate), currentDate);
                        predicates.add(startFuture);
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}