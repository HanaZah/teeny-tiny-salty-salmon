package com.finadvise.crm.clients;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

class ClientSearchMinimalSpecification {
    private ClientSearchMinimalSpecification()  {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Specification<ClientSearchMinimal> build(ClientSearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String advisorEmployeeId = criteria.advisorEmployeeId() == null ? null : criteria.advisorEmployeeId().trim();
            String name = criteria.name() == null ? null : criteria.name().trim();
            String personalId = criteria.personalId() == null ? null : criteria.personalId().trim();
            String city = criteria.city() == null ? null : criteria.city().trim();

            if (advisorEmployeeId != null && !advisorEmployeeId.isBlank()) {
                predicates.add(cb.equal(root.get(ClientSearchMinimal_.advisorEmployeeId), advisorEmployeeId));
            }

            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get(ClientSearchMinimal_.fullName)), pattern));
            }

            if (personalId != null && !personalId.isBlank()) {
                predicates.add(cb.equal(root.get(ClientSearchMinimal_.personalId), personalId));
            }

            if (city != null && !city.isBlank()) {
                String pattern = "%" + city.toLowerCase() + "%";
                Expression<String> cityNameLower = cb.lower(root.get(ClientSearchMinimal_.contactCityName));
                predicates.add(
                        cb.or(
                                cb.like(cityNameLower, pattern),
                                cb.like(root.get(ClientSearchMinimal_.contactPostalCode), pattern)
                        )
                );
            }

            if (criteria.status() != null) {
                switch (criteria.status()) {
                    case ACTIVE -> predicates.add(cb.equal(root.get(ClientSearchMinimal_.isActive), true));
                    case INACTIVE -> predicates.add(cb.equal(root.get(ClientSearchMinimal_.isActive), false));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
