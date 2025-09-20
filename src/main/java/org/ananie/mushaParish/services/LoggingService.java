package org.ananie.mushaParish.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LoggingService {
    
    // Main logger for this service - matches logback configuration
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    
    // Specialized loggers that match logback.xml configuration
    private static final Logger pdfLogger = LoggerFactory.getLogger("org.ananie.mushaParish.services.ContributionReportPDFService");
    private static final Logger dbLogger = LoggerFactory.getLogger("org.hibernate.SQL");
    
    private static final String LOG_DIR = "./logs";
    
    // Initialize logging directory
    static {
        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
                System.out.println("Created log directory: " + logPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }
    
    /**
     * Log application startup
     */
    public void logApplicationStartup() {
        logger.info("=== MUSHA PARISH APPLICATION STARTED ===");
        logger.info("Application started at: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("Operating System: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        logger.info("User: {}", System.getProperty("user.name"));
        logger.info("Working directory: {}", System.getProperty("user.dir"));
        logger.info("Log directory: {}", new File(LOG_DIR).getAbsolutePath());
    }
    
    /**
     * Log application shutdown
     */
    public void logApplicationShutdown() {
        logger.info("=== MUSHA PARISH APPLICATION SHUTDOWN ===");
        logger.info("Application stopped at: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * Log user actions
     */
    public void logUserAction(String action, String details) {
        logger.info("USER ACTION: {} - {}", action, details);
    }
    
    /**
     * Log PDF generation activities
     */
    public void logPDFGeneration(String reportType, String filePath, boolean success) {
        if (success) {
            pdfLogger.info("PDF generated successfully: {} - {}", reportType, filePath);
        } else {
            pdfLogger.error("PDF generation failed: {} - {}", reportType, filePath);
        }
    }
    
    /**
     * Log database operations
     */
    public void logDatabaseOperation(String operation, String entity, Object id) {
        dbLogger.info("DB Operation: {} on {} with ID: {}", operation, entity, id);
    }
    
    /**
     * Log errors with context
     */
    public void logError(String context, Throwable exception) {
        logger.error("ERROR in {}: {} - {}", context, exception.getClass().getSimpleName(), exception.getMessage(), exception);
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformance(String operation, long duration) {
        logger.info("PERFORMANCE: {} completed in {} ms", operation, duration);
    }
    
    /**
     * Log system resources
     */
    public void logSystemResources() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
        long usedMemory = totalMemory - freeMemory;
        
        logger.info("SYSTEM RESOURCES - Max Memory: {}MB, Used Memory: {}MB, Free Memory: {}MB", 
                   maxMemory, usedMemory, freeMemory);
    }
    
    /**
     * Get log file information
     */
    public String getLogFileInfo() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            return "Log directory does not exist";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Log Directory: ").append(logDir.getAbsolutePath()).append("\n");
        
        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles != null && logFiles.length > 0) {
            info.append("Log Files:\n");
            for (File logFile : logFiles) {
                info.append("- ").append(logFile.getName())
                    .append(" (").append(logFile.length() / 1024).append(" KB)")
                    .append(" - Last modified: ")
                    .append(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(logFile.lastModified()),
                        java.time.ZoneId.systemDefault()
                    ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("\n");
            }
        } else {
            info.append("No log files found\n");
        }
        
        return info.toString();
    }
    
    /**
     * Clean old log files (older than specified days)
     */
    public void cleanOldLogs(int daysToKeep) {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) return;
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        
        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles != null) {
            int deletedCount = 0;
            for (File logFile : logFiles) {
                if (logFile.lastModified() < cutoffTime) {
                    if (logFile.delete()) {
                        deletedCount++;
                        logger.info("Deleted old log file: {}", logFile.getName());
                    }
                }
            }
            logger.info("Log cleanup completed. Deleted {} old log files", deletedCount);
        }
    }
    
}