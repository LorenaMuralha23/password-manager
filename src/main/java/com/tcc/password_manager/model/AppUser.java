package com.tcc.password_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_data", columnDefinition = "TEXT")
    private String encryptedData;

    @Column(name = "umk_wrapped", columnDefinition = "TEXT", nullable = false)
    private String umkWrapped; // JSON do EncryptedPayload da UMK cifrada com a senha

    @Column(name = "umk_hash", length = 64, nullable = false)
    private String umkHash;    // SHA-256(UMK) em Base64 (ou hex)

    public AppUser() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getUmkWrapped() {
        return umkWrapped;
    }

    public void setUmkWrapped(String umkWrapped) {
        this.umkWrapped = umkWrapped;
    }

    public String getUmkHash() {
        return umkHash;
    }

    public void setUmkHash(String umkHash) {
        this.umkHash = umkHash;
    }
    
}
