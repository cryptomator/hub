CREATE TABLE "audit_event_setting_wot_update"
(
	"id"                BIGINT NOT NULL,
	"updated_by"        VARCHAR(255) COLLATE "C" NOT NULL,
	"wot_max_depth"     INTEGER NOT NULL,
	"wot_id_verify_len" INTEGER NOT NULL,
	CONSTRAINT "AUDIT_EVENT_SETTING_WOT_UPDATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_SETTING_WOT_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);