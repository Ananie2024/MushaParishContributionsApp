package org.ananie.mushaParish.services;

import org.ananie.mushaParish.dao.BECRepository;
import org.ananie.mushaParish.dao.SubParishRepository;
import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.SubParish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BECService {
    
    private final BECRepository becRepository;
    private final SubParishRepository subParishRepository;

    @Autowired
    public BECService(BECRepository becRepository, SubParishRepository subParishRepository) {
        this.becRepository = becRepository;
        this.subParishRepository = subParishRepository;
    }

    @Transactional
    public BEC save(BEC bec) {
        // Business logic: Ensure the associated SubParish exists before saving
        if (bec.getSubParish() == null || bec.getSubParish().getId() == null) {
            throw new IllegalArgumentException("BEC must be associated with an existing SubParish.");
        }

        // *** C Fetch managed SubParish entity ***
        SubParish managedSubParish = subParishRepository.findById(bec.getSubParish().getId())
            .orElseThrow(() -> new IllegalArgumentException("SubParish with ID " + 
                bec.getSubParish().getId() + " does not exist."));
        
        // Set the managed SubParish to ensure proper FK relationship
        bec.setSubParish(managedSubParish);

        // Check for uniqueness within the SubParish
        Optional<BEC> existingBEC = becRepository.findByNameAndSubParish(bec.getName(), managedSubParish);
        if (existingBEC.isPresent() && !existingBEC.get().getId().equals(bec.getId())) {
            throw new IllegalArgumentException("A BEC with this name already exists in the selected SubParish.");
        }

        // Save with proper relationship
        BEC savedBEC = becRepository.save(bec);
        
        // Optional: Log for debugging
        System.out.println("Saved BEC '" + savedBEC.getName() + "' with SubParish ID: " + 
                          savedBEC.getSubParish().getId());
        
        return savedBEC;
    }

    @Transactional
    public BEC update(BEC bec) {
        if (bec.getId() == null || !becRepository.existsById(bec.getId())) {
            throw new IllegalArgumentException("Cannot update BEC: ID is null or does not exist.");
        }
        return save(bec); // Reuse save logic for update, handles uniqueness check
    }

    @Transactional
    public void delete(Long id) {
        if (!becRepository.existsById(id)) {
            throw new IllegalArgumentException("Cannot delete BEC: ID " + id + " does not exist.");
        }
        becRepository.deleteById(id);
    }

    public Optional<BEC> findById(Long id) {
        return becRepository.findById(id);
    }

    public Optional<BEC> findByName(String name) {
        return becRepository.findByName(name);
    }

    public List<BEC> findAll() {
        return becRepository.findAll();
    }

    public List<BEC> findBySubParish(SubParish subParish) {
        if (subParish == null || subParish.getId() == null) {
            throw new IllegalArgumentException("SubParish must be provided to find associated BECs.");
        }
        return becRepository.findBySubParish(subParish);
    }

     // Method to get all BECs ordered by name
    public List<BEC> findAllOrderedByName() {
        return becRepository.findAllByOrderByNameAsc();
    }
}