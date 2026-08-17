package com.example.modbus_api.logging;

import com.example.modbus_api.service.UserActionLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
public class LogActionAspect {

    private final UserActionLogService userActionLogService;

    public LogActionAspect(UserActionLogService userActionLogService) {
        this.userActionLogService = userActionLogService;
    }

    @Around("@annotation(logAction)")
    public Object logAction(ProceedingJoinPoint joinPoint, LogAction logAction) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            boolean success = true;
            String details = null;
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> response = (ResponseEntity<?>) result;
                success = response.getStatusCode().is2xxSuccessful();
                if (!success) {
                    details = extractError(response.getBody());
                }
            }
            userActionLogService.log(logAction.type(), logAction.value(), success, details);
            return result;
        } catch (Throwable t) {
            userActionLogService.log(logAction.type(), logAction.value(), false, t.getMessage());
            throw t;
        }
    }

    private String extractError(Object body) {
        if (body instanceof Map) {
            Object error = ((Map<?, ?>) body).get("error");
            if (error != null) {
                return String.valueOf(error);
            }
            Object message = ((Map<?, ?>) body).get("message");
            if (message != null) {
                return String.valueOf(message);
            }
        }
        if (body instanceof String) {
            return (String) body;
        }
        return null;
    }
}
