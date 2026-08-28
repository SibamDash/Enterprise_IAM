package com.enterprise.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
