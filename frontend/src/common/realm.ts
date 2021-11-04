export default function createRealmJson(hubUrl: string, adminUser: string, adminPass: string): string {
    return `{
    "id" : "cryptomator",
    "realm" : "cryptomator",
    "displayName" : "Cryptomator",
    "enabled" : true,
    "sslRequired" : "external",
    "roles" : {
        "realm" : [{
                "name": "user",
                "description": "User",
                "composite" : true,
                "composites" : {
                    "realm" : [ "default-roles-cryptomator" ]
                }
            },
            {
                "name": "admin",
                "description": "Administrator",
                "composite" : true,
                "composites" : {
                    "realm" : [ "user" ],
                    "client" : {
                        "cryptomator-hub" : [ "vault-owner" ]
                    }
                }
            }],
        "client" : {
            "cryptomator-hub" : [{
                "name" : "vault-owner",
                "description": "Vault Owner"
            }]
        }
    },
    "users" : [ {
        "username" : "${adminUser}",
        "enabled" : true,
        "credentials" : [ {
            "type" : "password",
            "value" : "${adminPass}"
        } ],
        "realmRoles" : [ "admin" ]
    } ],
    "scopeMappings" : [ {
        "client" : "cryptomator-hub",
        "roles" : [ "user", "admin" ]
    } ],
    "clientScopeMappings" : {
        "account" : [ {
            "client" : "cryptomator-hub",
            "roles" : [ "vault-owner" ]
        } ]
    },
    "clients" : [ {
        "clientId" : "cryptomator-hub",
        "serviceAccountsEnabled" : false,
        "publicClient" : true,
        "name" : "Cryptomator Hub",
        "enabled" : true,
        "redirectUris" : [ "${hubUrl}/*", "http://127.0.0.1/*" ],
        "webOrigins" : [ "+" ],
        "bearerOnly" : false,
        "frontchannelLogout" : false,
        "protocol" : "openid-connect",
        "attributes": {
            "pkce.code.challenge.method": "S256"
        }
    }]
}`;
}
