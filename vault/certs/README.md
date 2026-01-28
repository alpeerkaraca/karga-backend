# How to create certificates for Vault

This document describes how to create self-signed SSL certificates for use with HashiCorp Vault in a development
environment.

## Prerequisites

- OpenSSL installed on your machine.
- Basic knowledge of command line operations.

## Steps to Create Certificates

**Create a Private Key**

- Open your terminal and run the following command to create a private key:

```bash
openssl req -x509 -newkey rsa:4096 -days 365 -nodes \
  -keyout vault/certs/vault.key \
  -out vault/certs/vault.crt \
  -subj "//CN=vault-server" \
  -addext "subjectAltName = DNS:localhost,DNS:vault-server,IP:127.0.0.1,IP:0.0.0.0"
```

This command generates a 4096-bit RSA private key and a self-signed certificate valid for 365 days.
The certificate includes Subject Alternative Names (SANs) for localhost and vault-server.
By default, Vault configured to use TLS. You can check the config files in the `vault/config` directory.

**Import the Certificate into Java Keystore**
<p>To make sure Java applications trust this certificate, you may need to import it into your Java Keystore (JKS):</p>

```bash
keytool -import -alias vault -keystore vault-truststore.jks -file vault.crt
```

You will be prompted to set a password for the keystore.
Remember this password, as it will be needed when configuring your Java applications to use this truststore.
Also answer "yes" when prompted to trust the certificate.

**Configure Spring Cloud Vault to Use the Certificate**

- Copy created `truststore.jks` to your resources folder. In your Spring Boot application's `application.properties` or
  `application.yml`,
- configure the Vault properties to use the created certificate:

```properties
spring.cloud.vault.ssl.trust-store=classpath:vault-truststore.jks
spring.cloud.vault.ssl.trust-store-password=your_keystore_password
```
