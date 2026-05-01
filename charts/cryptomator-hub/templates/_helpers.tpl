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
app.kubernetes.io/version: {{ $.Chart.AppVersion | quote }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "cryptomator-hub.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cryptomator-hub.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/* 

Compute relative URLs for hub and keycloak based on their public URLs and whether ingress is enabled.
This allows users to set the public URLs to the actual external URLs of the services, and we can derive the relative paths required for configuring the services to work correctly both with and without ingress.

*/}}

{{- define "cryptomator-hub.hubRelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" (required "urls.hub.public must be set" .Values.urls.hub.public) "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakRelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" (required "urls.kc.public must be set" .Values.urls.kc.public) "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakLocalUrl" -}}
{{- if .Values.urls.kc.clusterInternal -}}
{{- trimSuffix "/" .Values.urls.kc.clusterInternal -}}
{{- else if .Values.keycloak.enabled -}}
{{- printf "http://%s:%v%s" (print (include "cryptomator-hub.fullname" .) "-service-kc") .Values.keycloak.service.httpPort (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- else -}}
{{/* if keycloak isn't part of the deployment, use public url: */}}
{{- trimSuffix "/" (required "urls.kc.public must be set" .Values.urls.kc.public) -}}
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
{{- if .Values.urls.kc.authServerUrl -}}
{{- .Values.urls.kc.authServerUrl -}}
{{- else -}}
{{- printf "%s/realms/%s" (trimSuffix "/" (include "cryptomator-hub.keycloakLocalUrl" .)) .Values.hub.config.keycloakRealm -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.oidcTokenIssuer" -}}
{{- if .Values.urls.kc.tokenIssuer -}}
{{- .Values.urls.kc.tokenIssuer -}}
{{- else -}}
{{- printf "%s/realms/%s" (trimSuffix "/" .Values.urls.kc.public) .Values.hub.config.keycloakRealm -}}
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
{{- $secretName := print (include "cryptomator-hub.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_admin_password") -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" (index $existing.data "hub_admin_password" | b64dec) -}}
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
{{- if and $existing (hasKey $existing.data "kc_db_password") -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" (index $existing.data "kc_db_password" | b64dec) -}}
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
{{- if and $existing (hasKey $existing.data "kc_admin_password") -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" (index $existing.data "kc_admin_password" | b64dec) -}}
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
{{- if and $existing (hasKey $existing.data "pg_admin_password") -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (index $existing.data "pg_admin_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- end -}}
{{- end -}}
