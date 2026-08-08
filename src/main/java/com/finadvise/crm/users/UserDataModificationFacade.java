package com.finadvise.crm.users;

import com.finadvise.crm.common.EmailSender;
import com.finadvise.crm.common.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserDataModificationFacade {

    private final UserAdministrationService userAdministrationService;
    private final UserService userService; // Using the facade interface you defined earlier
    private final EmailSender emailSender;

    public UserDetailDTO registerAdvisor(UserCreateDTO dto) {
        UserContactDTO adminContact = userService.getAdminContact();
        UserCredentialsInternalResult result = userAdministrationService.createAdvisor(dto);

        String emailBody = EmailTemplateBuilder.buildNewUserSetupTemplate(
                result.userDetail().employeeId(),
                result.rawPassword(),
                adminContact
        );

        emailSender.sendEmail(adminContact.email(), dto.email(), "Vítejte ve FinAdvise CRM", emailBody);

        return result.userDetail();
    }

    public UserDetailDTO updateUserStatus(String employeeId, UserStatusUpdateDTO dto) {
        UserContactDTO adminContact = userService.getAdminContact();
        UserDetailDTO result = userAdministrationService.updateUserStatus(employeeId, dto);

        String emailBody = EmailTemplateBuilder.buildStatusChangeTemplate(employeeId, result.isActive(), adminContact);
        emailSender.sendEmail(
                adminContact.email(), result.email(), "Změna stavu uživatelského účtu FinAdvise CRM", emailBody
        );

        return result;
    }

    public UserDetailDTO resetPassword(String employeeId) {
        UserContactDTO adminContact = userService.getAdminContact();
        UserCredentialsInternalResult result = userAdministrationService.resetUserPassword(employeeId);

        String emailBody = EmailTemplateBuilder.buildPasswordResetTemplate(
                employeeId,
                result.rawPassword(),
                adminContact
        );

        emailSender.sendEmail(
                adminContact.email(), result.userDetail().email(), "FinAdvise CRM - Reset hesla", emailBody);

        return result.userDetail();
    }

    public UserDetailDTO updateUserEmail(String employeeId, UserEmailUpdateDTO dto) {
        UserContactDTO adminContact = userService.getAdminContact();
        UserDetailDTO result = userAdministrationService.updateUserEmail(employeeId, dto);
        String emailBody = EmailTemplateBuilder.buildEmailChangeConfirmationTemplate(employeeId, adminContact);

        emailSender.sendEmail(
                adminContact.email(), result.email(), "FinAdvise CRM - Změna emailové adresy", emailBody);

        return result;
    }
}
