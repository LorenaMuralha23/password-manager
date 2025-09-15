package com.tcc.password_manager.dto;

public class AppUserClearData {
    
    private String mfaSecret;
    private String umkWrapped; // chave simétrica do usuário cifrada com a senha
    private String umkHash;    // hash da chave simétrica em claro

    public AppUserClearData() {
    }

    public AppUserClearData(String mfaSecret, String umkWrapped, String umkHash) {
        this.mfaSecret = mfaSecret;
        this.umkWrapped = umkWrapped;
        this.umkHash = umkHash;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
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
