package org.cryptomator.hub.filters;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
class VaultAdminOnlyFilterProviderTestIT {

	private VaultAdminOnlyFilterProvider vaultAdminOnlyFilterProvider;
	private ContainerRequestContext context;
	private UriInfo uriInfo;

	@Inject
	AgroalDataSource dataSource;

	@BeforeEach
	void setUp() {
		vaultAdminOnlyFilterProvider = Mockito.spy(new VaultAdminOnlyFilterProvider());
		context = Mockito.mock(ContainerRequestContext.class);
		uriInfo = Mockito.mock(UriInfo.class);

		// Decorate verifier to use fixed time
		Mockito.doAnswer(invocationOnMock -> {
			Algorithm algorithm = invocationOnMock.getArgument(0);
			var verifier = (JWTVerifier.BaseVerification) vaultAdminOnlyFilterProvider.verification(algorithm);
			return verifier.build(VaultAdminOnlyFilterProviderTestConstants.NOW);
		}).when(vaultAdminOnlyFilterProvider).buildVerifier(Mockito.any());
	}

	@Test
	@DisplayName("validate valid vaultAdminAuthorizationJWT header")
	public void testValidVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		vaultAdminOnlyFilterProvider.filter(context);
	}

	@Test
	@DisplayName("validate no vaultAdminAuthorizationJWT header provided")
	public void testNoVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminNotProvidedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate other path-param provided")
	public void testOtherPathParamProvided() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault3000");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate expired vaultAdminAuthorizationJWT header")
	public void testExpiredVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.EXPIRED_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminTokenExpiredException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate future issue at vaultAdminAuthorizationJWT header")
	public void testVaultAdminAuthorizationJWTInFutureIssueAtHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.FUTURE_ISSUE_AT_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminTokenNotYetValidException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate future not before vaultAdminAuthorizationJWT header")
	public void testVaultAdminAuthorizationJWTInFutureNotBeforeHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.FUTURE_NOT_BEFORE_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminTokenNotYetValidException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate vaultAdminAuthorizationJWT header signed by other key")
	public void testOtherKeyVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.INVALID_SIGNATURE_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed vaultAdminAuthorizationJWT header")
	public void testMalformedVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.MALFORMED_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed key in database")
	public void testMalformedKeyInDatabase() throws SQLException {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault3000");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_3000);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		try (var s = dataSource.getConnection().createStatement()) {
			s.execute("""
					INSERT INTO "vault" ("id", "name", "description", "creation_time", "salt", "iterations", "masterkey", "auth_pubkey", "auth_prvkey") 
					VALUES ('vault3000', 'Vault 1000', 'This is a testvault.', '2020-02-20 20:20:20', 'salt3000', 'iterations3000', 'masterkey3000', 'pubkey', 'prvkey')
					""");

			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
		}
	}
}