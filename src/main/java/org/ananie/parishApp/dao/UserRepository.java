package org.ananie.parishApp.dao;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.ananie.parishApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
	
	Optional<User> findByUsername(String username);
	boolean existsByUsername(String username);

}
