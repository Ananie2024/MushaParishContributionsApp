package org.ananie.mushaParish.dao;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.model.SubParish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    // Custom method to find all contributions for a specific Faithful
    List<Contribution> findByFaithful(Faithful faithful);

    // Custom method to find contributions for a specific Faithful in a given year
    List<Contribution> findByFaithfulAndYear(Faithful faithful, Integer year);

    // Custom method to find contributions for a faithful ordered by date descending
    List<Contribution> findByFaithfulOrderByDateDesc(Faithful faithful);
    
    @Query("SELECT DISTINCT c.year FROM Contribution c ORDER BY c.year DESC")
	List<Integer> findDistinctYears();
    
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c")
	BigDecimal sumAllContributions();
    
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.year = :year")
	BigDecimal sumContributionsByYear(@Param("year") Integer year);

    /**
     * Sum contributions grouped by SubParish (all years)
     */
    @Query("SELECT f.bec.subParish, COALESCE(SUM(c.amount), 0) " +
           "FROM Contribution c JOIN c.faithful f " +
           "GROUP BY f.bec.subParish " +
           "ORDER BY f.bec.subParish.name")
    List<Object[]> sumContributionsBySubParish();

    /**
     * Sum contributions grouped by SubParish for a specific year
     */
    @Query("SELECT f.bec.subParish, COALESCE(SUM(c.amount), 0) " +
           "FROM Contribution c JOIN c.faithful f " +
           "WHERE c.year = :year " +
           "GROUP BY f.bec.subParish " +
           "ORDER BY f.bec.subParish.name")
    List<Object[]> sumContributionsBySubParishAndYear(@Param("year") Integer year);

    /**
     * Sum contributions grouped by BEC within a specific SubParish (all years)
     */
    @Query("SELECT f.bec, COALESCE(SUM(c.amount), 0) " +
           "FROM Contribution c JOIN c.faithful f " +
           "WHERE f.bec.subParish = :subParish " +
           "GROUP BY f.bec " +
           "ORDER BY f.bec.name")
    List<Object[]> sumContributionsByBecInSubParish(@Param("subParish") SubParish subParish);

    /**
     * Sum contributions grouped by BEC within a specific SubParish for a specific year
     */
    @Query("SELECT f.bec, COALESCE(SUM(c.amount), 0) " +
           "FROM Contribution c JOIN c.faithful f " +
           "WHERE f.bec.subParish = :subParish AND c.year = :year " +
           "GROUP BY f.bec " +
           "ORDER BY f.bec.name")
    List<Object[]> sumContributionsByBecInSubParishAndYear(@Param("subParish") SubParish subParish, @Param("year") Integer year);
    
    @Query("SELECT c.faithful.name AS name, c.year AS year, SUM(c.amount) AS totalAmount " +
            "FROM Contribution c " +
            "WHERE c.faithful.bec = :bec " +
            "GROUP BY c.faithful.name, c.year " +
            "ORDER BY c.faithful.name ASC, c.year ASC")
	List<FaithfulYearlyContributionProjection> getYearlyContributionSummaryByBEC(@Param("bec")BEC bec);
    
    @Query("SELECT c.faithful.name AS name, c.year AS year, SUM(c.amount) AS totalAmount " +
            "FROM Contribution c " +
            "WHERE c.faithful.bec IN :becs " +
            "GROUP BY c.faithful.name, c.year " +
            "ORDER BY c.faithful.name ASC, c.year ASC")
	List<FaithfulYearlyContributionProjection> getYearlyContributionSummaryByBECIn(@Param("becs")List<BEC> becs);
    
    @Query("SELECT c.faithful.name AS name, c.year AS year, SUM(c.amount) AS totalAmount " +
    	       "FROM Contribution c " +
    	       "GROUP BY c.faithful.name, c.year " +
    	       "ORDER BY c.faithful.name ASC, c.year ASC")
	List<FaithfulYearlyContributionProjection> getYearlyContributionSummary();
    
    @Override
    @EntityGraph(attributePaths = {"faithful"})
    @Query("SELECT c FROM Contribution c LEFT JOIN FETCH c.faithful WHERE c.id = :id")
    Optional<Contribution> findById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Contribution c WHERE c.id = :id")
    void deleteById(@Param("id") Long id);

}