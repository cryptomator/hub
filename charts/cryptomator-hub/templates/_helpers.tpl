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

{{- define "cryptomator-hub.resolvedSystemClientSecret" -}}
{{- if not (hasKey .Values "_resolvedSystemClientSecret") -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" (default (randAlphaNum 32) .Values.hub.secrets.systemClientSecret) -}}
{{- end -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- end -}}

{{- define "cryptomator-hub.realmSystemClientSecret" -}}
{{- include "cryptomator-hub.resolvedSystemClientSecret" . -}}
{{- end -}}
