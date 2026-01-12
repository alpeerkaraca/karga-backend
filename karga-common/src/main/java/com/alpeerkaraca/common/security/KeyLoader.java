package com.alpeerkaraca.common.security;

import com.alpeerkaraca.common.exception.KeyLoadingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

@Component
public class KeyLoader {

    public PrivateKey loadPrivateKey() {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("private_key.der")).toURI()));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | URISyntaxException e) {
            throw new KeyLoadingException("Could not load private key", e);
        }
    }

    public PublicKey loadPublicKey() {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("public_key.der")).toURI()));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | URISyntaxException e) {
            throw new KeyLoadingException("Could not load public key", e);
        }
    }

}
