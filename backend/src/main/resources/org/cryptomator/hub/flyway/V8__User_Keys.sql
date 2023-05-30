-- users will generate a new key pair during first login in the browser:
ALTER TABLE "user_details" ADD "publickey" VARCHAR(255); -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
ALTER TABLE "user_details" ADD "recovery_jwe" VARCHAR(2000); -- recovery code, encrypted using user's public key (JWE format)
ALTER TABLE "user_details" ADD "recovery_pbkdf2" VARCHAR(500); -- private key, encrypted using the recovery code (base64-encoded 12 byte IV + GCM-encrypted PKCS8 DER (RFC 5208))
ALTER TABLE "user_details" ADD "recovery_salt" VARCHAR(255); -- base64-encoded
ALTER TABLE "user_details" ADD "recovery_iterations" INTEGER DEFAULT 0;

-- keep existing device-based access tokens for continuous unlock from old clients.
ALTER TABLE "access_token" RENAME TO "access_token_legacy";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_PK" TO "ACCESS_LEGACY_PK";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_DEVICE" TO "ACCESS_LEGACY_FK_DEVICE";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_VAULT" TO "ACCESS_LEGACY_FK_VAULT";

-- as soon as a device gets verified by its owner, the owner's private key will be encrypted for this device:
ALTER TABLE "device" ADD "user_key_jwe" VARCHAR(2000) UNIQUE;
COMMENT ON COLUMN "device"."publickey" IS 'Note: This contains base64url-encoded data for historic reasons.';
ALTER TABLE "device" ADD "type" VARCHAR(255) NOT NULL;
ALTER TABLE "device" ADD "last_seen_time" TIMESTAMP WITH TIME ZONE NOT NULL;

-- new access tokens will be issued for users (not devices):
CREATE TABLE "access_token"
(
	"user_id"        VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"       UUID NOT NULL,
	"vault_key_jwe"  VARCHAR(2000) NOT NULL UNIQUE,
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_USER" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);