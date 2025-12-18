ALTER TABLE "user_details" ALTER COLUMN "language" TYPE VARCHAR;

UPDATE "user_details" SET "language" = 'de-DE' WHERE "language" = 'de';
UPDATE "user_details" SET "language" = 'en-US' WHERE "language" = 'en';
UPDATE "user_details" SET "language" = 'fr-FR' WHERE "language" = 'fr';
UPDATE "user_details" SET "language" = 'it-IT' WHERE "language" = 'it';
UPDATE "user_details" SET "language" = 'nl-NL' WHERE "language" = 'nl';
UPDATE "user_details" SET "language" = 'pt-PT' WHERE "language" = 'pt';
UPDATE "user_details" SET "language" = 'tr-TR' WHERE "language" = 'tr';
