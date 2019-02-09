package security;

import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.UserCredentialsDataSourceAdapter;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DatabaseConfig {
	
	Logger log = Logger.getLogger(DatabaseConfig.class);
	
	@Bean(name = "MyDatabaseName")
	@Primary
	public DataSource exampleDbDataSource() {
		String pass = "defaultPasswordWillFailLogin";
		try {
			// JNDI scope prefixes
			// java:comp - The namespace is scoped to the current component (i.e. EJB)
			// java:module - Scoped to the current module
			// java:app - Scoped to the current application
			// java:global - Scoped to the application server
			//to manage passwords in a centralized way, we are loading them into the local JNDI namespace on the servers
			pass = InitialContext.doLookup("java:comp/env/login/MyDatabaseUserId");
		} catch (NamingException e) {
			log.warn("Password lookup failed for API login", e);

		}
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
        DataSource ds = dataSourceLookup.getDataSource("java:/jdbc/MyDatabase");
		//this class allows the override of the user/pass from the config files and implements the DataSource interface
		UserCredentialsDataSourceAdapter userDs = new UserCredentialsDataSourceAdapter();
		userDs.setTargetDataSource(ds);
		userDs.setUsername("MyDatabaseUserId");
		userDs.setPassword(pass);
		try {
			userDs.getConnection().close();
		}
		catch (SQLException e) {
			log.fatal("DB login failed for MyDatabaseUserId", e);
		}
		return userDs;
	}
	
	@Bean(name="FrontDoorJdbcTemplate")
	public JdbcTemplate MobilityVendorScorecardJdbcTemplate(@Qualifier("MyDatabase") DataSource db) {
		return new JdbcTemplate(db);
	}
	
	@Primary
	@Bean(name = "entityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean r = 
				builder.dataSource(dataSource)
				.packages("com.myproject.entity")
				.persistenceUnit("entityManager")
				.build();
		return r;
	}
	
	@Primary
	@Bean(name = "transactionManager")
	public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory")EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
	
	@Bean
	public RestTemplate rest() {
	return new RestTemplate();
	}

}

