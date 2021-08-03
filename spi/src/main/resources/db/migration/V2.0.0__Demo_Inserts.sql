-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO user
VALUES ('5e362e34-a767-4b0d-a05f-f4b2aea32b88', 'owner');
INSERT INTO user
VALUES ('userId2', 'userName2');
INSERT INTO user
VALUES ('userId3', 'userName3');

INSERT INTO vault
VALUES ('vaultId1', '5e362e34-a767-4b0d-a05f-f4b2aea32b88', 'vault1', 'salt1', 'iterations1', 'masterkey1');
INSERT INTO vault
VALUES ('vaultId2', '5e362e34-a767-4b0d-a05f-f4b2aea32b88', 'vault2', 'salt2', 'iterations2', 'masterkey2');

INSERT INTO device
VALUES ('deviceId1', '5e362e34-a767-4b0d-a05f-f4b2aea32b88', 'device1', 'pubkey1');
INSERT INTO device
VALUES ('deviceId2', '5e362e34-a767-4b0d-a05f-f4b2aea32b88', 'device2', 'pubkey2');

INSERT INTO access
VALUES ('deviceId1', 'vaultId1', 'specificMasterKeyDevice1Vault1');
INSERT INTO access
VALUES ('deviceId1', 'vaultId2', 'specificMasterKeyDevice1Vault2');
