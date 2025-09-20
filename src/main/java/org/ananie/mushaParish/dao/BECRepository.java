package org.ananie.mushaParish.dao;


import org.ananie.mushaParish.model.BEC;
import org.ananie.mushaParish.model.SubParish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BECRepository extends JpaRepository<BEC, Long> {

    // Custom method to find BECs by their name
    Optional<BEC> findByName(String name);

    // Custom method to find BECs belonging to a specific SubParish
    List<BEC> findBySubParish(SubParish subParish);

    // Custom method to find a specific BEC by name within a specific SubParish
    Optional<BEC> findByNameAndSubParish(String name, SubParish subParish);

	List<BEC> findAllByOrderByNameAsc();
}