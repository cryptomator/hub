-- users will generate a new key pair during first login in the browser:
ALTER TABLE "user_details" ADD "publickey" VARCHAR(255); -- pem-encoded SPKI field (RFC 5280, 4.1.2.7)
ALTER TABLE "user_details" ADD "privatekey" VARCHAR(500); -- pem-encoded pkcs8 (RFC 5208), protected by kek generated via PBKDF2
ALTER TABLE "user_details" ADD "salt" VARCHAR(255);
ALTER TABLE "user_details" ADD "iterations" INTEGER DEFAULT 0;

-- keep existing device-based access tokens for continuous unlock from old clients.
ALTER TABLE "device" RENAME TO "device_legacy";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_PK" TO "DEVICE_LEGACY_PK";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_FK_USER" TO "DEVICE_LEGACY_FK_USER";
ALTER TABLE "device_legacy" RENAME CONSTRAINT "DEVICE_UNIQUE_NAME_PER_OWNER" TO "DEVICE_LEGACY_UNIQUE_NAME_PER_OWNER";
ALTER TABLE "access_token" RENAME TO "access_token_legacy";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_PK" TO "ACCESS_LEGACY_PK";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_DEVICE" TO "ACCESS_LEGACY_FK_DEVICE";
ALTER TABLE "access_token_legacy" RENAME CONSTRAINT "ACCESS_FK_VAULT" TO "ACCESS_LEGACY_FK_VAULT";

-- start from scratch with new device table (strictly require a user_key to be encrypted for this device):
CREATE TABLE "device"
(
	"id"            VARCHAR(64) COLLATE "C" NOT NULL,
	"owner_id"      VARCHAR(255) COLLATE "C" NOT NULL,
	"name"          VARCHAR(255) NOT NULL,
	"publickey"     VARCHAR(255) NOT NULL UNIQUE,
	"user_key_jwe"  VARCHAR(2000) NOT NULL UNIQUE,
	"creation_time" TIMESTAMP NOT NULL,
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_FK_USER" FOREIGN KEY ("owner_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEVICE_UNIQUE_NAME_PER_OWNER" UNIQUE ("owner_id", "name")
);

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