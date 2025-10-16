package com.jdahms.semantic_search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "industry_codes")
public class IndustryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embedding;    public IndustryCode() {}

    public IndustryCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public IndustryCode setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public IndustryCode setCode(String code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public IndustryCode setDescription(String description) {
        this.description = description;
        return this;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public IndustryCode setEmbedding(float[] embedding) {
        this.embedding = embedding;
        return this;
    }
}
