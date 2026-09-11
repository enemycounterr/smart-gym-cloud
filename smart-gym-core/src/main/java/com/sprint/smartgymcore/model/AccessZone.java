package com.sprint.smartgymcore.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "access_zone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String zoneName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "access_zone_clients", joinColumns = @JoinColumn(name = "access_zone_id"))
    @Column(name = "client_ids")
    private Set<Long> clientIds = new HashSet<>();

}
