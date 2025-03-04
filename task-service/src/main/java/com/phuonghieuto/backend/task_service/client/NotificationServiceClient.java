// task-service/src/main/java/com/phuonghieuto/backend/task_service/client/NotificationServiceClient.java
package com.phuonghieuto.backend.task_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.phuonghieuto.backend.task_service.config.FeignClientConfig;
import com.phuonghieuto.backend.task_service.model.task.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.request.NotificationRequestDTO;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@FeignClient(name = "notification-service", path = "/api/notifications", configuration = FeignClientConfig.class)
public interface NotificationServiceClient {
    @PostMapping
    void sendNotification(@RequestBody NotificationRequestDTO request);
    
    @PostMapping("/email")
    void sendEmailNotification(@RequestBody EmailNotificationRequestDTO request);
}