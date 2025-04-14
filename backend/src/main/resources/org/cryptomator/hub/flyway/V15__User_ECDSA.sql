ALTER TABLE "user_details" RENAME COLUMN "publickey" TO "ecdh_publickey";
ALTER TABLE "user_details" RENAME COLUMN "privatekey" TO "privatekeys";
ALTER TABLE "user_details" ADD "ecdsa_publickey" VARCHAR;
ALTER TABLE "device" RENAME COLUMN "user_privatekey" TO "user_privatekeys";