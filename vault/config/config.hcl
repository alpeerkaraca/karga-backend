storage "file" {
  path    = "/vault/file"
}

listener "tcp" {
  address       = "0.0.0.0:8200"

  tls_disable   = 0
  tls_cert_file = "/vault/config/certs/vault.crt"
  tls_key_file  = "/vault/config/certs/vault.key"
}

default_lease_ttl = "168h"
max_lease_ttl = "720h"
ui = true