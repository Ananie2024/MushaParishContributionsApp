package org.ananie.parishApp.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ananie.parishApp.dao.UserRepository;
import org.ananie.parishApp.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService  {
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	
	public UserService (UserRepository userRepo,PasswordEncoder passwordEncoder) {
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;		
		
	}
	
	@Transactional
	public User saveUser(String username, String email, String password, String fullname, Set<String> roles) {
		if(userRepo.existsByUsername(username)) {
			throw new IllegalArgumentException("This user already exists");
		}
		User user= new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setEmail(email);
		user.setFullName(fullname);
		user.setEnabled(true);
		user.setRoles(roles);
		
		return userRepo.save(user);
		
	}
	@Transactional
	public User updateUser(Long Id, String username, String fullname, Set<String>roles, Boolean enabled) {
		
		User user = userRepo.findById(Id).orElseThrow(() -> new IllegalArgumentException("User not found: " + Id));
		if(fullname!=null && !fullname.trim().isEmpty()) {
			user.setFullName(fullname);
		}
		if(roles!=null && !roles.isEmpty()) {
			user.setRoles(roles);
		}
		if(enabled != null) {
			user.setEnabled(enabled);
		}
		
		return userRepo.save(user);
	}
	@Transactional 
	public String deleteUser(String username) {
		
		if(!userRepo.existsByUsername(username)) {
			throw new IllegalArgumentException("This user does not exist");
		}
		User user= userRepo.findByUsername(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found "));
		userRepo.delete(user);
		
		return "The user  " + username + " is successfully deleted";
		}
	 @Transactional
	  public void changePassword(Long userId, String newRawPassword) {
	        User user = userRepo.findById(userId)
	                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

	        user.setPassword(passwordEncoder.encode(newRawPassword));
	        userRepo.save(user);
	 }
	 @Transactional
	 public void changeOwnPassword(String username, String oldPassword, String newPassword) {
		 User user = userRepo.findByUsername(username)
	                .orElseThrow(() -> new IllegalArgumentException("User not found "));
		 if(!passwordEncoder.matches(oldPassword, user.getPassword())) {
			 throw new IllegalArgumentException("This user does not exist or the password provided is incorrect");
	 }
	    user.setPassword(passwordEncoder.encode(newPassword));
	    userRepo.save(user);
     }
	 public List<User> findAllUsers() {
	        return userRepo.findAll();
	    }

	    public boolean existsByUsername(String username) {
	        return userRepo.existsByUsername(username);
	    }
	}
