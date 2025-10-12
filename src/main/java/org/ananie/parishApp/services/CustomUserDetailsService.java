package org.ananie.parishApp.services;

import java.time.LocalDateTime;

import org.ananie.parishApp.dao.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ananie.parishApp.model.User;
import org.ananie.parishApp.security.CustomUserDetails;
@Service
public class CustomUserDetailsService implements SecurityUserService {
	private final UserRepository userRepo;
	
	public CustomUserDetailsService (UserRepository userRepo) {
		this.userRepo = userRepo;
		
	}
   
	@Override
	@Transactional( readOnly =true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user= userRepo.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
		return new CustomUserDetails(user);
	}
   
    @Transactional
   	@Override
	public void updateLastLogin(String username) {
		User user = userRepo.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
    	user.setLastLogin(LocalDateTime.now());
    	userRepo.save(user);
	}
    }


