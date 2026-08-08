package com.finadvise.crm.users;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.JwtGenerator;
import com.finadvise.crm.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private AuthenticationManager authManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Mock the configuration properties
        SecurityProperties securityProperties = new SecurityProperties(
                "crm-backend",
                "dummy-secret",
                "",
                0,
                ""
        );

        authService = new AuthService(securityProperties, jwtGenerator, authManager);
    }

    //----AUTHENTICATION AND TOKEN GENERATION TESTS----

    @Test
    void authenticateAndGenerateToken_HappyPath_ReturnsValidLoginResponse() {
        LoginRequestDTO request = new LoginRequestDTO("EMP-123", "password123");
        User user = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        String token = "mocked-jwt-token";
        when(jwtGenerator.generateToken(anyString(), anyString(), anyList(), anyInt())).thenReturn(token);

        AuthResponseDTO response = authService.authenticateAndGenerateToken(request);

        assertNotNull(response);
        assertEquals(token, response.accessToken());
        assertEquals("Bearer", response.tokenType());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateAndGenerateToken_BadCredentials_BubblesException() {
        LoginRequestDTO request = new LoginRequestDTO("EMP-123", "wrong-password");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () ->
                authService.authenticateAndGenerateToken(request)
        );

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtGenerator);
    }
}