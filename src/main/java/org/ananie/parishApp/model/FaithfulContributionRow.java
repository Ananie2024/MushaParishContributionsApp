package org.ananie.parishApp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

//@Component
public class FaithfulContributionRow {
    private String name;
    private Map<Integer, BigDecimal> yearlyContributions = new HashMap<>();
	private Object totalContribution;

    public FaithfulContributionRow(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, BigDecimal> getYearlyContributions() {
        return yearlyContributions;
    }

	public void setTotalContribution(BigDecimal total) {
		  if (total != null) {
		        this.totalContribution = total.setScale(2, RoundingMode.HALF_UP);
		    } else {
		        this.totalContribution = new BigDecimal("0000.00");
		    }
	}

	public BigDecimal getTotalContribution() {
		    return totalContribution != null
		        ? ((BigDecimal) totalContribution).setScale(2, RoundingMode.HALF_UP)
		        : new BigDecimal("0000.00");
		}

	}

