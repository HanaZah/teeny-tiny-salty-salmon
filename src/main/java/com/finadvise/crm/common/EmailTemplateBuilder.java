package com.finadvise.crm.common;

import com.finadvise.crm.users.UserContactDTO;

public class EmailTemplateBuilder {
    private EmailTemplateBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String buildNewUserSetupTemplate(
            String employeeId, String temporaryPassword, UserContactDTO adminContact) {
        return "Vítejte ve FinAdvise CRM! Vaše uživatelské jméno (employeeId) je: " + employeeId
                + ". Pro první přihlášení do systému použijte následující jednorázové heslo: " + temporaryPassword
                + "\n Pokud jste o vytvoření účtu nežádal/a nebo máte potíže s přihlášením, kontaktujte administrátora."
                + "\n Jméno: " + adminContact.firstName() + " " + adminContact.lastName()
                + "\n Telefon: " + adminContact.phone() + "\n Email: " + adminContact.email()
                + "\n\n Těšíme se na spoluprácí! \n S pozdravem, \n FinAdvise CRM";
    }

    public static String buildPasswordResetTemplate(
            String employeeId, String temporaryPassword, UserContactDTO adminContact) {
        return "Uživatel " + employeeId + " zažádal o reset zapomenutého hesla. "
                + "Pro příští přihlášení do systému použijte následující jednorázové heslo: " + temporaryPassword
                + "\n Pokud jste o reset nežádal/a, kontaktujte neprodleně administrátora a nahlaste incident."
                + "\n Jméno: " + adminContact.firstName() + " " + adminContact.lastName()
                + "\n Telefon: " + adminContact.phone() + "\n Email: " + adminContact.email()
                + "\n\n S pozdravem, \n FinAdvise CRM";
    }

    public static String buildStatusChangeTemplate(String employeeId, boolean isActive, UserContactDTO adminContact) {
        String statusMessage = (isActive?
                "byl aktivován. Nyní se můžete přihlásit do systému pod svým starým heslem."
                : "byl deaktivován. Nyní již nemáte přístup do systému."
                );

        return "Uživatelský účet " + employeeId + " " + statusMessage
                + "\n Pokud máte dotaz ke změně stavu účtu, kontaktujte prosím administrátora."
                + "\n Jméno: " + adminContact.firstName() + " " + adminContact.lastName()
                + "\n Telefon: " + adminContact.phone() + "\n Email: " + adminContact.email()
                + "\n\n S pozdravem, \n FinAdvise CRM";
    }

    public static String buildEmailChangeConfirmationTemplate(String employeeId, UserContactDTO adminContact) {
        return "Uživatelský účet " + employeeId + " byl úspěšně provázán s touto emailovou adresou."
                + "\n Pokud jste o změnu emailu nežádal/a, kontaktujte neprodleně administrátora a nahlaste incident."
                + "\n Jméno: " + adminContact.firstName() + " " + adminContact.lastName()
                + "\n Telefon: " + adminContact.phone() + "\n Email: " + adminContact.email()
                + "\n\n S pozdravem, \n FinAdvise CRM";
    }
}
