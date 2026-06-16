# Vault ACL-политика payadmin-bff (применяется downstream-пайплайном tkbpay/vault-config).
# KV v1 mount `pay`, раскладка pay/{env}/{app}-{secret} (без сегмента data/).
# caller межсервисных вызовов. Ключ-идентичность pay/test/payadmin-bff-internal-key
# (его bff шлёт во все upstream'ы как X-Internal-Admin-Key) покрыт glob'ом ниже.
path "pay/test/payadmin-bff-*" {
  capabilities = ["read"]
}
