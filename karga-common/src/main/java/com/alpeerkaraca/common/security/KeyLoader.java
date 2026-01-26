package com.alpeerkaraca.common.security;

import com.alpeerkaraca.common.exception.KeyLoadingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Component
public class KeyLoader {
    @Value("${app.security.private-key-location:classpath:private_key.der}")
    private Resource privateKeyResource;

    @Value("${app.security.public-key-location:classpath:public_key.der}")
    private Resource publicKeyResource;

    public PrivateKey loadPrivateKey() {
        try {
            byte[] keyBytes = privateKeyResource.getInputStream().readAllBytes();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyLoadingException("Could not load private key", e);
        }
    }

    public PublicKey loadPublicKey() {
        try {
            byte[] keyBytes = publicKeyResource.getInputStream().readAllBytes();

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyLoadingException("Could not load public key", e);
        }
    }
}