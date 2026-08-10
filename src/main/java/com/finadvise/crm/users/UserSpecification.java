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
            String name = criteria.name() == null ? null : criteria.name().trim();
            String ico = criteria.ico() == null ? null : criteria.ico().trim();

            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.toLowerCase() + "%";
                Expression<String> fullName = cb.concat(
                        cb.concat(cb.lower(root.get(User_.firstName)), " "),
                        cb.lower(root.get(User_.lastName))
                );
                // Searches for the string in concatenation of first and last names
                predicates.add(cb.like(fullName, pattern));
            }

            if (ico != null && !ico.isBlank()) {
                predicates.add(cb.equal(root.get(User_.ico), ico));
            }

            if (criteria.status() != null) {
                switch (criteria.status()) {
                    case ACTIVE -> predicates.add(cb.equal(root.get(User_.isActive), true));
                    case INACTIVE -> predicates.add(cb.equal(root.get(User_.isActive), false));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
