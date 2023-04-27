package org.cryptomator.hub.filters;

import java.time.Instant;

final class VaultAdminOnlyFilterProviderTestConstants {

	// { "alg": "ES384", "typ": "JWT", "vaultId": "7E57C0DE-0000-4000-8000-000100002222" } { "iat": 1516239015 (2018-01-18T01:30:15) }
	static final String VALID_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiI3RTU3QzBERS0wMDAwLTQwMDAtODAwMC0wMDAxMDAwMDIyMjIifQ.eyJpYXQiOjE1MTYyMzkwMTV9.x3-JltFRYrwC6fNBgtvCHyIh8HzmcS190GVSbKzhLROeIyYpvvWo9PH_nVHa_8p6xQoMrwf7-H5gQYVm3EhtHWO_2CZro55zdzFkLThU26ql6yWtGPNroTmOyUT1MSQs";

	// { "alg": "ES384", "typ": "JWT", "vaultId": "7E57C0DE-0000-4000-8000-000100003000" } { "iat": 1516239015 (2018-01-18T01:30:15) }
	static final String VALID_TOKEN_VAULT_3000 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiI3RTU3QzBERS0wMDAwLTQwMDAtODAwMC0wMDAxMDAwMDMwMDAifQ.eyJpYXQiOjE1MTYyMzkwMTV9.MCI388EG6LAXuRLVm6_YFEP-Up8bYI2SBvCtIv3azrPtmNbidR5KxtSVoV_W3iFsG8AUj4G7JLxT8F-b4Dw1i3VBhPMVl4GlC_AN89yvp5SPgtfYmIUdHWvcugahayHh";

	// { "alg": "ES384", "typ": "JWT", "vaultId": "7E57C0DE-0000-4000-8000-000100002222" } { "iat": 1516239015 (2018-01-18T01:30:15) } but signed with key of vault1
	static final String INVALID_SIGNATURE_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsInZhdWx0SWQiOiI3RTU3QzBERS0wMDAwLTQwMDAtODAwMC0wMDAxMDAwMDIyMjIifQ.e30.9_5pMhgkn9iyOG01T82hB00tHEELwMX0BGIc2_DwzZSizJYNz312B5xWkI1TOwzteEpWO2ivdki3NfgJkRsNBOJ02H5QJ8Zg4qT5lCbWySdZpMeSODTjHRuN5lErwAR2";
	static final String MALFORMED_TOKEN = "hello world";
	static final Instant NOW  = Instant.ofEpochSecond(1516239015); // 2018-01-18T01:30:15

}
