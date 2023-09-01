-- users will generate a new key pair during first login in the browser:
ALTER TABLE "user_details" ADD "publickey" VARCHAR(255); -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
ALTER TABLE "user_details" ADD "privatekey" VARCHAR(2000); -- private key, encrypted using setup code (JWE PBES2)
ALTER TABLE "user_details" ADD "setupcode" VARCHAR(2000); -- setup code, encrypted using user's public key (JWE ECDH-ES)

-- keep existing device-based access tokens for continuous unlock from old clients.
ALTER TABLE "access_token" RENAME TO "access_token_legacy";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_PK" TO "ACCESS_LEGACY_PK";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_DEVICE" TO "ACCESS_LEGACY_FK_DEVICE";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_VAULT" TO "ACCESS_LEGACY_FK_VAULT";
ALTER TABLE "device" RENAME TO "device_legacy";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_PK" TO "DEVICE_LEGACY_PK";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_FK_USER" TO "DEVICE_LEGACY_FK_USER";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_UNIQUE_NAME_PER_OWNER" TO "DEVICE_LEGACY_UNIQUE_NAME_PER_OWNER";
COMMENT ON COLUMN "device_legacy"."publickey" IS 'Note: This contains base64url-encoded data for historic reasons.';

-- new device table with non-null user_privatekey:
CREATE TABLE "device"
(
	"id"              VARCHAR(255) NOT NULL,
	"owner_id"        VARCHAR(255) NOT NULL,
	"name"            VARCHAR(255) NOT NULL,
	"type"            VARCHAR(50) NOT NULL DEFAULT 'DESKTOP',
	"publickey"       VARCHAR(255) NOT NULL,
	"user_privatekey" VARCHAR(2000) NOT NULL UNIQUE, -- private key, encrypted using device's public key (JWE ECDH-ES)
	"creation_time"   TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_FK_USER" FOREIGN KEY ("owner_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEVICE_UNIQUE_NAME_PER_OWNER" UNIQUE ("owner_id", "name")
);

-- new access tokens will be issued for users (not devices):
CREATE TABLE "access_token"
(
	"user_id"          VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"         UUID NOT NULL,
	"vault_masterkey"  VARCHAR(2000) NOT NULL UNIQUE, -- private key, encrypted using user's public key (JWE ECDH-ES)
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_USER" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);

ALTER TABLE "vault_access" ADD "role" VARCHAR(50) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE "audit_event_vault_member_add" ADD "role" VARCHAR(50) NOT NULL DEFAULT 'MEMBER';
CREATE TABLE "audit_event_vault_member_update"
(
	"id"           BIGINT NOT NULL,
	"updated_by"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"role"         VARCHAR(50) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_UPDATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

-- @formatter:off
CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id", "role") AS
	SELECT "va"."vault_id", "va"."authority_id", "va"."role" FROM "vault_access" "va"
	UNION
	SELECT "va"."vault_id", "gm"."member_id", "va"."role" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on

-- deprecate vault admin password
ALTER TABLE "vault" ALTER COLUMN "salt" DROP NOT NULL;
ALTER TABLE "vault" ALTER COLUMN "iterations" DROP NOT NULL;
ALTER TABLE "vault" ALTER COLUMN "masterkey" DROP NOT NULL;
ALTER TABLE "vault" ALTER COLUMN "auth_pubkey" DROP NOT NULL;
ALTER TABLE "vault" ALTER COLUMN "auth_prvkey" DROP NOT NULL;
