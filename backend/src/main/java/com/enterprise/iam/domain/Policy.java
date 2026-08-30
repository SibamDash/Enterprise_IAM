package com.enterprise.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Getter
@Setter
public class Policy extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String effect; // ALLOW or DENY

    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> actions = new ArrayList<>();

    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> resources = new ArrayList<>();

    private String conditions;

    @Column(nullable = false)
    private int priority = 0;
}
