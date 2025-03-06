package com.phuonghieuto.backend.notification_service.exception.exception_handler;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RabbitMQErrorHandler {

    @Bean
    public MessageRecoverer messageRecoverer() {
        return new CustomErrorHandler();
    }
    
    private static class CustomErrorHandler implements MessageRecoverer {
        @Override
        public void recover(Message message, Throwable cause) {
            if (isRetryableError(cause)) {
                log.warn("Retrying message due to retryable error: {}", cause.getMessage());
                throw new ImmediateRequeueAmqpException("Retrying due to " + cause.getMessage(), cause);
            } else {
                log.error("Rejecting message due to non-retryable error: {}", cause.getMessage());
                throw new AmqpRejectAndDontRequeueException("Error processing message", cause);
            }
        }
        
        private boolean isRetryableError(Throwable cause) {
            // Define which exceptions should be retried vs. sent to dead letter
            return cause instanceof java.net.ConnectException 
                || cause instanceof java.net.SocketTimeoutException
                || cause instanceof org.springframework.dao.DataAccessResourceFailureException;
        }
    }
}
