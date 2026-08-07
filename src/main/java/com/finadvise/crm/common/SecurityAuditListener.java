package com.finadvise.crm.common;

import com.finadvise.crm.users.PasswordChangeFailureEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class SecurityAuditListener {

    @EventListener
    public void onAuthorizationFailure(AuthorizationDeniedEvent<?> event) {
        // Spring Security 6 uses a Supplier for Authentication in authorization events
        Authentication auth = event.getAuthentication().get();
        String user = auth.getName();
        Object securedResource = event.getObject();

        log.warn("AUDIT | TYPE: AUTHORIZATION_DENIED | USER: [{}] | RESOURCE: [{}]", user, securedResource);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        // Authentication events provide direct access to the Authentication object
        Authentication auth = event.getAuthentication();
        String attemptedUser = auth.getName();
        String failureReason = event.getException().getMessage();

        log.warn("AUDIT | TYPE: AUTHENTICATION_FAILED | USER: [{}] | REASON: [{}]", attemptedUser, failureReason);
    }

    @EventListener
    public void onPasswordChangeFailure(PasswordChangeFailureEvent event) {
        log.warn("AUDIT | TYPE: PASSWORD_CHANGE_FAILED | USER: [{}] | REASON: [{}]", event.employeeId(), event.reason());
    }
}
