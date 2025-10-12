package org.ananie.parishApp.configurations;

import com.itextpdf.text.BaseColor;
import com.zaxxer.hikari.HikariDataSource; 
import jakarta.validation.Validator;

import org.ananie.parishApp.utilities.ApplicationDirectoryUtil;
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
@ComponentScan(basePackages = "org.ananie")
@EnableJpaRepositories(basePackages = "org.ananie.parishApp.dao")
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class AppConfig {

    /**
     * H2 Database Configuration (For Development/Testing)
     */
    @Bean
    @Profile({"h2"})
    public DataSource h2DataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        
        String dbPath = ApplicationDirectoryUtil.getDatabaseFilePath();
        System.out.println("Database will be stored at: " + dbPath);
        
        ds.setJdbcUrl("jdbc:h2:file:" + dbPath + 
                     ";DB_CLOSE_ON_EXIT=FALSE" + 
                     ";AUTO_SERVER=TRUE" + 
                     ";MODE=MySQL" + 
                     ";DATABASE_TO_LOWER=TRUE" +
                     ";CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(30000);
        
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
     * MySQL Database Configuration (DEFAULT - Production)
     */
    @Bean
    @Profile({"default", "mysql", "production"})
    public DataSource mysqlDataSource() {
        System.out.println("==============================================");
        System.out.println("Attempting to connect to MySQL database...");
        System.out.println("URL: jdbc:mysql://localhost:3306/parishAppDB");
        System.out.println("==============================================");
        
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/parishAppDB?allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true&serverTimezone=UTC");
        ds.setUsername("ananie"); 
        ds.setPassword("ananie@11072"); 
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(5000); // Reduced to 5 seconds for faster failure
        
        // MySQL-specific optimizations
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "250");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds.addDataSourceProperty("useServerPrepStmts", "true");

        return ds;
    }

    /**
     * H2-specific Entity Manager Factory
     */
    @Bean(name = "entityManagerFactory")
    @Profile("h2")
    public LocalContainerEntityManagerFactoryBean h2EntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(h2DataSource());
        emf.setPackagesToScan("org.ananie.parishApp.model");

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        emf.setJpaVendorAdapter(adapter);

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.use_sql_comments", "true");
        props.put("hibernate.id.new_generator_mappings", "true");
        props.put(AvailableSettings.SHOW_SQL, true);
        props.put(AvailableSettings.FORMAT_SQL, true);
        props.put(AvailableSettings.GENERATE_STATISTICS, true);
        props.put("hibernate.jdbc.batch_size", "20");
        props.put("hibernate.order_inserts", "true");
        props.put("hibernate.order_updates", "true");
        props.put("hibernate.jdbc.batch_versioned_data", "true");
        
        emf.setJpaProperties(props);
        return emf;
    }

    /**
     * MySQL Entity Manager Factory (DEFAULT - Production)
     */
    @Bean(name = "entityManagerFactory")
    @Profile({"default", "mysql", "production"})
    public LocalContainerEntityManagerFactoryBean mysqlEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(mysqlDataSource());
        emf.setPackagesToScan("org.ananie.parishApp.model");

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
        props.put(AvailableSettings.SHOW_SQL, true);
        props.put(AvailableSettings.FORMAT_SQL, true);
        props.put(AvailableSettings.GENERATE_STATISTICS, true);
        props.put("hibernate.jdbc.batch_size", "20");
        props.put("hibernate.order_inserts", "true");
        props.put("hibernate.order_updates", "true");
        props.put("hibernate.jdbc.batch_versioned_data", "true");
        
        emf.setJpaProperties(props);
        return emf;
    }

    /**
     * Test Entity Manager Factory
     */
    @Bean(name = "entityManagerFactory")
    @Profile("test")
    public LocalContainerEntityManagerFactoryBean testEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(h2TestDataSource());
        emf.setPackagesToScan("org.ananie.parishApp.model");

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        emf.setJpaVendorAdapter(adapter);

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        
        emf.setJpaProperties(props);
        return emf;
    }

    @Bean(name = "transactionManager")
    @Profile({"default", "mysql", "production"})
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(mysqlEntityManagerFactory().getObject());
        return tm;
    }

    @Bean(name = "transactionManager")
    @Profile("h2")
    public PlatformTransactionManager h2TransactionManager() {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(h2EntityManagerFactory().getObject());
        return tm;
    }

    @Bean(name = "transactionManager")
    @Profile("test")
    public PlatformTransactionManager testTransactionManager() {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(testEntityManagerFactory().getObject());
        return tm;
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PDFConfig pdfConfig() {
        PDFConfig config = new PDFConfig();

        config.setLogoPath("/parish_logo.png");
        config.setParishName("PARUWASI MUSHA - MUTAGATIFU DOMINIKO SAVIYO");
        
        String outputDir = ApplicationDirectoryUtil.getReportsDirectory();
        System.out.println("PDF reports will be stored at: " + outputDir);
        
        config.setDefaultOutputDirectory(outputDir);
        config.setAutoOpenAfterGeneration(true);
        config.setFontFamily("HELVETICA");

        															config.setChurchAddress("xxxx - xxx - xxx");
        config.setChurchPhone("");
        config.setChurchEmail("xxxxxx@gmail.com");

        config.setPageMarginTop(60f);
        config.setPageMarginBottom(80f);
        config.setPageMarginLeft(40f);
        config.setPageMarginRight(40f);

        config.setTitleFontSize(20);
        config.setSubtitleFontSize(14);
        config.setHeaderFontSize(12);
        config.setLabelFontSize(11);
        config.setValueFontSize(11);
        config.setTableFontSize(10);

        config.setHeaderColor(new BaseColor(52, 58, 64));
        config.setAlternateRowColor(new BaseColor(248, 249, 250));
        config.setTitleColor(new BaseColor(0, 123, 255));

        return config;
    }

    @Bean
    public ApplicationDirectoryInitializer applicationDirectoryInitializer() {
        return new ApplicationDirectoryInitializer();
    }
    
    public static class ApplicationDirectoryInitializer {
        public ApplicationDirectoryInitializer() {
            ApplicationDirectoryUtil.initializeApplicationDirectories();
            ApplicationDirectoryUtil.printApplicationInfo();
        }
    }
}