package org.ananie.mushaParish.configurations;

import com.itextpdf.text.BaseColor;
import com.zaxxer.hikari.HikariDataSource; 
import jakarta.validation.Validator;
import org.ananie.mushaParish.utilities.ApplicationDirectoryUtil;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;

import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "org.ananie.mushaParish")
@EnableJpaRepositories(basePackages = "org.ananie.mushaParish.dao")
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class AppConfig {

    /**
     * H2 Database Configuration (Default - Embedded)
     * Perfect for packaging and distribution
     */
    @Bean
    @Profile({"default", "h2", "production"})
    public DataSource h2DataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        
        // Use utility to get database path in installation directory
        String dbPath = ApplicationDirectoryUtil.getDatabaseFilePath();
        System.out.println("Database will be stored at: " + dbPath);
        
        // H2 connection URL with file mode
        ds.setJdbcUrl("jdbc:h2:file:" + dbPath + 
                     ";DB_CLOSE_ON_EXIT=FALSE" + 
                     ";AUTO_SERVER=TRUE" + 
                     ";MODE=MySQL" + 
                     ";DATABASE_TO_LOWER=TRUE" +
                     ";CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        
        ds.setUsername("sa");
        ds.setPassword(""); // H2 default
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(30000); // 30 seconds
        
        // H2-specific optimizations
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "250");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
        // H2 connection URL with file mode
        ds.setJdbcUrl("jdbc:h2:file:" + dbPath + 
                     ";AUTO_SERVER=TRUE" + 
                     ";MODE=MySQL" + 
                     ";DATABASE_TO_LOWER=TRUE" +
                     ";CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        
        ds.setUsername("sa");
        ds.setPassword(""); // H2 default
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(30000); // 30 seconds
        
        // H2-specific optimizations
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "250");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return ds;
    }

    /**
     * In-Memory H2 Database Configuration (For Testing)
     */
    @Bean
    @Profile("test")
    public DataSource h2TestDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMaximumPoolSize(5);
        ds.setConnectionTimeout(10000);
        
        return ds;
    }

    /**
     * MySQL Database Configuration (For Development/Migration)
     * Keep this for backward compatibility or migration purposes
     */
    @Bean
    @Profile("mysql")
    public DataSource mysqlDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/AmaturoMushaDB?allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true&serverTimezone=UTC");
        ds.setUsername("ananie"); 
        ds.setPassword("ananie@11072"); 
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(50000);

        return ds;
    }

    /**
     * Primary DataSource Bean - will use H2 by default
     */
    @Bean
    public DataSource dataSource() {
        // Default to H2 - can be overridden by profile
        return h2DataSource();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("org.ananie.mushaParish.model");

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        // Use H2 dialect by default - will work with both H2 and MySQL due to MODE=MySQL setting
        adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        emf.setJpaVendorAdapter(adapter);

        Properties props = new Properties();
        
        // Schema management - create/update tables automatically
        props.put("hibernate.hbm2ddl.auto", "update");
        
        // Use H2 dialect - compatible with MySQL mode
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        
        // SQL logging and formatting
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.use_sql_comments", "true");
        
        // Modern Hibernate settings
        props.put("hibernate.id.new_generator_mappings", "true");
        props.put(AvailableSettings.SHOW_SQL, true);
        props.put(AvailableSettings.FORMAT_SQL, true);
        props.put(AvailableSettings.GENERATE_STATISTICS, true);
        
        // H2-specific optimizations
        props.put("hibernate.jdbc.batch_size", "20");
        props.put("hibernate.order_inserts", "true");
        props.put("hibernate.order_updates", "true");
        props.put("hibernate.jdbc.batch_versioned_data", "true");
        
        // Parameter logging (optional - can be verbose)
       props.put("hibernate.type.descriptor.sql.BasicBinder", "TRACE");
        
        emf.setJpaProperties(props);

        return emf;
    }

    /**
     * Profile-specific Entity Manager Factory for MySQL
     */
    @Bean
    @Profile("mysql")
    public LocalContainerEntityManagerFactoryBean mysqlEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(mysqlDataSource());
        emf.setPackagesToScan("org.ananie.mushaParish.model");

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        adapter.setDatabasePlatform("org.hibernate.dialect.MySQL8Dialect");
        emf.setJpaVendorAdapter(adapter);

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.use_sql_comments", "true");
        props.put("hibernate.id.new_generator_mappings", "true");
        
        emf.setJpaProperties(props);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory().getObject());
        return tm;
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PDFConfig pdfConfig() {
        PDFConfig config = new PDFConfig();

        // PDF Generation Settings - Updated for better resource handling
        config.setLogoPath("/parish_logo.png"); // Resource path - will work in packaged JAR
        config.setParishName("PARUWASI MUSHA - MUTAGATIFU DOMINIKO SAVIYO");
        
        // Use application installation directory for reports
        String outputDir = ApplicationDirectoryUtil.getReportsDirectory();
        System.out.println("PDF reports will be stored at: " + outputDir);
        
        config.setDefaultOutputDirectory(outputDir);
        config.setAutoOpenAfterGeneration(true);
        config.setFontFamily("HELVETICA");

        // Church Information
        config.setChurchAddress("MUSHA - RWAMAGANA - RWANDA");
        config.setChurchPhone("+250 788 827 032");
        config.setChurchEmail("mushaparish@gmail.com");

        // Page Layout Settings
        config.setPageMarginTop(60f);
        config.setPageMarginBottom(80f);
        config.setPageMarginLeft(40f);
        config.setPageMarginRight(40f);

        // Font Sizes
        config.setTitleFontSize(20);
        config.setSubtitleFontSize(14);
        config.setHeaderFontSize(12);
        config.setLabelFontSize(11);
        config.setValueFontSize(11);
        config.setTableFontSize(10);

        // Colors
        config.setHeaderColor(new BaseColor(52, 58, 64)); // Dark gray
        config.setAlternateRowColor(new BaseColor(248, 249, 250)); // Light gray
        config.setTitleColor(new BaseColor(0, 123, 255)); // Blue

        return config;
    }

    /**
     * Initialize application directories on startup
     * This ensures all necessary directories are created
     */
    @Bean
    public ApplicationDirectoryInitializer applicationDirectoryInitializer() {
        return new ApplicationDirectoryInitializer();
    }
    
    /**
     * Simple bean to initialize directories on startup
     */
    public static class ApplicationDirectoryInitializer {
        public ApplicationDirectoryInitializer() {
            ApplicationDirectoryUtil.initializeApplicationDirectories();
            ApplicationDirectoryUtil.printApplicationInfo();
        }
     }
   }
 
   
