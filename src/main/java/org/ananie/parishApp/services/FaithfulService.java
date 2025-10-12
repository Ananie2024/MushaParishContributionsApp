package org.ananie.parishApp.services;

import org.ananie.parishApp.dao.ContributionRepository;
import org.ananie.parishApp.dao.FaithfulRepository;
import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.model.SubParish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FaithfulService {

    private final FaithfulRepository faithfulRepository;
    private final PlatformTransactionManager transactionManager;

    // You might also need BECService to ensure BEC exists
    private final BECService becService;

    @Autowired
    public FaithfulService(FaithfulRepository faithfulRepository, BECService becService, ContributionRepository contributionRepository, PlatformTransactionManager transactionManager) {
        this.faithfulRepository = faithfulRepository;
		this.transactionManager = transactionManager;
        this.becService = becService;
    }

    @Transactional
    public Faithful save(Faithful faithful) {
        // Business logic: Ensure associated BEC exists
        if (faithful.getBec() == null || faithful.getBec().getId() == null) {
            throw new IllegalArgumentException("Faithful must be associated with an existing BEC.");
        }
        // Optionally, check if the BEC ID actually exists in the database
        becService.findById(faithful.getBec().getId())
                  .orElseThrow(() -> new IllegalArgumentException("Associated BEC does not exist."));

        // Add more validation if needed (e.g., name not empty)
        if (faithful.getName() == null || faithful.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Faithful name cannot be empty.");
        }

        // You might want to check for duplicate faithfuls (e.g., same name in same BEC)
        Optional<Faithful> existingFaithful = faithfulRepository.findByNameAndBec(faithful.getName(), faithful.getBec());
        if (existingFaithful.isPresent() && (faithful.getId() == null || !existingFaithful.get().getId().equals(faithful.getId()))) {
            throw new IllegalArgumentException("A faithful with this name already exists in the selected BEC.");
        }

        return faithfulRepository.save(faithful);
    }

    @Transactional
    public Faithful update(Faithful faithful) {
        if (faithful.getId() == null || !faithfulRepository.existsById(faithful.getId())) {
            throw new IllegalArgumentException("Cannot update Faithful: ID is null or does not exist.");
        }
        return save(faithful); // Reuse save logic for update, including BEC check and uniqueness
    }

    @Transactional
    public void deleteFaithful(Long id) {
        // 1. Explicitly fetch with contributions to avoid lazy loading issues
        Faithful faithful = faithfulRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Faithful not found"));
        
        // 3. Delete the faithful
        faithfulRepository.delete(faithful);
        faithfulRepository.flush(); // Force commit

        // 4. Verify deletion in a NEW transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            boolean exists = faithfulRepository.existsById(id);
            if (exists) {
                throw new IllegalStateException("Failed to delete faithful with id: " + id);
            }
            return null;
        });
    }

    public Optional<Faithful> findById(Long id) {
        return faithfulRepository.findById(id);
    }

    public List<Faithful> findAll() {
        return faithfulRepository.findAllByOrderByNameAsc(); // Assuming this method exists and is preferred
    }

    public List<Faithful> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll(); // Return all if search query is empty
        }
        return faithfulRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Faithful> findByBec(BEC bec) {
        if (bec == null || bec.getId() == null) {
           throw new IllegalArgumentException(" Shyiramo amazina y'umuryangoremezo ya nyayo");}
        return faithfulRepository.findByBec(bec); 
    }

    public List<Faithful> findBySubParish(SubParish subParish) {
        if (subParish == null || subParish.getId() == null) {
        	throw new IllegalArgumentException("Shyiramo izina rya Santarali rya nyaryo");
        }
        return faithfulRepository.findByBec_SubParish(subParish);
    }
}