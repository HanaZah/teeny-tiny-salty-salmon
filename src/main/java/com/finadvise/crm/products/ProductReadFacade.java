package com.finadvise.crm.products;

import com.finadvise.crm.dictionaries.DynamicDictionaryItemDTO;
import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;

import java.util.List;

public interface ProductReadFacade {
    List<DynamicDictionaryItemDTO> getAllProductTypes();
    List<DynamicDictionaryItemDTO> getAllProductProviders();
    List<StaticDictionaryItemDTO> getAllProductStates();
    ProductsStatisticsDTO getProductsStatisticsForClient(String clientUid, String requesterEmployeeId);
}
