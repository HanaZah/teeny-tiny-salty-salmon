package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Endpoints for identity verification and JWT issuance")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "User Login", description = "Authenticates credentials and returns a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "401", description = "Invalid employee ID or password")
    })
    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.authenticateAndGenerateToken(request);
    }

    @Operation(summary = "Reset Password", description = "Returns the contact details for password reset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved contact details"),
            @ApiResponse(responseCode = "500", description = "Critical failure, no admin found.")
    })
    @GetMapping("/forgotten-password")
    public UserContactDTO resetPassword() {
        return userService.getAdminContact();
    }
}
