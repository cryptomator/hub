-- align effective_group_membership authority id columns with "authority"."id" by collating them
ALTER TABLE "effective_group_membership" ALTER COLUMN "group_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "effective_group_membership" ALTER COLUMN "member_id" SET DATA TYPE VARCHAR(255) COLLATE "C";

-- make device_id columns the same width as in all other tables
ALTER TABLE "audit_event_device_register" ALTER COLUMN "device_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "audit_event_device_remove" ALTER COLUMN "device_id" SET DATA TYPE VARCHAR(255) COLLATE "C";

-- align device id columns with "legacy_device" columns by collating them
ALTER TABLE "device" ALTER COLUMN "id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "device" ALTER COLUMN "owner_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
