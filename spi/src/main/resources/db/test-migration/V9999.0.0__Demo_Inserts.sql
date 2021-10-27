-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO user
VALUES ('userId2', 'userName2');
INSERT INTO user
VALUES ('userId3', 'userName3');

INSERT INTO vault
VALUES ('vaultId1', 'userId2', 'vault1', 'salt1', 'iterations1', 'masterkey1');
INSERT INTO vault
VALUES ('vaultId2', 'userId3', 'vault2', 'salt2', 'iterations2', 'masterkey2');
