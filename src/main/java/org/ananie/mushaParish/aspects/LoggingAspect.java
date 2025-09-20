package org.ananie.mushaParish.aspects;

import org.ananie.mushaParish.services.LoggingService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    
    @Autowired
    private LoggingService loggingService;
    
    /**
     * Pointcut that matches all public methods in repository classes
     * within the org.ananie.mushaParish.dao package and its sub-packages
     */
    @Pointcut("execution(public * org.ananie.mushaParish.dao..*(..))")
    public void repositoryMethods() {}
    
    /**
     * Additional pointcut for methods annotated with @Repository
     * This catches repository classes that might be in other packages
     */
    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryClasses() {}
    
    /**
     * Combined pointcut - matches either repository package OR @Repository annotation
     */
    @Pointcut("repositoryMethods() || repositoryClasses()")
    public void allRepositoryOperations() {}
    
    /**
     * Around advice that logs performance metrics for repository methods
     * All logging is done through LoggingService to maintain consistency
     */
    @Around("allRepositoryOperations()")
    public Object logRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Extract method information
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Create operation description
        String operation = className + "." + methodName;
        
        // Log method entry using LoggingService
        loggingService.logUserAction("REPOSITORY_ENTRY", operation + " called with " + args.length + " parameters");
        
        Object result = null;
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Proceed with the actual method execution
            result = joinPoint.proceed();
            success = true;
            
            // Log success using LoggingService
            loggingService.logUserAction("REPOSITORY_SUCCESS", operation + " completed successfully");
            
            return result;
            
        } catch (Exception e) {
            // Log error using LoggingService
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            loggingService.logError("Repository Operation: " + operation, e);
            
            // Re-throw the exception to maintain normal error handling
            throw e;
            
        } finally {
            // Calculate execution time
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Log performance metrics using LoggingService
            loggingService.logPerformance(operation, duration);
            
            // Additional detailed logging using LoggingService
            if (success) {
                String performanceMessage = operation + " executed in " + duration + "ms [SUCCESS]";
                loggingService.logUserAction("REPO_PERFORMANCE", performanceMessage);
                
                // Log warning for slow operations using LoggingService
                if (duration > 1000) {
                    String slowOpMessage = operation + " took " + duration + "ms - Consider optimization";
                    loggingService.logUserAction("SLOW_REPOSITORY_OPERATION", slowOpMessage);
                }
                
                // Log result information using LoggingService
                logResultInfoViaService(operation, result, duration);
                
            } else {
                String failureMessage = operation + " executed in " + duration + "ms [FAILED: " + errorMessage + "]";
                loggingService.logUserAction("REPO_PERFORMANCE_FAILURE", failureMessage);
            }
        }
    }
    
    /**
     * Log information about the result using LoggingService
     */
    private void logResultInfoViaService(String operation, Object result, long duration) {
        String resultMessage;
        
        if (result == null) {
            resultMessage = operation + " returned null in " + duration + "ms";
        } else if (result instanceof java.util.Collection) {
            int size = ((java.util.Collection<?>) result).size();
            resultMessage = operation + " returned collection with " + size + " items in " + duration + "ms";
        } else if (result instanceof java.util.Optional) {
            boolean present = ((java.util.Optional<?>) result).isPresent();
            resultMessage = operation + " returned Optional." + (present ? "present" : "empty") + " in " + duration + "ms";
        } else {
            resultMessage = operation + " returned " + result.getClass().getSimpleName() + " instance in " + duration + "ms";
        }
        
        loggingService.logUserAction("REPO_RESULT", resultMessage);
    }
    
    /**
     * Pointcut for specific CRUD operations that we want to track separately
     */
    @Pointcut("execution(* org.ananie.mushaParish.dao..*.save*(..)) || " +
              "execution(* org.ananie.mushaParish.dao..*.find*(..)) || " +
              "execution(* org.ananie.mushaParish.dao..*.delete*(..)) || " +
              "execution(* org.ananie.mushaParish.dao..*.update*(..))")
    public void crudOperations() {}
    
    /**
     * Special logging for CRUD operations using LoggingService exclusively
     */
    @Around("crudOperations()")
    public Object logCrudOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Determine operation type
        String operationType = getCrudOperationType(methodName);
        String operation = className + "." + methodName;
        
        // Log database operation using LoggingService
        if (args.length > 0 && args[0] != null) {
            loggingService.logDatabaseOperation(operationType, className, args[0]);
        }
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Enhanced CRUD logging using LoggingService
            String crudMessage = operation + " [" + operationType + "] completed in " + duration + "ms";
            loggingService.logUserAction("CRUD_OPERATION", crudMessage);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String failureMessage = operation + " [" + operationType + "] failed after " + duration + "ms";
            loggingService.logUserAction("CRUD_OPERATION_FAILED", failureMessage);
            loggingService.logError("CRUD Operation: " + operation, e);
            throw e;
        }
    }
    
    /**
     * Determine CRUD operation type from method name
     */
    private String getCrudOperationType(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.startsWith("save") || lowerMethodName.startsWith("insert") || 
            lowerMethodName.startsWith("create")) {
            return "CREATE/UPDATE";
        } else if (lowerMethodName.startsWith("find") || lowerMethodName.startsWith("get") || 
                   lowerMethodName.startsWith("select") || lowerMethodName.startsWith("search")) {
            return "READ";
        } else if (lowerMethodName.startsWith("update") || lowerMethodName.startsWith("modify")) {
            return "UPDATE";
        } else if (lowerMethodName.startsWith("delete") || lowerMethodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * Pointcut for methods that might perform batch operations
     */
    @Pointcut("execution(* org.ananie.mushaParish.dao..*.*All(..)) || " +
              "execution(* org.ananie.mushaParish.dao..*.*Batch(..))")
    public void batchOperations() {}
    
    /**
     * Special logging for batch operations using LoggingService exclusively
     */
    @Around("batchOperations()")
    public Object logBatchOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String operation = joinPoint.getTarget().getClass().getSimpleName() + "." + 
                          joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        String startMessage = operation + " started with " + args.length + " parameters";
        loggingService.logUserAction("BATCH_OPERATION_STARTED", startMessage);
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            String completedMessage = operation + " completed in " + duration + "ms";
            loggingService.logUserAction("BATCH_OPERATION_COMPLETED", completedMessage);
            loggingService.logPerformance("BATCH-" + operation, duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String failedMessage = operation + " failed after " + duration + "ms";
            loggingService.logUserAction("BATCH_OPERATION_FAILED", failedMessage);
            loggingService.logError("Batch Operation: " + operation, e);
            throw e;
        }
    }
}