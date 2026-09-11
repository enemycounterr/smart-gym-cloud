package com.sprint.smartgymcore.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccessDirection direction;

    @Column(name = "time_stamp", nullable = false)
    private Instant timeStamp;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_zone_id", nullable = false)
    private AccessZone accessZone;

    public AccessLog(AccessDirection direction, Long clientId, AccessZone accessZone) {
        this.direction = direction;
        this.clientId = clientId;
        this.accessZone = accessZone;
        this.timeStamp = Instant.now();
    }

}
