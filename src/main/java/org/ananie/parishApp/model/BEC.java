package org.ananie.parishApp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "impuza", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "sub_parish_id"})})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"faithfuls", "subParish"}) // Exclude faithfuls and subParish to prevent recursion
public class BEC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_parish_id", nullable = false)
    private SubParish subParish;

    @OneToMany(mappedBy = "bec", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Faithful> faithfuls = new HashSet<>();

    public BEC(String name, SubParish subParish) {
        this.name = name;
        this.subParish = subParish;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BEC bec = (BEC) o;
        return id != null && Objects.equals(id, bec.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}