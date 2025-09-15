package com.tcc.password_manager.dto;

public class PasswordReferenceClearData {
    
    private String nickname;
    private String blockchainReference1;
    private String blockchainReference2;
    private String fragmentationReference;

    public PasswordReferenceClearData() {
    }

    public PasswordReferenceClearData(String nickname, String blockchainReference1, String blockchainReference2, String fragmentationReference) {
        this.nickname = nickname;
        this.blockchainReference1 = blockchainReference1;
        this.blockchainReference2 = blockchainReference2;
        this.fragmentationReference = fragmentationReference;
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
    
}
