package org.ananie.mushaParish.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet; // Use HashSet for collections initialized in the constructor
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "santarali")
@Getter // Generates all getters
@Setter // Generates all setters
@NoArgsConstructor // Generates a no-argument constructor
@ToString(exclude = {"becs"}) // Generates toString, exclude 'becs' to prevent recursion
public class SubParish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "subParish", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<BEC> becs = new HashSet<>(); // Initialize collection to prevent NullPointerException

    // Custom constructor if needed, Lombok's @NoArgsConstructor handles the default one
    public SubParish(String name) {
        this.name = name;
    }

    // Lombok's @EqualsAndHashCode(of = "id") is recommended for entities based on ID
    // However, it's generally safer to manually override equals/hashCode for entities
    // to ensure proper proxy handling and persistence context behavior if you ever face issues.
    // For simplicity, we can use Lombok's callSuper = false for now if you prefer.
    // If you explicitly generate equals/hashCode, Lombok will respect it.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubParish subParish = (SubParish) o;
        return id != null && Objects.equals(id, subParish.id); // Check for null id for unsaved entities
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Or a constant if ID can be null for unsaved entities
                                      // If ID is always non-null after creation, Objects.hash(id) is fine.
                                      // A common pattern is to use a constant for unsaved entities and id.hashCode() for saved ones.
                                      // For simplicity with JPA, using a constant or getClass().hashCode() is often safer until saved.
    }
}