-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO user
VALUES ('test-uuid-for-testing-only', 'owner');
INSERT INTO user
VALUES ('userId2', 'userName2');
INSERT INTO user
VALUES ('userId3', 'userName3');

INSERT INTO vault
VALUES ('vaultId1', 'test-uuid-for-testing-only', 'vault1', 'salt1', 'iterations1', 'masterkey1');
INSERT INTO vault
VALUES ('vaultId2', 'test-uuid-for-testing-only', 'vault2', 'salt2', 'iterations2', 'masterkey2');
