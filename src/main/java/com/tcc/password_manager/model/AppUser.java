package com.tcc.password_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "symmetric_key")
    private String symmetricKey;

    @Column(name = "sm_key_hash")
    private String smKeyHash;

    public AppUser() {
    }

    public AppUser(Long id, String mfaSecret, String symmetricKey, String smKeyHash) {
        this.id = id;
        this.mfaSecret = mfaSecret;
        this.symmetricKey = symmetricKey;
        this.smKeyHash = smKeyHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public String getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(String symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public String getSmKeyHash() {
        return smKeyHash;
    }

    public void setSmKeyHash(String smKeyHash) {
        this.smKeyHash = smKeyHash;
    }

}
