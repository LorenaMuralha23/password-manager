package com.tcc.password_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "passwords_ref")
public class PasswordReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_data", columnDefinition = "CLOB")
    private String encryptedData;

    // Relacionamento ainda precisa existir em claro para integridade do banco
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    public PasswordReference() {
    }

    public PasswordReference(Long id, String encryptedData, AppUser user) {
        this.id = id;
        this.encryptedData = encryptedData;
        this.user = user;
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

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }
    
}
