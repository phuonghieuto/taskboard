package com.phuonghieuto.backend.notification_service.service;

import java.util.Map;

public interface EmailService {
    void sendSimpleEmail(String to, String subject, String text);
    void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateVariables);
}
