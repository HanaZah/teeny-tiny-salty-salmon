package com.finadvise.crm.common;

public interface EmailSender {
    void sendEmail(String sender, String recipient, String subject, String body);
}
