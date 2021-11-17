-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE user
(
	id   VARCHAR(255) NOT NULL,
	name VARCHAR(255) NOT NULL,
	picture_url VARCHAR(255),
	CONSTRAINT pk_user PRIMARY KEY (id)
);

CREATE TABLE vault
(
	id         VARCHAR(255) NOT NULL,
	user_id    VARCHAR(255) NOT NULL,
	name       VARCHAR(255) NOT NULL,
	salt       VARCHAR(255) NOT NULL,
	iterations VARCHAR(255) NOT NULL,
	masterkey  VARCHAR(255) NOT NULL,
	CONSTRAINT pk_vault PRIMARY KEY (id),
	CONSTRAINT FK_VAULT_ON_USER FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE device
(
	id        VARCHAR(255) NOT NULL,
	user_id   VARCHAR(255) NOT NULL,
	name      VARCHAR(255) NOT NULL,
	publickey VARCHAR(255) NOT NULL,
	CONSTRAINT pk_device PRIMARY KEY (id),
	CONSTRAINT FK_DEVICE_ON_USER FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE access
(
	device_id                VARCHAR(255) NOT NULL,
	vault_id                 VARCHAR(255) NOT NULL,
	device_specific_masterkey VARCHAR(255) NOT NULL,
	ephemeral_public_key     VARCHAR(255) NOT NULL,
	CONSTRAINT pk_access PRIMARY KEY (device_id, vault_id),
	CONSTRAINT FK_ACCESS_ON_DEVICE FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE,
	CONSTRAINT FK_ACCESS_ON_VAULT FOREIGN KEY (vault_id) REFERENCES vault (id) ON DELETE CASCADE
);