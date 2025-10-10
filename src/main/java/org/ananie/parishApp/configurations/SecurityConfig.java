package org.ananie.parishApp.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
	
	 @PostConstruct
	    public void init() {
	        //  MODE_INHERITABLETHREADLOCAL for JavaFX threading
	        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	    }
	
	@Bean
	public UserDetailsService userDetailsService() {
		
	UserDetails admin = User.builder()
			.username("HAFASHIMANA Ananie")
			.password("11072@two")
			.roles("MANAGER")
			.build();
	UserDetails viewer = User.builder()
			.username("Guest")
			.password("1234")
			.roles("VIEWER")
			.build();
	return new InMemoryUserDetailsManager(admin,viewer);
		
	
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
		
	}
	@Bean
	public AuthenticationManager authenticationManager(
			UserDetailsService userdetailsService,PasswordEncoder passwordEncoder ) {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userdetailsService);
		authProvider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(authProvider);
		
	}
}
