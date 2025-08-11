ALTER TABLE "settings" ADD "default_required_emergency_key_shares" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "settings" ADD "allow_choosing_emergency_council" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE "vault" ADD "requried_emergency_key_shares" INTEGER NOT NULL DEFAULT 0;

CREATE TABLE "default_emergency_council"
(
	"settings_id" INT4 DEFAULT 0 NOT NULL,
	"member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_PK" PRIMARY KEY ("member_id"),
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_FK_SETTINGS" FOREIGN KEY ("settings_id") REFERENCES "settings" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEFAULT_EMERGENCY_COUNCIL_FK_USER" FOREIGN KEY ("member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "emergency_key_shares"
(
	"vault_id" UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
	"emergency_key_share" TEXT NOT NULL,
	CONSTRAINT "EMERGENCY_KEYS_PK" PRIMARY KEY ("vault_id", "council_member_id"),
	CONSTRAINT "EMERGENCY_KEYS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "EMERGENCY_KEYS_FK_USER" FOREIGN KEY ("council_member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "emergency_recovery_processes"
(
    "id" UUID NOT NULL,
    "vault_id" UUID NOT NULL,
    "type" VARCHAR(50) NOT NULL,
    "details" TEXT,
    "required_key_shares" INTEGER NOT NULL,
    "process_public_key" TEXT NOT NULL,
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_PK" PRIMARY KEY ("id"),
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_TYPE" CHECK ("type" = 'ASSIGN_OWNER' OR "type" = 'COUNCIL_CHANGE'),
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_UNIQUE_VAULT_AND_TYPE" UNIQUE ("vault_id", "type")
);

CREATE TABLE "recovered_emergency_key_shares"
(
    "recovery_process_id" UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
    "process_private_key" TEXT NOT NULL,
    "recovered_key_share" TEXT,
    "signed_process_info" TEXT,
    CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_PK" PRIMARY KEY ("recovery_process_id", "council_member_id"),
    CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_PROCESS" FOREIGN KEY ("recovery_process_id") REFERENCES "emergency_recovery_processes" ("id") ON DELETE CASCADE,
	CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_USER" FOREIGN KEY ("council_member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

-- Events

CREATE TABLE "audit_event_emergaccess_settings_updated"
(
	"id"                     BIGINT NOT NULL,
	"admin_id"               VARCHAR(255) COLLATE "C" NOT NULL,
	"council_member_ids"     TEXT NOT NULL,
	"required_key_shares"    INTEGER NOT NULL,
	"allow_choosing_council" BOOLEAN NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETTINGS_UPDATED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETTINGS_UPDATED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_emergaccess_setup"
(
	"id"           BIGINT NOT NULL,
	"vault_id"     UUID NOT NULL,
	"owner_id"     VARCHAR(255) COLLATE "C" NOT NULL,
	"settings"     TEXT NOT NULL,
	"ip_address"   VARCHAR(46) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETUP_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_SETUP_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
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
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_COMPLETED_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_EMERGACCESS_RECOVERY_COMPLETED_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
