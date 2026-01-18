package com.area.server.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Aspect that logs all controller responses sent to the frontend.
 * This provides visibility into what the API returns to clients.
 *
 * Logs are written to both the main log and a dedicated api-responses.log file.
 */
@Aspect
@Component
public class ApiResponseLogger {

    private static final Logger logger = LoggerFactory.getLogger(ApiResponseLogger.class);
    private static final int MAX_BODY_LENGTH = 5000;

    private final ObjectMapper objectMapper;

    public ApiResponseLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Pointcut that matches all public methods in the controller package.
     */
    @Pointcut("execution(public * com.area.server.controller..*(..))")
    public void controllerMethods() {}

    /**
     * Pointcut that matches all methods annotated with @RequestMapping or its variants.
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public void requestMappingMethods() {}

    /**
     * Around advice that logs the request and response for all controller endpoints.
     */
    @Around("controllerMethods() && requestMappingMethods()")
    public Object logApiResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String traceId = generateTraceId();
        MDC.put("traceId", traceId);

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryString = request != null ? request.getQueryString() : null;
        String fullPath = queryString != null ? uri + "?" + queryString : uri;

        // Log incoming request
        logIncomingRequest(traceId, method, fullPath, joinPoint);

        Object result;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log successful response
            logSuccessResponse(traceId, method, fullPath, result, duration);

            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;

            // Log error response
            logErrorResponse(traceId, method, fullPath, ex, duration);

            throw ex;
        } finally {
            MDC.remove("traceId");
        }
    }

    private void logIncomingRequest(String traceId, String method, String path,
                                    ProceedingJoinPoint joinPoint) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("┌──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ INCOMING REQUEST - %s\n", traceId));
        logMessage.append("├──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ %s %s\n", method, path));
        logMessage.append(String.format("│ Handler: %s.%s\n",
            joinPoint.getSignature().getDeclaringType().getSimpleName(),
            joinPoint.getSignature().getName()));

        // Log method arguments (if any)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            logMessage.append("│ Arguments:\n");
            for (int i = 0; i < args.length; i++) {
                String argValue = formatArgument(args[i]);
                logMessage.append(String.format("│   [%d]: %s\n", i, argValue));
            }
        }
        logMessage.append("└──────────────────────────────────────────────────────────────────────────────");

        logger.debug(logMessage.toString());
    }

    private void logSuccessResponse(String traceId, String method, String path,
                                    Object result, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("┌──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ OUTGOING RESPONSE - %s\n", traceId));
        logMessage.append("├──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ %s %s\n", method, path));
        logMessage.append(String.format("│ Duration: %d ms\n", duration));

        // Extract status code and body from result
        if (result instanceof ResponseEntity<?> responseEntity) {
            logMessage.append(String.format("│ Status: %d\n", responseEntity.getStatusCode().value()));

            Object body = responseEntity.getBody();
            if (body != null) {
                String bodyStr = formatResponseBody(body);
                logMessage.append(String.format("│ Body: %s\n", bodyStr));
            }
        } else if (result != null) {
            logMessage.append(String.format("│ Status: 200 (implicit)\n"));
            String bodyStr = formatResponseBody(result);
            logMessage.append(String.format("│ Body: %s\n", bodyStr));
        } else {
            logMessage.append("│ Status: 200 (no content)\n");
        }

        logMessage.append("└──────────────────────────────────────────────────────────────────────────────");

        logger.debug(logMessage.toString());
    }

    private void logErrorResponse(String traceId, String method, String path,
                                  Throwable error, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("┌──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ ERROR RESPONSE - %s\n", traceId));
        logMessage.append("├──────────────────────────────────────────────────────────────────────────────\n");
        logMessage.append(String.format("│ %s %s\n", method, path));
        logMessage.append(String.format("│ Duration: %d ms\n", duration));
        logMessage.append(String.format("│ Exception: %s\n", error.getClass().getSimpleName()));
        logMessage.append(String.format("│ Message: %s\n", error.getMessage()));
        logMessage.append("└──────────────────────────────────────────────────────────────────────────────");

        logger.warn(logMessage.toString());
    }

    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        // Avoid logging sensitive objects or request/response objects
        String className = arg.getClass().getSimpleName();
        if (className.contains("Request") || className.contains("Response") ||
            className.contains("Servlet") || className.contains("Principal")) {
            return "[" + className + "]";
        }
        try {
            String json = objectMapper.writeValueAsString(arg);
            return truncate(json);
        } catch (JsonProcessingException e) {
            return arg.toString();
        }
    }

    private String formatResponseBody(Object body) {
        if (body == null) {
            return "null";
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            return truncate(json);
        } catch (JsonProcessingException e) {
            return truncate(body.toString());
        }
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= MAX_BODY_LENGTH) return text;
        return text.substring(0, MAX_BODY_LENGTH) + "... [truncated]";
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
