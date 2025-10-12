package org.ananie.parishApp.services;

import org.ananie.parishApp.dao.BECRepository;
import org.ananie.parishApp.dao.ContributionRepository;
import org.ananie.parishApp.dao.FaithfulYearlyContributionProjection;
import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.model.FaithfulContributionRow;
import org.ananie.parishApp.model.SubParish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final FaithfulService faithfulService;
    private final BECRepository becRepository;

    @Autowired
    public ContributionService(ContributionRepository contributionRepository,
                               FaithfulService faithfulService,
                               BECRepository becRepository) {
        this.contributionRepository = contributionRepository;
        this.faithfulService = faithfulService;
        this.becRepository = becRepository;
    }
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public Contribution save(Contribution contribution) {
        if (contribution.getFaithful() == null || contribution.getFaithful().getId() == null)
            throw new IllegalArgumentException("Contribution must be associated with an existing Faithful.");
        faithfulService.findById(contribution.getFaithful().getId())
                .orElseThrow(() -> new IllegalArgumentException("Associated Faithful does not exist."));
        if (contribution.getAmount() == null || contribution.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Contribution amount must be positive.");
        if (contribution.getDate() == null)
            throw new IllegalArgumentException("Contribution date cannot be empty.");
        if (contribution.getYear() == null)
            contribution.setYear(contribution.getDate().getYear());

        return contributionRepository.save(contribution);
    }
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public Contribution update(Contribution contribution) {
        if (contribution.getId() == null || !contributionRepository.existsById(contribution.getId()))
            throw new IllegalArgumentException("Cannot update Contribution: ID is null or does not exist.");
        return save(contribution);
    }
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public void delete(Long id) {
    	 Contribution contribution = contributionRepository.findById(id)
    	            .orElseThrow(() -> new IllegalArgumentException("Contribution not found"));
    	        
    	        contributionRepository.delete(contribution);
    	        contributionRepository.flush(); // Force immediate execution
    	        
    	        // Verify deletion
    	        if (contributionRepository.existsById(id)) {
    	            throw new IllegalStateException("Deletion failed for contribution: " + id);
    	        }
    	        
    }

    public Optional<Contribution> findById(Long id) {
        return contributionRepository.findById(id);
    }

    public List<Contribution> findAll() {
        return contributionRepository.findAll();
    }

    public List<Contribution> findByFaithful(Faithful faithful) {
        if (faithful == null || faithful.getId() == null)
            throw new IllegalArgumentException("Faithful must be provided.");
        return contributionRepository.findByFaithfulOrderByDateDesc(faithful);
    }

    public List<Integer> getAvailableYears() {
        return contributionRepository.findDistinctYears();
    }

    public BigDecimal getTotalContributions(Integer year) {
        return (year == null)
                ? contributionRepository.sumAllContributions()
                : contributionRepository.sumContributionsByYear(year);
    }

    public Map<SubParish, BigDecimal> getTotalsBySubParish(Integer year) {
        List<Object[]> results = (year == null)
                ? contributionRepository.sumContributionsBySubParish()
                : contributionRepository.sumContributionsBySubParishAndYear(year);
        Map<SubParish, BigDecimal> totals = new HashMap<>();
        for (Object[] result : results) {
            totals.put((SubParish) result[0], (BigDecimal) result[1]);
        }
        return totals;
    }

    public Map<BEC, BigDecimal> getTotalsByBecInSubParish(SubParish subParish, Integer year) {
        List<Object[]> results = (year == null)
                ? contributionRepository.sumContributionsByBecInSubParish(subParish)
                : contributionRepository.sumContributionsByBecInSubParishAndYear(subParish, year);
        Map<BEC, BigDecimal> totals = new HashMap<>();
        for (Object[] result : results) {
            totals.put((BEC) result[0], (BigDecimal) result[1]);
        }
        return totals;
    }

    /**
     * Generate pivot-style contribution rows filtered by SubParish and/or BEC
     */
    public List<FaithfulContributionRow> getFilteredContributionMatrix(SubParish subParish, BEC bec) {
        List<FaithfulYearlyContributionProjection> data;

        if (subParish != null && bec != null) {
            data = contributionRepository.getYearlyContributionSummaryByBEC(bec);
        } else if (subParish != null) {
            List<BEC> becs = becRepository.findBySubParish(subParish);
            data = contributionRepository.getYearlyContributionSummaryByBECIn(becs);
        } else {
            data = contributionRepository.getYearlyContributionSummary();
        }

        // 🚀 Step 1: Identify all years present in the data
        Set<Integer> allYears = data.stream()
            .map(FaithfulYearlyContributionProjection::getYear)
            .collect(Collectors.toCollection(TreeSet::new)); // maintains order

        Map<String, FaithfulContributionRow> rowMap = new LinkedHashMap<>();

        // 🚀 Step 2: Group contributions by name and year
        for (FaithfulYearlyContributionProjection record : data) {
            String name = record.getName();
            Integer year = record.getYear();
            BigDecimal amount = record.getTotalAmount();

            rowMap
                .computeIfAbsent(name, k -> new FaithfulContributionRow(name))
                .getYearlyContributions()
                .put(year, amount);
        }

        // 🚀 Step 3: Pad missing years and calculate total
        for (FaithfulContributionRow row : rowMap.values()) {
            BigDecimal total = BigDecimal.ZERO;
            for (Integer year : allYears) {
                BigDecimal amount = row.getYearlyContributions().getOrDefault(year, new BigDecimal("0000.00"));
                row.getYearlyContributions().putIfAbsent(year, amount);
                total = total.add(amount);
            }
            row.setTotalContribution(total); // assuming your class has this setter
        }

        return new ArrayList<>(rowMap.values());
    }

}
