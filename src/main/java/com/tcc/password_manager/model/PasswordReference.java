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

    private String nickname;

    @Column(name = "blockchain_reference_1")
    private String blockchainReference1;

    @Column(name = "blockchain_reference_2")
    private String blockchainReference2;

    @Column(name = "fragmentation_reference")
    private String fragmentationReference;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    public PasswordReference() {
    }

    public PasswordReference(Long id, String nickname, String blockchain_reference_1, String blockchain_reference_2, String fragmentation_reference) {
        this.id = id;
        this.nickname = nickname;
        this.blockchainReference1 = blockchain_reference_1;
        this.blockchainReference2 = blockchain_reference_2;
        this.fragmentationReference = fragmentation_reference;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBlockchainReference1() {
        return blockchainReference1;
    }

    public void setBlockchainReference1(String blockchainReference1) {
        this.blockchainReference1 = blockchainReference1;
    }

    public String getBlockchainReference2() {
        return blockchainReference2;
    }

    public void setBlockchainReference2(String blockchainReference2) {
        this.blockchainReference2 = blockchainReference2;
    }

    public String getFragmentationReference() {
        return fragmentationReference;
    }

    public void setFragmentationReference(String fragmentationReference) {
        this.fragmentationReference = fragmentationReference;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }
    
}
