package com.finadvise.crm.users;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    // Private constructor to prevent instantiation of utility class
    private UserSpecification() {throw new UnsupportedOperationException("Utility class cannot be instantiated");}

    public static Specification<User> build(UserSearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.name() != null && !criteria.name().isBlank()) {
                String pattern = "%" + criteria.name().trim().toLowerCase() + "%";
                Expression<String> fullName = cb.concat(
                        cb.concat(cb.lower(root.get(User_.firstName)), " "),
                        cb.lower(root.get(User_.lastName))
                );
                // Searches for the string in concatenation of first and last names
                predicates.add(cb.like(fullName, pattern));
            }

            if (criteria.ico() != null && !criteria.ico().isBlank()) {
                predicates.add(cb.equal(root.get(User_.ico), criteria.ico()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get(User_.isActive), criteria.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
