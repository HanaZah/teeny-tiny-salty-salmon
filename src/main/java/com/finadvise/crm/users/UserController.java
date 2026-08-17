package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Fetches the profile of the currently authenticated user based on their JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Critical failure, authenticated user record missing")
    })
    @GetMapping("/me")
    public UserProfileDTO getCurrentUser(Principal principal) {
        return userService.getUserProfile(principal.getName());
    }

    @Operation(
            summary = "Update current user profile",
            description = "Updates the profile details of the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure) or unique value conflict"),
            @ApiResponse(responseCode = "500", description = "Critical failure, authenticated user record missing")
    })
    @PutMapping("/me/profile")
    public UserProfileDTO updateProfile(
            Principal principal,
            @Valid @RequestBody UserUpdateDTO dto) {

        return userService.updateUserProfile(principal.getName(), dto);
    }

    @Operation(
            summary = "Change current user password",
            description = "Changes the password of the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "409", description = "Version mismatch (Optimistic locking failure) or unique value conflict"),
            @ApiResponse(responseCode = "500", description = "Critical failure, authenticated user record missing")
    })
    @PutMapping("/me/password")
    public UserProfileDTO changePassword(@Valid @RequestBody PasswordChangeDTO dto, Principal principal) {
        return userService.updateUserPassword(dto, principal.getName());
    }
}
