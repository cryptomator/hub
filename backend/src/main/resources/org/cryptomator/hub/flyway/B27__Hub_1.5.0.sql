-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE "settings"
(
	"id"                                    INT4         NOT NULL,
	"hub_id"                                VARCHAR(255) NOT NULL,
	"license_key"                           VARCHAR,
	"wot_max_depth"                         INTEGER      NOT NULL DEFAULT 3,
	"wot_id_verify_len"                     INTEGER      NOT NULL DEFAULT 2,
	"default_required_emergency_key_shares" INTEGER      NOT NULL DEFAULT 2,
	"default_min_members"                   INTEGER      NOT NULL DEFAULT 3,
	"allow_choosing_emergency_council"      BOOLEAN      NOT NULL DEFAULT false,
	"enable_emergency_access"               BOOLEAN      NOT NULL DEFAULT false,
	CONSTRAINT "SETTINGS_PK" PRIMARY KEY ("id"),
	CONSTRAINT "check_wot_max_depth" CHECK ("wot_max_depth" >= 0 AND "wot_max_depth" < 10)
);

INSERT INTO "settings" ("id", "hub_id") VALUES (0, gen_random_uuid());

CREATE TABLE "authority"
(
	"id"          VARCHAR(255) COLLATE "C" NOT NULL,
	"type"        VARCHAR(5)   NOT NULL,
	"name"        VARCHAR      NOT NULL,
	"picture_url" VARCHAR,
	CONSTRAINT "AUTHORITY_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUTHORITY_CHK_TYPE" CHECK ("type" = 'USER' OR "type" = 'GROUP')
);

CREATE TABLE "user_details"
(
	"id"              VARCHAR(255) COLLATE "C" NOT NULL,
	"email"           VARCHAR,
	"ecdh_publickey"  VARCHAR, -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
	"privatekeys"     VARCHAR, -- private keys (ECDH + ECDSA), encrypted using setup code (JWE PBES2)
	"setupcode"       VARCHAR, -- setup code, encrypted using user's public key (JWE ECDH-ES)
	"ecdsa_publickey" VARCHAR, -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
	"language"        VARCHAR,
	"firstname"       VARCHAR,
	"lastname"        VARCHAR,
	"enabled"         BOOLEAN NOT NULL DEFAULT true,
	CONSTRAINT "USER_DETAIL_PK" PRIMARY KEY ("id"),
	CONSTRAINT "USER_DETAIL_FK_USER" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE TABLE "group_details"
(
	"id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "GROUP_DETAIL_PK" PRIMARY KEY ("id"),
	CONSTRAINT "GROUP_DETAIL_FK_GROUP" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
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

-- top down lookups ("get members of a group") is already efficient due to PK
-- bottom up lookups ("get groups of a member") need an additional index:
CREATE INDEX "group_membership_idx_member" ON "group_membership" USING btree ("member_id");

CREATE TABLE "effective_group_membership"
(
	"group_id"               VARCHAR(255) NOT NULL,
	"intermediate_group_ids" VARCHAR[]    NOT NULL,
	"member_id"              VARCHAR(255) NOT NULL,
	PRIMARY KEY ("group_id", "member_id"),
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_CHK_NOTSAME" CHECK ("group_id" <> "member_id"),
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_FK_GROUP" FOREIGN KEY ("group_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_FK_MEMBER" FOREIGN KEY ("member_id") REFERENCES "authority" ("id") ON DELETE CASCADE
);
-- index to speed up queries filtering by intermediate groups
CREATE INDEX "effective_group_membership_idx_intermediate" ON "effective_group_membership" USING gin ("intermediate_group_ids");
-- top down lookups ("get members of a group") is already efficient due to PK
-- bottom up lookups ("get groups of a member") need an additional index:
CREATE INDEX "effective_group_membership_idx_member" ON "effective_group_membership" USING btree ("member_id");

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
	"required_emergency_key_shares" INTEGER NOT NULL DEFAULT 0,
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
	"id"               VARCHAR(255) NOT NULL,
	"owner_id"         VARCHAR(255) NOT NULL,
	"name"             VARCHAR NOT NULL,
	"type"             VARCHAR(50) NOT NULL DEFAULT 'DESKTOP',
	"publickey"        VARCHAR NOT NULL,        -- base64-encoded SPKI DER (RFC 5280, 4.1.2.7)
	"user_privatekeys" VARCHAR NOT NULL,        -- private keys (ECDH + ECDSA), encrypted using device's public key (JWE ECDH-ES)
	"creation_time"    TIMESTAMP WITH TIME ZONE NOT NULL,
    "last_access_time" TIMESTAMP WITH TIME ZONE,
    "last_ip_address"  VARCHAR(46),
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "device_user_privatekey_key" UNIQUE ("user_privatekeys"),
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

-- ----------- --
-- WEB OF TRUST --
-- ----------- --
CREATE TABLE "wot"
(
	"user_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"signer_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"signature" VARCHAR NOT NULL,
	CONSTRAINT "wot_pkey" PRIMARY KEY ("user_id", "signer_id"),
	CONSTRAINT "fk_user_id" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
	CONSTRAINT "fk_signer_id" FOREIGN KEY ("signer_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

-- @formatter:off
CREATE VIEW "effective_wot" ("trusting_user_id", "trusted_user_id", "signature_chain") AS
WITH RECURSIVE "r" ("trusting_user_id", "trusted_user_id", "depth", "signer_chain", "signature_chain") AS (
	-- Anchor member: Directly trusted users
	SELECT "signer_id", "user_id", 0, array["signer_id"]::varchar[], array["signature"]::varchar[]
	FROM "wot"

	UNION ALL

	-- Recursive member: Transitive trust
	SELECT "r"."trusting_user_id", "wot"."user_id", "r"."depth" + 1, ("r"."signer_chain" || "wot"."signer_id")::varchar[], ("r"."signature_chain" || "wot"."signature")::varchar[]
	FROM  "wot"
	INNER JOIN "r"
	    ON "wot"."signer_id" = "r"."trusted_user_id"  -- primary recursion criteria
	    AND "wot"."user_id" <> ALL("r"."signer_chain") -- only if user isn't part of signature chain already (avoid loops)
	INNER JOIN "settings" ON "settings"."id" = 0
	WHERE "r"."depth" < "settings"."wot_max_depth"
)
SELECT
    DISTINCT ON ("trusting_user_id", "trusted_user_id") -- keep only one relation (ordered by depth), i.e. the shortest path
    "trusting_user_id", "trusted_user_id", "signature_chain"
    FROM "r"
    ORDER BY "trusting_user_id", "trusted_user_id", "depth";
-- @formatter:on

-- ---------------- --
-- EMERGENCY ACCESS --
-- ---------------- --
CREATE TABLE "emergency_recovery_processes"
(
	"id"                   UUID NOT NULL,
	"vault_id"             UUID NOT NULL,
	"type"                 VARCHAR(50) NOT NULL,
	"details"              TEXT,
	"required_key_shares"  INTEGER NOT NULL,
	"process_public_key"   TEXT NOT NULL,
	CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_PK" PRIMARY KEY ("id"),
	CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_UNIQUE_VAULT_AND_TYPE" UNIQUE ("vault_id", "type"),
	CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_TYPE" CHECK ("type" = 'CHANGE_PERMISSIONS' OR "type" = 'COUNCIL_CHANGE')
);

CREATE TABLE "emergency_key_shares"
(
	"vault_id"           UUID NOT NULL,
	"council_member_id"  VARCHAR(255) COLLATE "C" NOT NULL,
	"emergency_key_share" TEXT NOT NULL,
	CONSTRAINT "EMERGENCY_KEYS_PK" PRIMARY KEY ("vault_id", "council_member_id"),
	CONSTRAINT "EMERGENCY_KEYS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "EMERGENCY_KEYS_FK_USER" FOREIGN KEY ("council_member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "recovered_emergency_key_shares"
(
	"recovery_process_id"   UUID NOT NULL,
	"council_member_id"     VARCHAR(255) COLLATE "C" NOT NULL,
	"process_private_key"   TEXT NOT NULL,
	"unrecovered_key_share" TEXT NOT NULL,
	"recovered_key_share"   TEXT,
	"signed_process_info"   TEXT,
	CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_PK" PRIMARY KEY ("recovery_process_id", "council_member_id"),
	CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_PROCESS" FOREIGN KEY ("recovery_process_id") REFERENCES "emergency_recovery_processes" ("id") ON DELETE CASCADE,
	CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_USER" FOREIGN KEY ("council_member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "default_emergency_council"
(
	"settings_id" INTEGER NOT NULL DEFAULT 0,
	"member_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_PK" PRIMARY KEY ("member_id"),
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_FK_SETTINGS" FOREIGN KEY ("settings_id") REFERENCES "settings" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_FK_USER" FOREIGN KEY ("member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
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
	"id"           BIGINT NOT NULL,
	"retrieved_by" VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"result"       VARCHAR(50) NOT NULL,
	"ip_address"   VARCHAR(46),
	"device_id"    VARCHAR(255) COLLATE "C",
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
	"id"         BIGINT NOT NULL,
	"claimed_by" VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"   UUID NOT NULL,
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

CREATE TABLE "audit_event_user_keys_change"
(
	"id"         BIGINT NOT NULL,
	"changed_by" VARCHAR(255) COLLATE "C" NOT NULL,
	"user_name"  VARCHAR NOT NULL,
	CONSTRAINT "AUDIT_EVENT_USER_ACCOUNT_SETUP_COMPLETE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_USER_ACCOUNT_SETUP_COMPLETE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_user_setupcode_change"
(
	"id"         BIGINT NOT NULL,
	"changed_by" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "AUDIT_EVENT_USER_SETUPCODE_CHANGE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_USER_ACCOUNT_SETUPCODE_CHANGE_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_setting_wot_update"
(
	"id"                BIGINT NOT NULL,
	"updated_by"        VARCHAR(255) COLLATE "C" NOT NULL,
	"wot_max_depth"     INTEGER NOT NULL,
	"wot_id_verify_len" INTEGER NOT NULL,
	CONSTRAINT "AUDIT_EVENT_SETTING_WOT_UPDATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_SETTING_WOT_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_sign_wot_id"
(
	"id"         BIGINT NOT NULL,
	"user_id"    VARCHAR(255) COLLATE "C" NOT NULL,
	"signer_id"  VARCHAR(255) COLLATE "C" NOT NULL,
	"signer_key" VARCHAR NOT NULL,
	"signature"  VARCHAR NOT NULL,
	CONSTRAINT "AUDIT_EVENT_SIGN_WOT_ID_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_SIGN_WOT_ID_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_user_account_reset"
(
	"id"       BIGINT NOT NULL,
	"reset_by" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "AUDIT_EVENT_USER_ACCOUNT_RESET_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_USER_ACCOUNT_RESET_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_setup"
(
	"id"         BIGINT NOT NULL,
	"vault_id"   UUID NOT NULL,
	"owner_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"settings"   TEXT NOT NULL,
	"ip_address" VARCHAR(46) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETUP_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETUP_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_settings_updated"
(
	"id"                      BIGINT NOT NULL,
	"admin_id"                VARCHAR(255) COLLATE "C" NOT NULL,
	"enable_emergency_access" BOOLEAN NOT NULL,
	"council_member_ids"      TEXT NOT NULL,
	"required_key_shares"     INTEGER NOT NULL,
	"min_members"             INTEGER NOT NULL,
	"allow_choosing_council"  BOOLEAN NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETTINGS_UPDATED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETTINGS_UPDATED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_recovery_started"
(
	"id"                BIGINT NOT NULL,
	"vault_id"          UUID NOT NULL,
	"process_id"        UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"process_type"      VARCHAR(50) NOT NULL,
	"details"           TEXT NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_STARTED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_STARTED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_recovery_aborted"
(
	"id"                BIGINT NOT NULL,
	"vault_id"          UUID NOT NULL,
	"process_id"        UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"ip_address"        VARCHAR(46) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_ABORTED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_ABORTED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_recovery_approved"
(
	"id"                BIGINT NOT NULL,
	"process_id"        UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"ip_address"        VARCHAR(46) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_APPROVED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_APPROVED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_recovery_completed"
(
	"id"                BIGINT NOT NULL,
	"process_id"        UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"ip_address"        VARCHAR(46) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_COMPLETED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_COMPLETED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
