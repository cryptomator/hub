package org.cryptomator.hub.filters;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.Clock;
import java.time.ZoneId;

@QuarkusTest
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
			var verifier = (JWTVerifier.BaseVerification) vaultAdminOnlyFilterProvider.verification(algorithm, VaultAdminOnlyFilterProviderTestConstants.NOW);
			return verifier.build(Clock.fixed(VaultAdminOnlyFilterProviderTestConstants.NOW, ZoneId.of("UTC")));
		}).when(vaultAdminOnlyFilterProvider).buildVerifier(Mockito.any());
	}

	@Test
	@DisplayName("validate valid vaultAdminAuthorizationJWT header")
	public void testValidVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100002222");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		vaultAdminOnlyFilterProvider.filter(context);
	}

	@Test
	@DisplayName("validate no vaultAdminAuthorizationJWT header provided")
	public void testNoVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100002222");

		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminNotProvidedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate other path-param provided")
	public void testOtherPathParamProvided() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100003000");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_2);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate vaultAdminAuthorizationJWT header signed by other key")
	public void testOtherKeyVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100002222");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.INVALID_SIGNATURE_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed vaultAdminAuthorizationJWT header")
	public void testMalformedVaultAdminAuthorizationJWTHeader() {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100002222");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.MALFORMED_TOKEN);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
	}

	@Test
	@DisplayName("validate malformed key in database")
	public void testMalformedKeyInDatabase() throws SQLException {
		var pathParams = new MultivaluedHashMap<String, String>();
		pathParams.add(VaultAdminOnlyFilterProvider.VAULT_ID, "7E57C0DE-0000-4000-8000-000100003000");

		Mockito.when(context.getHeaderString(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION)).thenReturn(VaultAdminOnlyFilterProviderTestConstants.VALID_TOKEN_VAULT_3000);
		Mockito.when(context.getUriInfo()).thenReturn(uriInfo);
		Mockito.when(context.getUriInfo().getPathParameters()).thenReturn(pathParams);

		try (var s = dataSource.getConnection().createStatement()) {
			s.execute("""
					INSERT INTO "vault" ("id", "name", "description", "creation_time", "salt", "iterations", "masterkey", "auth_pubkey", "auth_prvkey") 
					VALUES ('7E57C0DE-0000-4000-8000-000100003000', 'Vault 1000', 'This is a testvault.', '2020-02-20 20:20:20', 'salt3000', 3000, 'masterkey3000', 'pubkey', 'prvkey')
					""");

			Assertions.assertThrows(VaultAdminValidationFailedException.class, () -> vaultAdminOnlyFilterProvider.filter(context));
		}
	}
}