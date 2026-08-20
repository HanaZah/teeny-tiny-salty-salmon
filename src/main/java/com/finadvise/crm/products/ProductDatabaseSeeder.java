package com.finadvise.crm.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(20)
public class ProductDatabaseSeeder implements CommandLineRunner {
    private final ProductTypeRepository productTypeRepository;
    private final ProviderRepository providerRepository;

    @Override
    public void run(String @NonNull ... args) {
        if (productTypeRepository.count() == 0) {
            productTypeRepository.saveAll(List.of(
                    ProductType.builder().name("Životní pojištění").build(),
                    ProductType.builder().name("Penzijní spoření").build(),
                    ProductType.builder().name("Stavební spoření").build(),
                    ProductType.builder().name("Podílové fondy").build(),
                    ProductType.builder().name("Hypoteční úvěr").build(),
                    ProductType.builder().name("Spotřební úvěr").build(),
                    ProductType.builder().name("Neživotní pojištění").build(),
                    ProductType.builder().name("Investiční smlouva").build()
            ));
            log.info("Seeded initial Product Types.");
        }

        if (providerRepository.count() == 0) {
            providerRepository.saveAll(List.of(
                    Provider.builder().name("Generali Česká pojišťovna").build(),
                    Provider.builder().name("Kooperativa").build(),
                    Provider.builder().name("Conseq").build(),
                    Provider.builder().name("Česká spořitelna").build(),
                    Provider.builder().name("Amundi").build()
            ));
            log.info("Seeded initial Providers.");
        }
    }
}
