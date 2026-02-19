{{- define "cryptomator-hub.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "cryptomator-hub.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.labels" -}}
app.kubernetes.io/name: {{ include "cryptomator-hub.name" . }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "cryptomator-hub.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cryptomator-hub.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "cryptomator-hub.keycloakRelativePath" -}}
{{- $path := default "/kc" .Values.keycloak.config.relativePath -}}
{{- if hasPrefix "/" $path -}}
{{- $path -}}
{{- else -}}
{{- printf "/%s" $path -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubRelativePath" -}}
{{- $path := default "/" .Values.hub.config.publicRootPath -}}
{{- if hasPrefix "/" $path -}}
{{- $path -}}
{{- else -}}
{{- printf "/%s" $path -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakLocalUrl" -}}
{{- if .Values.hub.config.keycloakLocalUrl -}}
{{- .Values.hub.config.keycloakLocalUrl -}}
{{- else if .Values.keycloak.enabled -}}
{{- printf "http://%s:%v%s" (print (include "cryptomator-hub.fullname" .) "-service-kc") .Values.keycloak.service.httpPort (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- else -}}
{{- required "hub.config.keycloakLocalUrl must be set when keycloak.enabled=false" .Values.hub.config.keycloakLocalUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakPublicUrl" -}}
{{- if .Values.hub.config.keycloakPublicUrl -}}
{{- .Values.hub.config.keycloakPublicUrl -}}
{{- else if and .Values.global.host .Values.keycloak.enabled -}}
{{- if .Values.ingress.tls.enabled -}}
{{- printf "https://%s%s" .Values.global.host (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- else -}}
{{- printf "http://%s%s" .Values.global.host (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- end -}}
{{- else if .Values.keycloak.enabled -}}
{{- include "cryptomator-hub.keycloakLocalUrl" . -}}
{{- else -}}
{{- required "hub.config.keycloakPublicUrl must be set when keycloak.enabled=false and global.host is empty" .Values.hub.config.keycloakPublicUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubPublicUrl" -}}
{{- if .Values.ingress.tls.enabled -}}
{{- printf "https://%s%s" .Values.global.host (include "cryptomator-hub.hubRelativePath" .) -}}
{{- else -}}
{{- printf "http://%s%s" .Values.global.host (include "cryptomator-hub.hubRelativePath" .) -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubJdbcUrl" -}}
{{- if .Values.hub.database.jdbcUrl -}}
{{- .Values.hub.database.jdbcUrl -}}
{{- else if .Values.postgres.enabled -}}
{{- printf "jdbc:postgresql://%s:%v/%s" (print (include "cryptomator-hub.fullname" .) "-service-pg") .Values.postgres.service.port .Values.hub.database.name -}}
{{- else -}}
{{- required "hub.database.jdbcUrl must be set when postgres.enabled=false" .Values.hub.database.jdbcUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.oidcAuthServerUrl" -}}
{{- if .Values.hub.oidc.authServerUrl -}}
{{- .Values.hub.oidc.authServerUrl -}}
{{- else if .Values.keycloak.enabled -}}
{{- printf "%s/realms/%s" (include "cryptomator-hub.keycloakLocalUrl" .) .Values.hub.config.keycloakRealm -}}
{{- else -}}
{{- required "hub.oidc.authServerUrl must be set when keycloak.enabled=false" .Values.hub.oidc.authServerUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.oidcTokenIssuer" -}}
{{- if .Values.hub.oidc.tokenIssuer -}}
{{- .Values.hub.oidc.tokenIssuer -}}
{{- else if .Values.keycloak.enabled -}}
{{- printf "%s/realms/%s" (include "cryptomator-hub.keycloakPublicUrl" .) .Values.hub.config.keycloakRealm -}}
{{- else -}}
{{- required "hub.oidc.tokenIssuer must be set when keycloak.enabled=false" .Values.hub.oidc.tokenIssuer -}}
{{- end -}}
{{- end -}}

{{/* 

Auto-generated secrets below:

1. try to use cached value (required so we don't generate a new random value on every template rendering)
2. try to use value from values.yaml (if user has set it, e.g. `hub.secrets.systemClientSecret`)
3. try to look up existing secret in cluster and use it if it exists (this allows users to upgrade from older versions of the chart without losing their secrets)
4. if all else fails, generate a new random value

*/}}

{{- define "cryptomator-hub.resolvedSystemClientSecret" -}}
{{- if hasKey .Values "_resolvedSystemClientSecret" -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- else if .Values.hub.secrets.systemClientSecret -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" .Values.hub.secrets.systemClientSecret -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_system_client_secret") -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" (index $existing.data "hub_system_client_secret" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedHubDbPassword" -}}
{{- if hasKey .Values "_resolvedHubDbPassword" -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- else if .Values.hub.database.password -}}
{{- $_ := set .Values "_resolvedHubDbPassword" .Values.hub.database.password -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_db_password") -}}
{{- $_ := set .Values "_resolvedHubDbPassword" (index $existing.data "hub_db_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedHubDbPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedHubAdminPassword" -}}
{{- if hasKey .Values "_resolvedHubAdminPassword" -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- else if .Values.hub.admin.password -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" .Values.hub.admin.password -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-kc" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub-admin-password") -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" (index $existing.data "hub-admin-password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedKeycloakDbPassword" -}}
{{- if hasKey .Values "_resolvedKeycloakDbPassword" -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- else if .Values.keycloak.database.password -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" .Values.keycloak.database.password -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-kc" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "db-password") -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" (index $existing.data "db-password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedKeycloakAdminPassword" -}}
{{- if hasKey .Values "_resolvedKeycloakAdminPassword" -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- else if .Values.keycloak.admin.password -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" .Values.keycloak.admin.password -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-kc" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "admin-password") -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" (index $existing.data "admin-password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedPostgresAdminPassword" -}}
{{- if hasKey .Values "_resolvedPostgresAdminPassword" -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- else if .Values.postgres.auth.adminPassword -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" .Values.postgres.auth.adminPassword -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-pg" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "admin-password") -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (index $existing.data "admin-password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- end -}}
{{- end -}}