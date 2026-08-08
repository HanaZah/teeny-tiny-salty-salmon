package com.finadvise.crm.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DummyEmailSender implements EmailSender{
    @Override
    public void sendEmail(String sender, String recipient, String subject, String body) {
        log.info("--- MVP EMAIL STUB ---");
        log.info("From: {}, To: {}, Subject: {}, Body: {}", sender, recipient, subject, body);
        log.info("In production, this would send a secure sign-in link.");
    }
}
