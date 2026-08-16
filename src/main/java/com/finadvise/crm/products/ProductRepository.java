package com.finadvise.crm.products;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndAdvisor_EmployeeId(Long id, String employeeId);

    @EntityGraph(attributePaths = {"provider", "productType", "client", "advisor"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"provider", "productType", "client", "advisor"})
    @NonNull Page<Product> findAll(@NonNull Specification<Product> spec, @NonNull Pageable pageable);

    @Query("""
        SELECT new com.finadvise.crm.products.ProductsStatisticsDTO(
            COUNT(p),
            COUNT(CASE WHEN p.startDate <= :today AND (p.endDate IS NULL OR p.endDate >= :today) THEN 1 END),
            COUNT(CASE WHEN p.startDate <= :today AND (p.endDate IS NULL OR p.endDate >= :today) AND a.employeeId = :requesterEmployeeId THEN 1 END),
            SUM(CASE WHEN p.startDate <= :today AND (p.endDate IS NULL OR p.endDate >= :today) THEN p.amount END)
        )
        FROM Product p
        LEFT JOIN p.advisor a
        WHERE p.client.clientUid = :clientUid
    """)
    ProductsStatisticsDTO getClientProductStatistics(
            @Param("clientUid") String clientUid,
            @Param("requesterEmployeeId") String requesterEmployeeId,
            @Param("today") LocalDate today
    );
}
