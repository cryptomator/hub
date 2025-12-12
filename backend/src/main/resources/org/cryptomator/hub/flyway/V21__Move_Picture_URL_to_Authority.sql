-- noinspection SqlNoDataSourceInspectionForFile
ALTER TABLE "user_details" DROP COLUMN "picture_url";
ALTER TABLE "authority" ADD "picture_url" VARCHAR;