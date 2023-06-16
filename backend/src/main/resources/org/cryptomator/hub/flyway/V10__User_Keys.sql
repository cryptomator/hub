-- users will generate a new key pair during first login in the browser:
ALTER TABLE "user_details" ADD "publickey" VARCHAR(255); -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
ALTER TABLE "user_details" ADD "privatekey" VARCHAR(2000); -- private key, encrypted using setup code (JWE PBES2)
ALTER TABLE "user_details" ADD "setupcode" VARCHAR(2000); -- setup code, encrypted using user's public key (JWE ECDH-ES)

-- keep existing device-based access tokens for continuous unlock from old clients.
ALTER TABLE "access_token" RENAME TO "access_token_legacy";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_PK" TO "ACCESS_LEGACY_PK";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_DEVICE" TO "ACCESS_LEGACY_FK_DEVICE";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_VAULT" TO "ACCESS_LEGACY_FK_VAULT";

-- as soon as a device gets verified by its owner, the owner's private key will be encrypted for this device:
ALTER TABLE "device" ADD "user_key" VARCHAR(2000) UNIQUE; -- private key, encrypted using device's public key (JWE ECDH-ES)
ALTER TABLE "device" ADD "last_seen_time" TIMESTAMP WITH TIME ZONE NOT NULL;
COMMENT ON COLUMN "device"."publickey" IS 'Note: This contains base64url-encoded data for historic reasons.';

-- new access tokens will be issued for users (not devices):
CREATE TABLE "access_token"
(
	"user_id"        VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"       UUID NOT NULL,
	"vault_key"  VARCHAR(2000) NOT NULL UNIQUE, -- private key, encrypted using user's public key (JWE ECDH-ES)
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_USER" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);

ALTER TABLE "vault_access" ADD "role" VARCHAR(50) NOT NULL DEFAULT 'MEMBER';

-- @formatter:off
CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id", "role") AS
	SELECT * FROM "vault_access"
	UNION
	SELECT "va"."vault_id", "gm"."member_id", "va"."role" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on