ALTER TABLE "settings" ADD "enable_automatic_access_grant" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "settings" ADD "automatic_access_grant_trust_threshold" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "settings" ADD "allow_automatic_access_grant_override" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "settings" ADD CONSTRAINT "check_auto_grant_trust_threshold" CHECK ("automatic_access_grant_trust_threshold" >= -1 AND "automatic_access_grant_trust_threshold" < 10);

CREATE TABLE "audit_event_setting_auto_grant_update"
(
	"id"               BIGINT NOT NULL,
	"updated_by"       VARCHAR(255) COLLATE "C" NOT NULL,
	"enabled"          BOOLEAN NOT NULL,
	"trust_threshold"  INTEGER NOT NULL,
	"allow_override"   BOOLEAN NOT NULL,
	CONSTRAINT "AUDIT_EVENT_SETTING_AUTO_GRANT_UPDATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_SETTING_AUTO_GRANT_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
