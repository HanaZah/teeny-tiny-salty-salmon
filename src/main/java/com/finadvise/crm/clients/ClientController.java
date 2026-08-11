package com.finadvise.crm.clients;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Validated
public class ClientController {
    private final ClientService clientService;
    private final ClientDetailOrchestrator clientDetailOrchestrator;

    @Operation(
            summary = "Get client detail",
            description = "Returns the details of a client. Restricted to admin and owning advisor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied"),
    })
    @PreAuthorize( "hasAuthority('ADVISOR') or hasAuthority('ADMIN')")
    @GetMapping("/{clientUid}")
    public ClientDetailDTO getClientDetail(@PathVariable String clientUid, Authentication authentication) {
        return clientDetailOrchestrator.getClientDetail(clientUid, authentication.getName(), isAdmin(authentication));
    }

    @Operation(
            summary = "Get recent client overviews",
            description = "Returns a list of most recently updated active client overviews of the authenticated advisor. " +
                    "Restricted to advisors.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent client overviews retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect request parameters format"),
    })
    @PreAuthorize( "hasAuthority('ADVISOR')")
    @GetMapping("/recent")
    public List<ClientOverviewDTO> getRecentClientOverviews(
            @RequestParam @Min(1) @Max(20) Integer limit,
            Principal principal) {
        return clientService.getRecentClientOverviews(principal.getName(), limit);
    }

    @Operation(summary = "Get client autocomplete suggestions",
            description = "Returns client name suggestions. " +
                    "Suggests any client for admin but only owned client for advisor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect request parameters format"),
    })
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN')")
    @GetMapping("/suggestions")
    public List<ClientSuggestionResultDTO> getClientSuggestions(
            @Valid ClientSuggestionRequestDTO request,
            Authentication authentication) {

        return clientService.getClientSuggestions(request, authentication.getName(), isAdmin(authentication));
    }

    @Operation(summary = "Search clients",
            description = "Returns a page of clients matching the search criteria. " +
                    "Searches all clients for admin but only owned clients for advisor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clients retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect request parameters format"),
    })
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN')")
    @PostMapping("/search")
    public Page<ClientSearchResultDTO> searchClients(
            @Valid @RequestBody ClientSearchCriteriaDTO criteria,
            Pageable pageable,
            Authentication authentication) {

        return clientService.searchClients(criteria, pageable, authentication.getName(), isAdmin(authentication));
    }

    @Operation(
            summary = "Create a new client",
            description = "Advisor restricted endpoint for new client creation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure) or unique value conflict"),
    })
    @PreAuthorize("hasAuthority('ADVISOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ClientDetailDTO> createClient(@Valid @RequestBody ClientCreateDTO dto, Principal principal) {
        ClientDetailDTO client = clientDetailOrchestrator.createClient(dto, principal.getName());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/clients/{clientUid}")
                .buildAndExpand(client.clientUid())
                .toUri();

        return ResponseEntity.created(location).body(client);
    }

    @Operation(
            summary = "Update client's general info",
            description = "Owning-advisor restricted endpoint for updating general info in client details"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure) or unique value conflict")
    })
    @PreAuthorize("hasAuthority('ADVISOR')")
    @PutMapping("/{clientUid}/general-info")
    public ClientDetailDTO updateClientGeneralInfo(
            @Valid @RequestBody ClientGeneralUpdateDTO dto,
            @PathVariable String clientUid,
            Principal principal) {

        return clientDetailOrchestrator.updateClientGeneralInfo(dto, clientUid, principal.getName());
    }

    @Operation(
            summary = "Update client's ID card",
            description = "Owning-advisor restricted endpoint for updating ID card in client details"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure) or unique value conflict")
    })
    @PreAuthorize("hasAuthority('ADVISOR')")
    @PutMapping("/{clientUid}/id-card")
    public ClientDetailDTO updateClientIdCard(
            @Valid @RequestBody ClientIdCardUpdateDTO dto,
            @PathVariable String clientUid,
            Principal principal) {

        return clientDetailOrchestrator.updateClientIdCard(dto, clientUid, principal.getName());
    }

    @Operation(
            summary = "Update client status",
            description = "Admin-restricted endpoint for updating client status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format or status already set"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{clientUid}/status")
    public ClientDetailDTO updateClientStatus(
            @PathVariable String clientUid,
            @Valid @RequestBody ClientStatusUpdateDTO dto,
            Principal principal) {

        return clientDetailOrchestrator.updateClientStatus(dto, clientUid, principal.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ADMIN"::equals);
    }
}
