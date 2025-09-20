package org.ananie.mushaParish.dao;

import java.math.BigDecimal;

public interface FaithfulYearlyContributionProjection {
    String getName();
    Integer getYear();
    BigDecimal getTotalAmount();
}
