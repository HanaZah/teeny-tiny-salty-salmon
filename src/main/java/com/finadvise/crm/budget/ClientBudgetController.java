package com.finadvise.crm.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/clients/{clientUid}/budget")
@RequiredArgsConstructor
@PreAuthorize( "hasAuthority('ADVISOR')")
public class ClientBudgetController {
    private final BudgetService budgetService;

    @Operation(
            summary = "Update client budget",
            description = "Owning-advisor restricted endpoint for full budget update of a client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied"),
    })
    @PutMapping
    public FullBudgetDTO updateClientBudget(
            @PathVariable String clientUid,
            @Valid @RequestBody FullBudgetUpdateDTO dto,
            Principal principal) {

        return budgetService.updateFullBudgetForClient(clientUid, principal.getName(), dto);
    }
}
