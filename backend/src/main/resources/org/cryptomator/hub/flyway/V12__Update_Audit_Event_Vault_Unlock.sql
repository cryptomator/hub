ALTER TABLE "audit_event_vault_unlock" RENAME TO "audit_event_vault_key_retrieve";
ALTER TABLE "audit_event_vault_key_retrieve" RENAME COLUMN "unlocked_by" TO "retrieved_by";
ALTER TABLE "audit_event_vault_key_retrieve" DROP COLUMN "device_id";
