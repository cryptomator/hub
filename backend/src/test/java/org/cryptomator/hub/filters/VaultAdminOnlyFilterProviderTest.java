package org.cryptomator.hub.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
class VaultAdminOnlyFilterProviderTest {

	private static final String NO_VAULT_ID_TOKEN = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCJ9.e30.vT0jNCotwtzr37JNM_C6uZFCw3GvVjcikn-CVrDociILPiXBXA8i7dWFwBnUQkDBcFbouh-eUB_wEWgqe9WTG2rT66_c1G2LZUQcCsKdWJdTyK4ZxLXLYOYhNOHtqShI";
	private static final String INVALID_VAULT_ID_TOKEN = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOjI1fQ.e30.YxqmX5xeOviP9WldQV870zhPEF4PRaZrW0TaoWzm4lvkEmacIUt3OIoH0grAeh_gtJNRg4WfnqFNTgUx40-yDOtBLzyoeubfrMgb0-agN1898Mbr4ZhD1xqor0lBDrmc";
	private static final String NO_DATES_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.gRKW5JvCZ2th1tpigYTPdP_0v0ChwfoTg-Bpt8YsFujteAp6MBUtNhHQ5_O9yUl_8_Uj0S3F4ztVveIL_U37JMO0q41K_QQ68Nkhf7qlHX2tk2EyzIVXyttwYnwuubeO";
	private static final String NO_ISSUED_DATE_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJleHAiOjE1MTYyMzkwMzAsIm5iZiI6MTUxNjIzOTAwMH0.AKnDRHfm9TSrNDWNM_OYHMWyCZFEekpmwqpOlpEjH4jnygcb5nT6e_kPgORN0yua0fn6GoDLNeZnvsAQriHPFZbidYnPL7OzLgx1b3gPnL5ntdAtmDJY-TcCsPGgkCeG";
	private static final String NO_NOT_BEFORE_DATE_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMTUsImV4cCI6MTUxNjIzOTAzMH0.hTUUOsGR6_45JDliD8td1CMyLqiTxMtfgJqx23_UtZI4A8p13uAAESy_aE6YxF3unGliyB8FuBE5q8tZkYBGXb3oWgubCtG1_LvxNednmw_tczXkge1aG_LWsiuLl53E";
	private static final String NO_EXPIRES_DATE_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMTUsIm5iZiI6MTUxNjIzOTAwMH0.J7EX_yuYLoQSakCIl-c4-uSpw3idn2CJdr51_B968d06K87Z_lFzvOSq6aqS0NJvsdDOOfCdCID5zQDgFI2ROUeMqy2jpwjpGqioikCgNNBSwiq6VlfAsAxKZ-MGY36y";

	private VaultAdminOnlyFilterProvider vaultAdminOnlyFilterProvider;
	private ContainerRequestContext context;
	private UriInfo uriInfo;

	@BeforeEach
	void setUp() {
		vaultAdminOnlyFilterProvider = Mockito.spy(new VaultAdminOnlyFilterProvider());
		context = Mockito.mock(ContainerRequestContext.class);
		uriInfo = Mockito.mock(UriInfo.class);

		// Decorate verifier to use fixed time
		Mockito.doAnswer(invocationOnMock -> {
			Algorithm algorithm = invocationOnMock.getArgument(0);
			var verifier = (JWTVerifier.BaseVerification) vaultAdminOnlyFilterProvider.buildVerifier(algorithm);
			return verifier.build(VaultAdminOnlyFilterProviderTestConstants.NOW);
		}).when(vaultAdminOnlyFilterProvider).buildVerification(Mockito.any());
	}

	@Nested
	@DisplayName("Test JWT verification")
	public class TestJWTVerification {

		private static final String PUBLIC_KEY_VAULT_2 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEC1uWSXj2czCDwMTLWV5BFmwxdM6PX9p+Pk9Yf9rIf374m5XP1U8q79dBhLSIuaojsvOT39UUcPJROSD1FqYLued0rXiooIii1D3jaW6pmGVJFhodzC31cy5sfOYotrzF";

		@Test
		@DisplayName("validate valid clientJWT")
		public void testValidClientJWT() {
			vaultAdminOnlyFilterProvider.verify(verifier(), JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2));
		}

		@Test
		@DisplayName("validate expired clientJWT")
		public void testExpiredClientJWT() {
			Assertions.assertThrows(VaultAdminTokenExpiredException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), JWT.decode(VaultAdminOnlyFilterProviderTestConstants.EXPIRED_TOKEN_VAULT_2)));
		}

		@Test
		@DisplayName("validate future issue at clientJWT")
		public void testFutureIssueAtClientJWT() {
			Assertions.assertThrows(VaultAdminTokenNotYetValidException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), JWT.decode(VaultAdminOnlyFilterProviderTestConstants.FUTURE_ISSUE_AT_TOKEN_VAULT_2)));
		}

		@Test
		@DisplayName("validate future not before clientJWT")
		public void testFutureNotBeforeClientJWT() {
			Assertions.assertThrows(VaultAdminTokenNotYetValidException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), JWT.decode(VaultAdminOnlyFilterProviderTestConstants.FUTURE_NOT_BEFORE_TOKEN_VAULT_2)));
		}

		private com.auth0.jwt.interfaces.JWTVerifier verifier() {
			return vaultAdminOnlyFilterProvider.buildVerification(Algorithm.ECDSA384(VaultAdminOnlyFilterProvider.decodePublicKey(PUBLIC_KEY_VAULT_2), null));
		}
	}

	@Nested
	@DisplayName("Test vaultId qurey parameter")
	public class TestVaultIdQueryParameter {

		@Test
		@DisplayName("validate valid vaultId in query")
		public void testGetValidVaultIdQueryParameter() {
			var pathParams = new MultivaluedHashMap<String, String>();
			pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

			Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
			Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

			String result = vaultAdminOnlyFilterProvider.getVaultIdQueryParameter(context);
			Assertions.assertEquals("vault2", result);
		}

		@Test
		@DisplayName("validate no vaultId in query")
		public void testNoVaultIdQueryParameter() {
			var pathParams = new MultivaluedHashMap<String, String>();

			Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
			Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getVaultIdQueryParameter(context));
		}

		@Test
		@DisplayName("validate multiple vaultId in query")
		public void testMultipleVaultIdQueryParameter() {
			var pathParams = new MultivaluedHashMap<String, String>();
			pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");
			pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault3");

			Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
			Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getVaultIdQueryParameter(context));
		}
	}

	@Nested
	@DisplayName("Test ClientJWT decoding")
	public class TestClientJWTDecoding {
		@Test
		@DisplayName("validate valid Client-JWT")
		public void testValidClientJWTProvided() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
			DecodedJWT result = vaultAdminOnlyFilterProvider.getClientJWT(context);
			Assertions.assertEquals(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2).getHeader(), result.getHeader());
			Assertions.assertEquals(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2).getPayload(), result.getPayload());
		}

		@Test
		@DisplayName("validate no Client-JWT")
		public void testNoClientJWTProvided() {
			Assertions.assertThrows(VaultAdminNotProvidedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}

		@Test
		@DisplayName("validate malformed Client-JWT")
		public void testMalformedClientJWT() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.MALFORMED_TOKEN);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}

		@Test
		@DisplayName("validate no dates in Client-JWT")
		public void testMalformedClientJWTNoDates() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(NO_DATES_TOKEN_VAULT_2);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}

		@Test
		@DisplayName("validate no issued date in Client-JWT")
		public void testMalformedClientJWTNoIssueDate() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(NO_ISSUED_DATE_TOKEN_VAULT_2);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}

		@Test
		@DisplayName("validate no not before date in Client-JWT")
		public void testMalformedClientJWTNoNotBeforeDate() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(NO_NOT_BEFORE_DATE_TOKEN_VAULT_2);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}

		@Test
		@DisplayName("validate no expires date in Client-JWT")
		public void testMalformedClientJWTNoExpiresDate() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(NO_EXPIRES_DATE_TOKEN_VAULT_2);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getClientJWT(context));
		}
	}

	@Nested
	@DisplayName("Test ClientJWT header extraction")
	public class TestClientJWTHeaderExtraction {

		@Test
		@DisplayName("validate valid Client-JWT leads to valid vaultId")
		public void testValidClientJWTLeadsToValidVaultId() {
			String result = vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2));
			Assertions.assertEquals("vault2", result);
		}

		@Test
		@DisplayName("validate no vaultId in Client-JWT")
		public void testNoVaultIdInJWT() {
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(NO_VAULT_ID_TOKEN)));
		}

		@Test
		@DisplayName("validate invalid vaultId in Client-JWT")
		public void testInvalidVaultIdInJWT() {
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(INVALID_VAULT_ID_TOKEN)));
		}
	}
}