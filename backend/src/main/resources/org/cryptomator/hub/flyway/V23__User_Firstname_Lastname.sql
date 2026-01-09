ALTER TABLE "user_details"
ADD COLUMN "firstname" VARCHAR,
ADD COLUMN "lastname" VARCHAR,
ADD COLUMN "realm_roles" VARCHAR[] NOT NULL DEFAULT ARRAY['user'];