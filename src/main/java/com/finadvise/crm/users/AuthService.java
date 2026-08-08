package com.finadvise.crm.users;

import com.finadvise.crm.common.JwtGenerator;
import com.finadvise.crm.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AuthService {

    private final SecurityProperties securityProperties;
    private final JwtGenerator jwtGenerator;
    private final AuthenticationManager authManager;

    AuthResponseDTO authenticateAndGenerateToken(LoginRequestDTO request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.employeeId(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        // can't be null by this point (authManager would have thrown already if the user was not found), but just in case
        return getAuthTokenForUser(Objects.requireNonNull(userDetails));
    }

    AuthResponseDTO getAuthTokenForUser(CustomUserDetails userDetails) {
        User user = userDetails.user();
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        String token = jwtGenerator.generateToken(
                user.getEmployeeId(), authorities, List.of(securityProperties.self()), 8*60
        );

        return new AuthResponseDTO(
                token,
                "Bearer"
        );
    }

}
