-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE "settings"
(
	"id"     INT4         NOT NULL,
	"hub_id" VARCHAR(255) NOT NULL,
	"license_key"  VARCHAR,
	CONSTRAINT "SETTINGS_PK" PRIMARY KEY ("id")
);

INSERT INTO "settings" ("id", "hub_id") VALUES (0, gen_random_uuid());

CREATE TABLE "authority"
(
	"id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"type" VARCHAR(5)   NOT NULL,
	"name" VARCHAR      NOT NULL,
	CONSTRAINT "AUTHORITY_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUTHORITY_CHK_TYPE" CHECK ("type" = 'USER' OR "type" = 'GROUP')
);

CREATE TABLE "group_membership"
(
	"group_id"  VARCHAR(255) COLLATE "C" NOT NULL,
	"member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "GROUP_MEMBERSHIP_PK" PRIMARY KEY ("group_id", "member_id"),
	CONSTRAINT "GROUP_MEMBERSHIP_FK_GROUP" FOREIGN KEY ("group_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "GROUP_MEMBERSHIP_FK_MEMBER" FOREIGN KEY ("member_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "GROUP_MEMBERSHIP_CHK_NOTSAME" CHECK ("group_id" <> "member_id")
);

-- @formatter:off
CREATE VIEW "effective_group_membership" ("group_id", "member_id", "path") AS
WITH RECURSIVE "members" ("root", "member_id", "depth", "path") AS (
	SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
	    FROM "group_membership"
	UNION
	SELECT "parent"."root", "child"."member_id", "parent"."depth" + 1, "parent"."path" || '/' || "child"."member_id"
	    FROM "group_membership" "child"
		INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id"
		WHERE "parent"."depth" < 10
) SELECT "root", "member_id", "path" FROM "members";
-- @formatter:on

CREATE TABLE "user_details"
(
	"id"          VARCHAR(255) COLLATE "C" NOT NULL,
	"picture_url" VARCHAR,
	"email"       VARCHAR,
	"publickey"   VARCHAR, -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
	"privatekey"  VARCHAR, -- private key, encrypted using setup code (JWE PBES2)
	"setupcode"   VARCHAR, -- setup code, encrypted using user's public key (JWE ECDH-ES)
	CONSTRAINT "USER_DETAIL_PK" PRIMARY KEY ("id"),
	CONSTRAINT "USER_DETAIL_FK_USER" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE TABLE "group_details"
(
	"id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "GROUP_DETAIL_PK" PRIMARY KEY ("id"),
	CONSTRAINT "GROUP_DETAIL_FK_GROUP" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE TABLE "vault"
(
	"id"            UUID NOT NULL,
	"name"          VARCHAR NOT NULL,
	"description"   VARCHAR,
	"creation_time" TIMESTAMP WITH TIME ZONE NOT NULL,
	"archived"      BOOLEAN NOT NULL DEFAULT false,
	"salt"          VARCHAR(255), -- deprecated ("vault admin password")
	"iterations"    INTEGER,      -- deprecated ("vault admin password")
	"masterkey"     VARCHAR(255), -- deprecated ("vault admin password")
	"auth_pubkey"   VARCHAR,      -- deprecated ("vault admin password")
	"auth_prvkey"   VARCHAR,      -- deprecated ("vault admin password")
	CONSTRAINT "VAULT_PK" PRIMARY KEY ("id")
);

CREATE TABLE "vault_access"
(
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"role" VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
	CONSTRAINT "VAULT_ACCESS_PK" PRIMARY KEY ("vault_id", "authority_id"),
	CONSTRAINT "VAULT_ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "VAULT_ACCESS_FK_AUTHORITY" FOREIGN KEY ("authority_id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

-- @formatter:off
CREATE VIEW "effective_vault_access" ("vault_id", "authority_id", "role") AS
	SELECT "va"."vault_id", "va"."authority_id", "va"."role" FROM "vault_access" "va"
	UNION
	SELECT "va"."vault_id", "gm"."member_id", "va"."role" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on

CREATE TABLE "device"
(
	"id"              VARCHAR(255) NOT NULL,
	"owner_id"        VARCHAR(255) NOT NULL,
	"name"            VARCHAR NOT NULL,
	"type"            VARCHAR(50) NOT NULL DEFAULT 'DESKTOP',
	"publickey"       VARCHAR NOT NULL,        -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
	"user_privatekey" VARCHAR NOT NULL UNIQUE, -- private key, encrypted using device's public key (JWE ECDH-ES)
	"creation_time"   TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_FK_USER" FOREIGN KEY ("owner_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "access_token"
(
	"user_id"          VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"         UUID NOT NULL,
	"vault_masterkey"  VARCHAR NOT NULL UNIQUE, -- private key, encrypted using user's public key (JWE ECDH-ES)
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_USER" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);

-- ------------- --
-- LEGACY TABLES --
-- ------------- --
CREATE TABLE "device_legacy"
(
	"id"        VARCHAR(255) COLLATE "C" NOT NULL,
	"owner_id"  VARCHAR(255) COLLATE "C" NOT NULL,
	"name"      VARCHAR NOT NULL,
	"type"      VARCHAR(50) NOT NULL DEFAULT 'DESKTOP',
	"publickey" VARCHAR NOT NULL,
	"creation_time" TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT "DEVICE_LEGACY_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_LEGACY_FK_USER" FOREIGN KEY ("owner_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEVICE_LEGACY_UNIQUE_NAME_PER_OWNER" UNIQUE ("owner_id", "name")
);
COMMENT ON COLUMN "device_legacy"."publickey" IS 'Note: This contains base64url-encoded data for historic reasons.';

CREATE TABLE "access_token_legacy"
(
	"device_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"  UUID NOT NULL,
	"jwe"       VARCHAR NOT NULL UNIQUE,
	CONSTRAINT "ACCESS_LEGACY_PK" PRIMARY KEY ("device_id", "vault_id"),
	CONSTRAINT "ACCESS_LEGACY_FK_DEVICE" FOREIGN KEY ("device_id") REFERENCES "device_legacy" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_LEGACY_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);

-- --------- --
-- AUDIT LOG --
-- --------- --
CREATE SEQUENCE audit_event_id_seq AS BIGINT;
CREATE TABLE "audit_event"
(
	"id"        BIGINT NOT NULL DEFAULT nextval('audit_event_id_seq'),
	"type"      VARCHAR(50) NOT NULL,
	"timestamp" TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT "AUDIT_EVENT_PK" PRIMARY KEY ("id")
);
ALTER SEQUENCE audit_event_id_seq OWNED BY audit_event.id;

CREATE TABLE "audit_event_vault_create"
(
	"id"                BIGINT NOT NULL,
	"created_by"        VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"          UUID NOT NULL,
	"vault_name"        VARCHAR NOT NULL,
	"vault_description" VARCHAR,
	CONSTRAINT "AUDIT_EVENT_VAULT_CREATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_CREATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_key_retrieve"
(
	"id"          BIGINT NOT NULL,
	"retrieved_by" VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"    UUID NOT NULL,
	"result"      VARCHAR(50) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_UNLOCK_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_UNLOCK_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_member_add"
(
	"id"           BIGINT NOT NULL,
	"added_by"     VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"role"         VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_ADD_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_ADD_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_member_remove"
(
	"id"           BIGINT NOT NULL,
	"removed_by"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_REMOVE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_REMOVE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

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

CREATE TABLE "audit_event_vault_ownership_claim"
(
	"id"           BIGINT NOT NULL,
	"claimed_by"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_OWNERSHIP_CLAIM_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_OWNERSGIP_CLAIM_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_device_register"
(
    "id"            BIGINT NOT NULL,
    "registered_by" VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id"     VARCHAR(64) COLLATE "C" NOT NULL,
    "device_name"   VARCHAR NOT NULL,
    "device_type"   VARCHAR(50) NOT NULL,
    CONSTRAINT "AUDIT_EVENT_DEVICE_REGISTER_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_DEVICE_REGISTER_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_device_remove"
(
    "id"         BIGINT NOT NULL,
    "removed_by" VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id"  VARCHAR(64) COLLATE "C" NOT NULL,
    CONSTRAINT "AUDIT_EVENT_DEVICE_REMOVE_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_DEVICE_REMOVE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_update"
(
    "id"                BIGINT NOT NULL,
    "updated_by"        VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"          UUID NOT NULL,
    "vault_name"        VARCHAR NOT NULL,
    "vault_description" VARCHAR,
    "vault_archived"    BOOLEAN NOT NULL,
    CONSTRAINT "AUDIT_EVENT_VAULT_UPDATE_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_VAULT_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_access_grant"
(
    "id"           BIGINT NOT NULL,
    "granted_by"   VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"     UUID NOT NULL,
    "authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
    CONSTRAINT "AUDIT_EVENT_VAULT_ACCESS_GRANT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_VAULT_ACCESS_GRANT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);