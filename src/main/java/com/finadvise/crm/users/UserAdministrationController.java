package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class UserAdministrationController {

    private final UserAdministrationService userService;
    private final UserDataModificationFacade userModificationFacade;

    @Operation(
            summary = "Get user details",
            description = "Admin-restricted endpoint for fetching user details by employee ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{employeeId}")
    public UserDetailDTO getUserDetail(@PathVariable String employeeId) {
        return userService.getUserDetail(employeeId);
    }

    @Operation(
            summary = "Get orphaned portfolios",
            description = "Admin-restricted endpoint for fetching orphaned portfolios."
    )
    @ApiResponse(responseCode = "200", description = "Orphaned portfolios retrieved successfully")
    @GetMapping("/orphaned-portfolios")
    public OrphanedPortfoliosDTO getOrphanedPortfolios() {
        return userService.getOrphanedPortfolios();
    }

    @GetMapping // Spring will automatically map request parameters to UserSearchCriteriaDTO
    public Page<UserSearchResultDTO> searchUsers(UserSearchCriteriaDTO criteria, Pageable pageable) {
        return userService.searchUsers(criteria, pageable);
    }

    @Operation(
            summary = "Register a new advisor",
            description = "Admin-restricted endpoint for advisor registration. " +
                    "Generates an employee ID and random password sent via email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "409", description = "Unique values conflict"),
            @ApiResponse(responseCode = "500", description = "Critical failure, admin record missing")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserDetailDTO> registerAdvisor(@Valid @RequestBody UserCreateDTO dto) {
        UserDetailDTO createdAdvisor = userModificationFacade.registerAdvisor(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/admin/users/{employeeId}")
                .buildAndExpand(createdAdvisor.employeeId())
                .toUri();

        return ResponseEntity.created(location).body(createdAdvisor);
    }

    @Operation(
            summary = "Update advisor status",
            description = "Admin-restricted endpoint for updating advisor status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format or status already set"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Critical failure, admin record missing")
    })
    @PatchMapping("/{employeeId}/status")
    public UserDetailDTO updateAdvisorStatus(
            @PathVariable String employeeId, @Valid @RequestBody UserStatusUpdateDTO dto) {
        return userModificationFacade.updateUserStatus(employeeId, dto);
    }

    @Operation(
            summary = "Admin-granted password reset",
            description = "Admin-restricted endpoint for resetting user password. " +
                    "Generates random password sent via email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User password reset successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Critical failure, admin record missing")
    })
    @PostMapping("/{employeeId}/reset-password")
    public UserDetailDTO resetPassword(@PathVariable String employeeId) {
        return userModificationFacade.resetPassword(employeeId);
    }

    @Operation(
            summary = "Get advisor autocomplete suggestions",
            description = "Admin-restricted endpoint for fetching active advisor name suggestions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect request parameters format"),
    })
    @GetMapping("/advisors-suggestions")
    public List<AdvisorSuggestionResultDTO> getAdvisorSuggestions(@Valid AdvisorSuggestionRequestDTO request) {
        return userService.getAdvisorSuggestions(request);
    }

    @Operation(
            summary = "Update user email",
            description = "Admin-restricted endpoint for updating user email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User email updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure)"),
            @ApiResponse(responseCode = "500", description = "Critical failure, admin record missing")
    })
    @PatchMapping("/{employeeId}/email")
    public UserDetailDTO updateUserEmail(@PathVariable String employeeId, @Valid @RequestBody UserEmailUpdateDTO dto) {
        return userModificationFacade.updateUserEmail(employeeId, dto);
    }
}
