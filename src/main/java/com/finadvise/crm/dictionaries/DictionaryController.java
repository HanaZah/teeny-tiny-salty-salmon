package com.finadvise.crm.dictionaries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DictionaryController {
    private final DictionaryService dictionaryService;

    @Operation(
            summary = "Get dynamic dictionary items",
            description = "Returns a list of dynamic dictionary items based on the specified type. " +
                    "Intended solely for drop-down populating.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dynamic dictionary items retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Unsupported dictionary type"),
    })
    @GetMapping("/dynamic/{type}")
    public List<DynamicDictionaryItemDTO> getDynamicDictionaryItems(@PathVariable DynamicDictionaryType type) {
        return dictionaryService.getDynamicDictionaryItems(type);
    }

    @Operation(
            summary = "Get static dictionary items",
            description = "Returns a list of static dictionary items based on the specified type. " +
                    "Intended solely for drop-down populating.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Static dictionary items retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Unsupported dictionary type"),
    })
    @GetMapping("/static/{type}")
    public List<StaticDictionaryItemDTO> getStaticDictionaryItems(@PathVariable StaticDictionaryType type) {
        return dictionaryService.getStaticDictionaryItems(type);
    }
}
