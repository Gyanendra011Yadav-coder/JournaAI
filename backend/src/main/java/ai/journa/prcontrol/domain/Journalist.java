package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "journalists")
public class Journalist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String outlet;

    private String location;

    private String email;

    private String phone;

    @Column(nullable = false)
    private String sourceProvider;

    @Column(nullable = false)
    private String providerReferenceId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany
    @JoinTable(
            name = "journalist_tags",
            joinColumns = @JoinColumn(name = "journalist_id"),
            inverseJoinColumns = @JoinColumn(name = "beat_id")
    )
    private Set<Beat> beats = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutlet() {
        return outlet;
    }

    public void setOutlet(String outlet) {
        this.outlet = outlet;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getProviderReferenceId() {
        return providerReferenceId;
    }

    public void setProviderReferenceId(String providerReferenceId) {
        this.providerReferenceId = providerReferenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Beat> getBeats() {
        return beats;
    }

    public void setBeats(Set<Beat> beats) {
        this.beats = beats;
    }
}
