package org.ananie.parishApp.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "amaturo")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"faithful"}) // Exclude parent object to prevent recursion
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faithful_id", nullable = false)
    private Faithful faithful;

    @Column(name = "umwaka", nullable = false)
    private Integer year;

    @Column(name = "amafranga", nullable = false, precision = 10 /*,scale = 2*/)
    private BigDecimal amount;

    @Column(name = "itariki", nullable = false)
    private LocalDate date;

    @Column(name = "notes", length = 500)
    private String notes;

    public Contribution(Faithful faithful, Integer year, BigDecimal amount, LocalDate date, String notes) {
        this.faithful = faithful;
        this.year = year;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contribution that = (Contribution) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}