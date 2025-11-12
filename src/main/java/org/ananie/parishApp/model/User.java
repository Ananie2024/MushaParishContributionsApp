package org.ananie.parishApp.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name ="users")
public class User {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(nullable = false, unique = true, length = 50)
	    private String username;
	    
	    @Column(nullable =false, unique = true, length = 150)
	    @Email(message ="email should be valid")
	    @NotBlank( message ="email is required")
	    private String email;

	    @Column(nullable = false)
	    private String password;

	    @Column(nullable = false, length = 100)
	    private String fullName;

	    @Column(nullable = false)
	    private boolean enabled = true;

	    @Column(nullable = false)
	    private boolean accountNonExpired = true;

	    @Column(nullable = false)
	    private boolean accountNonLocked = true;

	    @Column(nullable = false)
	    private boolean credentialsNonExpired = true;

	    @ElementCollection(fetch = FetchType.EAGER)
	    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	    @Column(name = "role")
	    private Set<String> roles = new HashSet<>();
	    
	    @CreationTimestamp
	    @Column(name = "created_at")
	    private LocalDateTime createdAt;
	    
	    @UpdateTimestamp
	    @Column(name = "updated_at")
	    private LocalDateTime updatedAt;

	    @Column(name = "last_login")
	    private LocalDateTime lastLogin;

	    // Convenience method to add roles
	    public void addRole(String role) {
	        if (this.roles == null) {
	            this.roles = new HashSet<>();
	        }
	        this.roles.add(role);
	    }
	    
	    // Convenience method to remove roles
	    public void removeRole(String role) {
	        if (this.roles != null) {
	            this.roles.remove(role);
	        }
	    }
	    public User (String username, String password, String email) {
	    	this.username= username;
	    	this.password=password;
	    	this.email=email;
	    }
}
