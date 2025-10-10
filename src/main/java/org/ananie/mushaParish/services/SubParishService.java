package org.ananie.mushaParish.services;

import org.ananie.mushaParish.dao.SubParishRepository;
import org.ananie.parishApp.model.SubParish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For transaction management

import java.util.List;
import java.util.Optional;

@Service // Marks this class as a Spring Service component
@Transactional(readOnly = true) // Default to read-only transactions for methods not modifying data
public class SubParishService {

    private final SubParishRepository subParishRepository;

    // Constructor injection for the repository
    // Spring will automatically inject SubParishRepository here
    @Autowired
    public SubParishService(SubParishRepository subParishRepository) {
        this.subParishRepository = subParishRepository;
    }

    @Transactional // Override to make this method transactional (modifies data)
    public SubParish save(SubParish subParish) {
        // You could add business logic here before saving, e.g., validation
        return subParishRepository.save(subParish);
    }

    @Transactional
    public SubParish update(SubParish subParish) {
        // Ensure the ID exists before updating, or handle it as a save if ID is null
        if (subParish.getId() == null || !subParishRepository.existsById(subParish.getId())) {
            throw new IllegalArgumentException("Cannot update SubParish: ID is null or does not exist.");
        }
        return subParishRepository.save(subParish); // save() also acts as update() if ID exists
    }

    public Optional<SubParish> findByName(String name) {
        return subParishRepository.findByName(name);
    }

    public List<SubParish> findAll() {
        return subParishRepository.findAll();
    }
    
    public List<SubParish> findAllOrderedByName() {
        return subParishRepository.findAllByOrderByNameAsc(); 
    }
}