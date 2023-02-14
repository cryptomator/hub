-- Cryptomator Hub 1.1.0

CREATE TABLE "audit_event"
(
	"id"        UUID  NOT NULL,
	"type"      VARCHAR(50) NOT NULL,
	"timestamp" TIMESTAMP  NOT NULL,
	CONSTRAINT "AUDIT_EVENT_PK" PRIMARY KEY ("id")
);

CREATE TABLE "unlock_event"
(
	"id"        UUID NOT NULL,
	"user_id"   VARCHAR(255) NOT NULL, -- TODO migrate
	"vault_id"  VARCHAR(255) NOT NULL, -- TODO migrate
	"device_id" VARCHAR(255) NOT NULL, -- TODO migrate
	"result"    VARCHAR(50) NOT NULL,
	CONSTRAINT "UNLOCK_EVENT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "UNLOCK_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
