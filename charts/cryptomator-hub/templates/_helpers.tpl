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

{{- define "cryptomator-hub.hubSecretName" -}}
{{- if .Values.hub.secrets.existingSecret -}}
{{- .Values.hub.secrets.existingSecret -}}
{{- else if not .Values.hub.secrets.create -}}
{{- required "hub.secrets.existingSecret must be set when hub.secrets.create=false" .Values.hub.secrets.existingSecret -}}
{{- else -}}
{{- include "cryptomator-hub.fullname" . -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.postgresSecretName" -}}
{{- printf "%s-postgres" (include "cryptomator-hub.fullname" .) -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakSecretName" -}}
{{- printf "%s-keycloak" (include "cryptomator-hub.fullname" .) -}}
{{- end -}}

{{- define "cryptomator-hub.postgresServiceName" -}}
{{- printf "%s-postgres" (include "cryptomator-hub.fullname" .) -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakServiceName" -}}
{{- printf "%s-keycloak" (include "cryptomator-hub.fullname" .) -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakRelativePath" -}}
{{- $path := default "/kc" .Values.keycloak.config.relativePath -}}
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
{{- printf "http://%s:%v%s" (include "cryptomator-hub.keycloakServiceName" .) .Values.keycloak.service.httpPort (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- else -}}
{{- required "hub.config.keycloakLocalUrl must be set when keycloak.enabled=false" .Values.hub.config.keycloakLocalUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.keycloakPublicUrl" -}}
{{- if .Values.hub.config.keycloakPublicUrl -}}
{{- .Values.hub.config.keycloakPublicUrl -}}
{{- else if and .Values.global.host .Values.keycloak.enabled -}}
{{- printf "https://%s%s" .Values.global.host (include "cryptomator-hub.keycloakRelativePath" .) -}}
{{- else if .Values.keycloak.enabled -}}
{{- include "cryptomator-hub.keycloakLocalUrl" . -}}
{{- else -}}
{{- required "hub.config.keycloakPublicUrl must be set when keycloak.enabled=false and global.host is empty" .Values.hub.config.keycloakPublicUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubJdbcUrl" -}}
{{- if .Values.hub.database.jdbcUrl -}}
{{- .Values.hub.database.jdbcUrl -}}
{{- else if .Values.postgres.enabled -}}
{{- printf "jdbc:postgresql://%s:%v/%s" (include "cryptomator-hub.postgresServiceName" .) .Values.postgres.service.port .Values.postgres.auth.hubDatabase -}}
{{- else -}}
{{- required "hub.database.jdbcUrl must be set when postgres.enabled=false" .Values.hub.database.jdbcUrl -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubDbUsername" -}}
{{- if .Values.hub.database.username -}}
{{- .Values.hub.database.username -}}
{{- else if .Values.postgres.enabled -}}
{{- .Values.postgres.auth.hubUsername -}}
{{- else -}}
{{- required "hub.database.username must be set when postgres.enabled=false" .Values.hub.database.username -}}
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

{{- define "cryptomator-hub.realmPublicHost" -}}
{{- if .Values.keycloak.realmBootstrap.publicHost -}}
{{- .Values.keycloak.realmBootstrap.publicHost -}}
{{- else if .Values.global.host -}}
{{- .Values.global.host -}}
{{- else -}}
domain.tld
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.resolvedSystemClientSecret" -}}
{{- if hasKey .Values "_resolvedSystemClientSecret" -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- else -}}
{{- $resolved := "" -}}
{{- if .Values.hub.secrets.data.systemClientSecret -}}
{{- $resolved = .Values.hub.secrets.data.systemClientSecret -}}
{{- else if .Values.keycloak.realmBootstrap.systemClientSecret -}}
{{- $resolved = .Values.keycloak.realmBootstrap.systemClientSecret -}}
{{- else -}}
{{- $existing := (lookup "v1" "Secret" .Release.Namespace (include "cryptomator-hub.hubSecretName" .)) -}}
{{- if and $existing (hasKey $existing.data .Values.hub.secrets.keys.systemClientSecret) -}}
{{- $resolved = (index $existing.data .Values.hub.secrets.keys.systemClientSecret | b64dec) -}}
{{- else if and .Values.hub.secrets.create (not .Values.hub.secrets.existingSecret) -}}
{{- $resolved = randAlphaNum 32 -}}
{{- else -}}
{{- $resolved = required "Provide hub.secrets.data.systemClientSecret (or keycloak.realmBootstrap.systemClientSecret) when using an existing Hub secret." .Values.hub.secrets.data.systemClientSecret -}}
{{- end -}}
{{- end -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" $resolved -}}
{{- $resolved -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.realmSystemClientSecret" -}}
{{- include "cryptomator-hub.resolvedSystemClientSecret" . -}}
{{- end -}}

{{- define "cryptomator-hub.hubDbPasswordSecretName" -}}
{{- if .Values.postgres.enabled -}}
{{- include "cryptomator-hub.postgresSecretName" . -}}
{{- else -}}
{{- include "cryptomator-hub.hubSecretName" . -}}
{{- end -}}
{{- end -}}

{{- define "cryptomator-hub.hubDbPasswordSecretKey" -}}
{{- if .Values.postgres.enabled -}}
hub-password
{{- else -}}
{{- .Values.hub.secrets.keys.dbPassword -}}
{{- end -}}
{{- end -}}
