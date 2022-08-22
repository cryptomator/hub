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
			var verifier = (JWTVerifier.BaseVerification) vaultAdminOnlyFilterProvider.buildVerifier(algorithm);
			return verifier.build(VaultAdminOnlyFilterProviderTestConstants.NOW);
		}).when(vaultAdminOnlyFilterProvider).buildVerification(Mockito.any());
	}

	@Test
	@DisplayName("validate valid Client-JWT header")
	public void testValidClientJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		vaultAdminOnlyFilterProvider.filter(context);
	}

	@Test
	@DisplayName("validate no Client-JWT header provided")
	public void testNoClientJWTHeader() {
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

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate expired Client-JWT header")
	public void testExpiredClientJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.EXPIRED_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminTokenExpiredException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate future Client-JWT header")
	public void testClientJWTInFutureHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.FUTURE_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		// TODO to be fixed when #191 is implemented, should throw VaultAdminTokenNotYetValidException (HTTP status code 403)
		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate Client-JWT header signed by other key")
	public void testOtherKeyClientJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.INVALID_SIGNATURE_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed Client-JWT header")
	public void testMalformedClientJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault2");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.MALFORMED_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed key in database")
	public void testMalformedKeyInDatabase() throws SQLException {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "vault3000");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.CLIENT_JWT)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_3000);
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