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

INSERT INTO device
VALUES ('deviceId1', 'test-uuid-for-testing-only', 'device1', 'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEInfsHWhmb9iQssLt_VqVX6Dkd8nVekf0jApSJuNYfoAmvvi-bbp6SxoXZV5I8USOsSW-dc9kxBqCNHGuTTCtRg==');
INSERT INTO device
VALUES ('deviceId2', 'test-uuid-for-testing-only', 'device2', 'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8PAmkx_HLeVaZfpJX9lJEXwtQCpm97bNfjEAQQwUH9RjGCTeZggTIQE6oUSVd07dtjvhfXG7MRvHDM3llaYw7Q==');
