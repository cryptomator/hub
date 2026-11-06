ALTER TABLE "user_details" ADD COLUMN "onboarding_completed" BOOLEAN NOT NULL DEFAULT FALSE;

-- existing users skip the onboarding tour
UPDATE "user_details" SET "onboarding_completed" = TRUE;
