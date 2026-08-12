package com.finadvise.crm.products;

import com.finadvise.crm.clients.ClientSummaryDTO;
import com.finadvise.crm.dictionaries.DynamicDictionaryItemDTO;
import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;
import com.finadvise.crm.users.AdvisorSummaryDTO;
import org.springframework.stereotype.Component;

@Component
class ProductMapper {
    ProductDTO toDto(Product product, AdvisorSummaryDTO advisor, ClientSummaryDTO client) {
        // return null for malformed data, only advisor is optional
        if (product == null
                || product.getProductType() == null
                || product.getProvider() == null
                || client == null) {
            return null;
        }

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getAmount(),
                product.getStartDate(),
                product.getEndDate(),
                new ProductTypeDTO(product.getProductType().getId(), product.getProductType().getName()),
                new ProductProviderDTO(product.getProvider().getId(), product.getProvider().getName()),
                advisor,
                client
        );
    }

    DynamicDictionaryItemDTO toDynamicDictionaryItemDto(ProductType type) {
        if (type == null) {
            return null;
        }
        return new DynamicDictionaryItemDTO(type.getId(), type.getName());
    }

    DynamicDictionaryItemDTO toDynamicDictionaryItemDto(Provider provider) {
        if (provider == null) {
            return null;
        }
        return new DynamicDictionaryItemDTO(provider.getId(), provider.getName());
    }

    StaticDictionaryItemDTO toStaticDictionaryItemDto(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return new StaticDictionaryItemDTO(status.name(), status.getLabel());
    }
}
