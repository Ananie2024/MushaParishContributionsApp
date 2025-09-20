package org.ananie.mushaParish.dao;

import java.util.List;
import java.util.Optional;

import org.ananie.mushaParish.model.SubParish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubParishRepository extends JpaRepository<SubParish, Long> {

	Optional<SubParish> findByName(String name);

	List<SubParish> findAllByOrderByNameAsc();

}
