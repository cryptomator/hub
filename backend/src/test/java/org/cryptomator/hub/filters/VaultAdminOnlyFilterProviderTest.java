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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
class VaultAdminOnlyFilterProviderTest {

	private static final String NO_VAULT_ID_TOKEN = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCJ9.e30.vT0jNCotwtzr37JNM_C6uZFCw3GvVjcikn-CVrDociILPiXBXA8i7dWFwBnUQkDBcFbouh-eUB_wEWgqe9WTG2rT66_c1G2LZUQcCsKdWJdTyK4ZxLXLYOYhNOHtqShI";
	private static final String INVALID_VAULT_ID_TOKEN = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOjI1fQ.e30.YxqmX5xeOviP9WldQV870zhPEF4PRaZrW0TaoWzm4lvkEmacIUt3OIoH0grAeh_gtJNRg4WfnqFNTgUx40-yDOtBLzyoeubfrMgb0-agN1898Mbr4ZhD1xqor0lBDrmc";
	private static final String NO_IAT_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.gRKW5JvCZ2th1tpigYTPdP_0v0ChwfoTg-Bpt8YsFujteAp6MBUtNhHQ5_O9yUl_8_Uj0S3F4ztVveIL_U37JMO0q41K_QQ68Nkhf7qlHX2tk2EyzIVXyttwYnwuubeO";
	private VaultAdminOnlyFilterProvider vaultAdminOnlyFilterProvider;
	private ContainerRequestContext context;
	private UriInfo uriInfo;

	@BeforeEach
	void setUp() {
		vaultAdminOnlyFilterProvider = Mockito.spy(new VaultAdminOnlyFilterProvider());
		context = Mockito.mock(ContainerRequestContext.class);
		uriInfo = Mockito.mock(UriInfo.class);
	}

	@Nested
	@DisplayName("Test JWT verification")
	public class TestJWTVerification {

		private static final String PUBLIC_KEY_VAULT_2 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEC1uWSXj2czCDwMTLWV5BFmwxdM6PX9p+Pk9Yf9rIf374m5XP1U8q79dBhLSIuaojsvOT39UUcPJROSD1FqYLued0rXiooIii1D3jaW6pmGVJFhodzC31cy5sfOYotrzF";

		private static final DecodedJWT VALID_VAULT2 = JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);

		@Test
		@DisplayName("validate future IAT in leeway vaultAdminAuthorizationJWT")
		public void testFutureIATInLeewayVaultAdminAuthorizationJWT() {
			vaultAdminOnlyFilterProviderVerifierFor(VaultAdminOnlyFilterProviderTestConstants.NOW.plus(VaultAdminOnlyFilterProvider.REQUEST_LEEWAY_IN_SECONDS - 1, ChronoUnit.SECONDS));
			vaultAdminOnlyFilterProvider.verify(verifier(), VALID_VAULT2);
		}

		@Test
		@DisplayName("validate after IAT in leeway vaultAdminAuthorizationJWT")
		public void testAfterIATInLeewayVaultAdminAuthorizationJWT() {
			vaultAdminOnlyFilterProviderVerifierFor(VaultAdminOnlyFilterProviderTestConstants.NOW.minus(VaultAdminOnlyFilterProvider.REQUEST_LEEWAY_IN_SECONDS - 1, ChronoUnit.SECONDS));
			vaultAdminOnlyFilterProvider.verify(verifier(), VALID_VAULT2);
		}

		@Test
		@DisplayName("validate future IAT out leeway vaultAdminAuthorizationJWT")
		public void testFutureIATOutLeewayVaultAdminAuthorizationJWT() {
			vaultAdminOnlyFilterProviderVerifierFor(VaultAdminOnlyFilterProviderTestConstants.NOW.plus(VaultAdminOnlyFilterProvider.REQUEST_LEEWAY_IN_SECONDS + 1, ChronoUnit.SECONDS));
			Assertions.assertThrows(VaultAdminTokenIAPNotValidException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), VALID_VAULT2));
		}

		@Test
		@DisplayName("validate after IAT out leeway vaultAdminAuthorizationJWT")
		public void testAfterIATOutLeewayAdminAuthorizationJWT() {
			vaultAdminOnlyFilterProviderVerifierFor(VaultAdminOnlyFilterProviderTestConstants.NOW.minus(VaultAdminOnlyFilterProvider.REQUEST_LEEWAY_IN_SECONDS + 1, ChronoUnit.SECONDS));
			Assertions.assertThrows(VaultAdminTokenIAPNotValidException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), VALID_VAULT2));
		}

		@Test
		@DisplayName("validate no IAT in vaultAdminAuthorizationJWT")
		public void testMalformedVaultAdminAuthorizationJWTNoDates() {
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.verify(verifier(), JWT.decode(NO_IAT_TOKEN_VAULT_2)));
		}

		private com.auth0.jwt.interfaces.JWTVerifier verifier() {
			return vaultAdminOnlyFilterProvider.buildVerifier(Algorithm.ECDSA384(VaultAdminOnlyFilterProvider.decodePublicKey(PUBLIC_KEY_VAULT_2), null));
		}

		private void vaultAdminOnlyFilterProviderVerifierFor(Instant instant) {
			// Decorate verifier to use fixed time
			Mockito.doAnswer(invocationOnMock -> {
				Algorithm algorithm = invocationOnMock.getArgument(0);
				var verifier = (JWTVerifier.BaseVerification) vaultAdminOnlyFilterProvider.verification(algorithm, instant);
				return verifier.build(Clock.fixed(instant, ZoneId.of("UTC")));
			}).when(vaultAdminOnlyFilterProvider).buildVerifier(Mockito.any());
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
	@DisplayName("Test vaultAdminAuthorizationJWT decoding")
	public class TestVaultAdminAuthorizationJWTDecoding {
		@Test
		@DisplayName("validate valid VAULT_ADMIN_AUTHORIZATION")
		public void testValidVaultAdminAuthorizationJWTProvided() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
			DecodedJWT result = vaultAdminOnlyFilterProvider.getUnverifiedvaultAdminAuthorizationJWT(context);
			Assertions.assertEquals(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2).getHeader(), result.getHeader());
			Assertions.assertEquals(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2).getPayload(), result.getPayload());
		}

		@Test
		@DisplayName("validate no vaultAdminAuthorizationJWT")
		public void testNoVaultAdminAuthorizationJWTProvided() {
			Assertions.assertThrows(VaultAdminNotProvidedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedvaultAdminAuthorizationJWT(context));
		}

		@Test
		@DisplayName("validate malformed vaultAdminAuthorizationJWT")
		public void testMalformedVaultAdminAuthorizationJWT() {
			Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.MALFORMED_TOKEN);
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedvaultAdminAuthorizationJWT(context));
		}
	}

	@Nested
	@DisplayName("Test VAULT_ADMIN_AUTHORIZATION header extraction")
	public class TestVaultAdminAuthorizationJWTHeaderExtraction {

		@Test
		@DisplayName("validate valid VAULT_ADMIN_AUTHORIZATION leads to valid vaultId")
		public void testValidVaultAdminAuthorizationJWTLeadsToValidVaultId() {
			String result = vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2));
			Assertions.assertEquals("vault2", result);
		}

		@Test
		@DisplayName("validate no vaultId in VAULT_ADMIN_AUTHORIZATION")
		public void testNoVaultIdInJWT() {
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(NO_VAULT_ID_TOKEN)));
		}

		@Test
		@DisplayName("validate invalid vaultId in VAULT_ADMIN_AUTHORIZATION")
		public void testInvalidVaultIdInJWT() {
			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.getUnverifiedVaultId(JWT.decode(INVALID_VAULT_ID_TOKEN)));
		}
	}
}