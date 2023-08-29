package org.cryptomator.hub.filters;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.cryptomator.hub.entities.VaultAccess;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Map;

@QuarkusTest
public class VaultRoleFilterTest {

	private final ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
	private final UriInfo uriInfo = Mockito.mock(UriInfo.class);
	private final ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	private final JsonWebToken jwt = Mockito.mock(JsonWebToken.class);
	private final VaultRoleFilter filter = new VaultRoleFilter();

	@BeforeEach
	public void setup() {
		filter.resourceInfo = resourceInfo;
		filter.jwt = jwt;

		Mockito.doReturn(uriInfo).when(context).getUriInfo();
	}

	@Test
	@DisplayName("error 403 if annotated resource has no vaultId path param")
	public void testFilterWithMissingVaultId() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowMember")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>()).when(uriInfo).getPathParameters();

		Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));
	}

	@Test
	@DisplayName("error 401 if JWT is missing")
	public void testFilterWithMissingJWT() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowMember")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100001111"))).when(uriInfo).getPathParameters();

		Assertions.assertThrows(NotAuthorizedException.class, () -> filter.filter(context));
	}

	@Test
	@DisplayName("error 403 if user2 tries to access 7E57C0DE-0000-4000-8000-000100001111")
	public void testFilterWithInsufficientPrivileges() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100001111"))).when(uriInfo).getPathParameters();
		Mockito.doReturn("user2").when(jwt).getSubject();

		var e = Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));

		Assertions.assertEquals("Vault role required: OWNER", e.getMessage());
	}

	@Test
	@DisplayName("pass if user1 tries to access 7E57C0DE-0000-4000-8000-000100001111 (user1 is OWNER of vault)")
	public void testFilterSuccess1() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100001111"))).when(uriInfo).getPathParameters();
		Mockito.doReturn("user1").when(jwt).getSubject();

		Assertions.assertDoesNotThrow(() -> filter.filter(context));
	}

	@Test
	@DisplayName("pass if user2 tries to access 7E57C0DE-0000-4000-8000-000100002222 (user2 is member of group2, which is OWNER of the vault)")
	public void testFilterSuccess2() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100002222"))).when(uriInfo).getPathParameters();
		Mockito.doReturn("user2").when(jwt).getSubject();

		Assertions.assertDoesNotThrow(() -> filter.filter(context));
	}

	@Nested
	@DisplayName("when attempting to access archived vault")
	public class OnArchivedVault {

		@BeforeEach
		public void setup() {
			Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-00010000AAAA"))).when(uriInfo).getPathParameters();
		}

		@Test
		@DisplayName("pass if user1 tries to access 7E57C0DE-0000-4000-8000-00010000AAAA (user1 is OWNER of vault)")
		public void testFilterSuccess() throws NoSuchMethodException {
			Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
			Mockito.doReturn("user1").when(jwt).getSubject();

			Assertions.assertDoesNotThrow(() -> filter.filter(context));
		}

		@Test
		@DisplayName("error 403 if user2 tries to access 7E57C0DE-0000-4000-8000-00010000AAAA")
		public void testFilterWithInsufficientPrivileges() throws NoSuchMethodException {
			Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
			Mockito.doReturn("user2").when(jwt).getSubject();

			var e = Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));

			Assertions.assertEquals("Vault role required: OWNER", e.getMessage());
		}

	}

	@Nested
	@DisplayName("when attempting to access non-existing vault")
	public class OnMissingVault {

		@BeforeEach
		public void setup() {
			Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-BADBADBADBAD"))).when(uriInfo).getPathParameters();
			Mockito.doReturn("user1").when(jwt).getSubject();
		}

		@Test
		@DisplayName("error 403 if annotated with @VaultRole(onMissingVault = OnMissingVault.FORBIDDEN)")
		public void testForbidden() throws NoSuchMethodException {
			Mockito.doReturn(NonExistingVault.class.getMethod("forbidden")).when(resourceInfo).getResourceMethod();

			var e = Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));

			Assertions.assertEquals("Vault role required: OWNER", e.getMessage());
		}

		@Test
		@DisplayName("error 404 if annotated with @VaultRole(onMissingVault = OnMissingVault.NOT_FOUND)")
		public void testNotFound() throws NoSuchMethodException {
			Mockito.doReturn(NonExistingVault.class.getMethod("notFound")).when(resourceInfo).getResourceMethod();

			var e = Assertions.assertThrows(NotFoundException.class, () -> filter.filter(context));

			Assertions.assertEquals("Vault not found", e.getMessage());
		}

		@Test
		@DisplayName("pass if annotated with @VaultRole(onMissingVault = OnMissingVault.PASS)")
		public void testPass() throws NoSuchMethodException {
			Mockito.doReturn(NonExistingVault.class.getMethod("pass")).when(resourceInfo).getResourceMethod();

			Assertions.assertDoesNotThrow(() -> filter.filter(context));
		}

	}

	/*
	 * "real" methods for testing below, as we can not mock Method.class without breaking Mockito
	 */

	@VaultRole({VaultAccess.Role.MEMBER})
	public void allowMember() {}

	@VaultRole({VaultAccess.Role.OWNER})
	public void allowOwner() {}

	public static class NonExistingVault {
		@VaultRole(value = {VaultAccess.Role.OWNER}, onMissingVault = VaultRole.OnMissingVault.FORBIDDEN)
		public void forbidden() {}

		@VaultRole(value = {VaultAccess.Role.OWNER}, onMissingVault = VaultRole.OnMissingVault.NOT_FOUND)
		public void notFound() {}

		@VaultRole(value = {VaultAccess.Role.OWNER}, onMissingVault = VaultRole.OnMissingVault.PASS)
		public void pass() {}
	}


}