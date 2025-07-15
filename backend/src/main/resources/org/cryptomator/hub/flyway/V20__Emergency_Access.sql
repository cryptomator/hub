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
	"emergency_key_share" VARCHAR(255) COLLATE "C" NOT NULL,
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
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_PK" PRIMARY KEY ("id"),
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
    CONSTRAINT "EMERGENCY_RECOVERY_PROCESSES_TYPE" CHECK ("type" = 'RECOVERY' OR "type" = 'COUNCIL_CHANGE')
);

CREATE TABLE "recovered_emergency_key_shares"
(
    "recovery_process_id" UUID NOT NULL,
	"council_member_id" VARCHAR(255) COLLATE "C" NOT NULL,
    "process_private_key" VARCHAR(255) NOT NULL,
    "recovered_key_share" VARCHAR(255),
    CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_PK" PRIMARY KEY ("recovery_process_id", "council_member_id"),
    CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_PROCESS" FOREIGN KEY ("recovery_process_id") REFERENCES "emergency_recovery_processes" ("id") ON DELETE CASCADE,
	CONSTRAINT "RECOVERED_EMERGENCY_KEY_SHARES_FK_USER" FOREIGN KEY ("council_member_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);