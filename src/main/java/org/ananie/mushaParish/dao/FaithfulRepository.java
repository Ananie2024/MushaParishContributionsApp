package org.ananie.mushaParish.dao;

import java.util.List;
import java.util.Optional;

import org.ananie.mushaParish.model.BEC;
import org.ananie.mushaParish.model.Faithful;
import org.ananie.mushaParish.model.SubParish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaithfulRepository extends JpaRepository<Faithful, Long>{
 Optional<Faithful> findByName(String name);

 // Custom method to find Faithfuls by their name (case-insensitive contains)
 // This will be useful for your "Search faithful" functionality
 List<Faithful> findByNameContainingIgnoreCase(String name);

 // Custom method to find Faithfuls belonging to a specific BEC
 List<Faithful> findByBec(BEC bec);

 // Custom method to find Faithfuls belonging to a specific SubParish (through BEC)
 // This uses a "property path" through the 'bec' relationship
 List<Faithful> findByBec_SubParish(SubParish subParish);

 // To find by name within a specific BEC
 Optional<Faithful> findByNameAndBec(String name, BEC bec);

 // Find all faithfuls sorted by name
 List<Faithful> findAllByOrderByNameAsc();
 
 @EntityGraph(attributePaths = "contributions")
 Optional<Faithful>findById(Long id);

}
