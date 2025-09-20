package org.ananie.mushaParish.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

@Entity
@Table(name = "abakristu")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"contributions", "bec"}) // Exclude collections and parent object to prevent recursion
public class Faithful {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amazina", nullable = false)
    private String name;

    @Column(name = "Telefone")
    private String contactNumber;

    @Column(name = "Aderesi")
    private String address;

    @Column(name = "umwaka_yabatirijwemo")
    private String baptismYear;

    @Column(name = "Icyo_akora")
    private String occupation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bec_id", nullable = false)
    private BEC bec;

    @OneToMany(mappedBy = "faithful", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Contribution> contributions = new HashSet<>();

    public Faithful(String name, String contactNumber, String address, String baptismYear, String occupation, BEC bec) {
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
        this.baptismYear = baptismYear;
        this.occupation = occupation;
        this.bec = bec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faithful faithful = (Faithful) o;
        return id != null && Objects.equals(id, faithful.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}