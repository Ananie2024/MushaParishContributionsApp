package org.ananie.parishApp.utilities;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Utility class for managing application directories and paths
 * Ensures consistent directory structure across the application
 */
public class ApplicationDirectoryUtil {
    
    private static final String DATABASE_DIR = "database";
    private static final String REPORTS_DIR = "reports";
    private static final String LOGS_DIR = "logs";
    private static final String CONFIG_DIR = "config";
    private static final String BACKUP_DIR = "backups";
    
    private static String applicationDirectory = null;
    
    /**
     * Get the main application directory
     * This is where all application data will be stored
     */
    public static String getApplicationDirectory() {
        if (applicationDirectory != null) {
            return applicationDirectory;
        }
        
        String appDir;
        
        try {
            // Try to get the directory where the JAR is located
            String jarPath = ApplicationDirectoryUtil.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            
            File jarFile = new File(jarPath);
            
            // If running from JAR, get parent directory
            if (jarFile.isFile() && jarPath.toLowerCase().endsWith(".jar")) {
                appDir = jarFile.getParent();
                System.out.println("Application directory (JAR mode): " + appDir);
            } 
            // If running from IDE/classes directory
            else if (jarPath.contains("target" + File.separator + "classes") || 
                     jarPath.contains("build" + File.separator + "classes")) {
                // Go up from target/classes or build/classes to project root
                appDir = jarFile.getParentFile().getParentFile().getAbsolutePath();
                System.out.println("Application directory (Development mode): " + appDir);
            }
            // IDE bin or out directories
            else if (jarPath.contains(File.separator + "bin") || 
                     jarPath.contains(File.separator + "out")) {
                appDir = jarFile.getParentFile().getAbsolutePath();
                System.out.println("Application directory (IDE mode): " + appDir);
            }
            // Default fallback
            else {
                appDir = System.getProperty("user.dir");
                System.out.println("Application directory (working dir fallback): " + appDir);
            }
            
        } catch (URISyntaxException e) {
            // Fallback to current working directory
            appDir = System.getProperty("user.dir");
            System.out.println("Application directory (exception fallback): " + appDir);
            e.printStackTrace();
        }
        
        // Validate and ensure directory is usable
        appDir = validateAndPrepareDirectory(appDir);
        applicationDirectory = appDir;
        
        return applicationDirectory;
    }
    
    /**
     * Validate directory and ensure it's writable
     */
    private static String validateAndPrepareDirectory(String dirPath) {
        File dir = new File(dirPath);
        
        // Try to create if it doesn't exist
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Warning: Could not create application directory: " + dirPath);
                // Fallback to user home
                dirPath = System.getProperty("user.home") + File.separator + ".mushaparish";
                dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                System.out.println("Falling back to user home directory: " + dirPath);
            }
        }
        
        // Check if writable
        if (!dir.canWrite()) {
            System.err.println("Warning: Directory is not writable: " + dirPath);
            // Fallback to user home
            dirPath = System.getProperty("user.home") + File.separator + ".mushaparish";
            dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            System.out.println("Falling back to writable user home directory: " + dirPath);
        }
        
        return dirPath;
    }
    
    /**
     * Get database directory path
     */
    public static String getDatabaseDirectory() {
        String dbDir = getApplicationDirectory() + File.separator + DATABASE_DIR;
        ensureDirectoryExists(dbDir);
        return dbDir;
    }
    
    /**
     * Get reports directory path
     */
    public static String getReportsDirectory() {
        String reportsDir = getApplicationDirectory() + File.separator + REPORTS_DIR;
        ensureDirectoryExists(reportsDir);
        return reportsDir;
    }
    
    /**
     * Get logs directory path
     */
    public static String getLogsDirectory() {
        String logsDir = getApplicationDirectory() + File.separator + LOGS_DIR;
        ensureDirectoryExists(logsDir);
        return logsDir;
    }
    
    /**
     * Get configuration directory path
     */
    public static String getConfigDirectory() {
        String configDir = getApplicationDirectory() + File.separator + CONFIG_DIR;
        ensureDirectoryExists(configDir);
        return configDir;
    }
    
    /**
     * Get backups directory path
     */
    public static String getBackupDirectory() {
        String backupDir = getApplicationDirectory() + File.separator + BACKUP_DIR;
        ensureDirectoryExists(backupDir);
        return backupDir;
    }
    
    /**
     * Get the full database file path
     */
    public static String getDatabaseFilePath() {
        return getDatabaseDirectory() + File.separator + "mushadb";
    }
    
    /**
     * Ensure a directory exists, create it if it doesn't
     */
    private static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created directory: " + dirPath);
            } else {
                System.err.println("Failed to create directory: " + dirPath);
            }
        }
    }
    
    /**
     * Initialize all application directories
     * Call this during application startup
     */
    public static void initializeApplicationDirectories() {
        System.out.println("Initializing application directory structure...");
        System.out.println("Application root: " + getApplicationDirectory());
        System.out.println("Database directory: " + getDatabaseDirectory());
        System.out.println("Reports directory: " + getReportsDirectory());
        System.out.println("Logs directory: " + getLogsDirectory());
        System.out.println("Config directory: " + getConfigDirectory());
        System.out.println("Backup directory: " + getBackupDirectory());
        System.out.println("Directory initialization complete.");
    }
    
    /**
     * Get application info for debugging
     */
    public static void printApplicationInfo() {
        System.out.println("=== Application Directory Information ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Version: " + System.getProperty("os.version"));
        System.out.println("User Name: " + System.getProperty("user.name"));
        System.out.println("User Home: " + System.getProperty("user.home"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Application Directory: " + getApplicationDirectory());
        System.out.println("========================================");
    }
}