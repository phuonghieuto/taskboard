package com.phuonghieuto.backend.notification_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@FeignClient(name = "task-service", path = "/api/v1/tasks")
public interface TaskServiceClient {
    @GetMapping("/upcoming")
    List<Map<String, Object>> getUpcomingTasks();
    
    @GetMapping("/due-between/{start}/{end}")
    List<Map<String, Object>> getTasksDueBetween(@PathVariable LocalDateTime start, @PathVariable LocalDateTime end);
}