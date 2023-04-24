-- users will generate a new key pair during first login in the browser:
ALTER TABLE "user_details" ADD "publickey" VARCHAR(255); -- pem-encoded SPKI field (RFC 5280, 4.1.2.7)
ALTER TABLE "user_details" ADD "privatekey" VARCHAR(500); -- pem-encoded pkcs8 (RFC 5208), protected by kek generated via PBKDF2
ALTER TABLE "user_details" ADD "salt" VARCHAR(255);
ALTER TABLE "user_details" ADD "iterations" INTEGER;

-- when granting access, the vault key ("masterkey") is encrypted for this user using an ECIES (requires the user to have a key pair)
ALTER TABLE "vault_access" ADD "vault_key_jwe" VARCHAR(2000) UNIQUE;

-- when the user adds a device, the user's private key is encrypted for this device
ALTER TABLE "device" ADD "user_key_jwe" VARCHAR(2000) UNIQUE;

-- do be dropped in a later version:
COMMENT ON TABLE "access_token" IS 'DEPRECATED: This table is kept for compatibility with Cryptomator 1.7.x. No new tokens are issued.';