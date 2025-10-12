package org.ananie.parishApp.dao;

import java.math.BigDecimal;

public interface FaithfulYearlyContributionProjection {
    String getName();
    Integer getYear();
    BigDecimal getTotalAmount();
}
