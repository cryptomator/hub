package org.cryptomator.hub.filters;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class VaultRoleFilterTest {

	private final ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
	private final UriInfo uriInfo = Mockito.mock(UriInfo.class);
	private final ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	private final SecurityContext securityContext = Mockito.mock(SecurityContext.class);
	private final JsonWebToken jwt = Mockito.mock(JsonWebToken.class);
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo = Mockito.mock(EffectiveVaultAccess.Repository.class);
	private final Vault.Repository vaultRepo = Mockito.mock(Vault.Repository.class);
	private final VaultRoleFilter filter = new VaultRoleFilter();

	@BeforeEach
	public void setup() {
		filter.resourceInfo = resourceInfo;
		filter.jwt = jwt;
		filter.effectiveVaultAccessRepo = effectiveVaultAccessRepo;
		filter.vaultRepo = vaultRepo;

		Mockito.doReturn(uriInfo).when(context).getUriInfo();
		Mockito.doReturn(securityContext).when(context).getSecurityContext();
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

		Mockito.when(vaultRepo.findByIdOptional(ArgumentMatchers.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100001111")))).thenReturn(Optional.of(Mockito.mock(Vault.class)));
		Mockito.when(effectiveVaultAccessRepo.listRoles(Mockito.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100001111")), Mockito.eq("user2"))).thenReturn(Set.of());

		var e = Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));

		Assertions.assertEquals("Vault role required: OWNER", e.getMessage());
	}

	@Test
	@DisplayName("pass if user1 tries to access 7E57C0DE-0000-4000-8000-000100001111 (user1 is OWNER of vault)")
	public void testFilterSuccess1() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100001111"))).when(uriInfo).getPathParameters();
		Mockito.doReturn("user1").when(jwt).getSubject();

		Mockito.when(vaultRepo.findByIdOptional(ArgumentMatchers.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100001111")))).thenReturn(Optional.of(Mockito.mock(Vault.class)));
		Mockito.when(effectiveVaultAccessRepo.listRoles(Mockito.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100001111")), Mockito.eq("user1"))).thenReturn(Set.of(VaultAccess.Role.OWNER));

		Assertions.assertDoesNotThrow(() -> filter.filter(context));
	}

	@Test
	@DisplayName("pass if user2 tries to access 7E57C0DE-0000-4000-8000-000100002222 (user2 is member of group2, which is OWNER of the vault)")
	public void testFilterSuccess2() throws NoSuchMethodException {
		Mockito.doReturn(VaultRoleFilterTest.class.getMethod("allowOwner")).when(resourceInfo).getResourceMethod();
		Mockito.doReturn(new MultivaluedHashMap<>(Map.of(VaultRole.DEFAULT_VAULT_ID_PARAM, "7E57C0DE-0000-4000-8000-000100002222"))).when(uriInfo).getPathParameters();
		Mockito.doReturn("user2").when(jwt).getSubject();

		Mockito.when(vaultRepo.findByIdOptional(ArgumentMatchers.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100002222")))).thenReturn(Optional.of(Mockito.mock(Vault.class)));
		Mockito.when(effectiveVaultAccessRepo.listRoles(Mockito.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-000100002222")), Mockito.eq("user2"))).thenReturn(Set.of(VaultAccess.Role.OWNER));

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
			Mockito.when(vaultRepo.findByIdOptional(ArgumentMatchers.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-00010000AAAA")))).thenReturn(Optional.of(Mockito.mock(Vault.class)));
			Mockito.when(effectiveVaultAccessRepo.listRoles(Mockito.argThat(uuid -> uuid.toString().equalsIgnoreCase("7E57C0DE-0000-4000-8000-00010000AAAA")), Mockito.eq("user1"))).thenReturn(Set.of(VaultAccess.Role.OWNER));

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

		@Nested
		@DisplayName("if @VaultRole(onMissingVault = OnMissingVault.REQUIRE_REALM_ROLE)")
		public class RequireRealmRole {

			@BeforeEach
			public void setup() throws NoSuchMethodException {
				Mockito.doReturn(NonExistingVault.class.getMethod("requireRealmRole")).when(resourceInfo).getResourceMethod();
			}

			@Test
			@DisplayName("error 403 if user lacks realm role required by @VaultRole(realmRole = \"foobar\")")
			public void testMissesRole() {
				Mockito.doReturn(false).when(securityContext).isUserInRole("foobar");

				Assertions.assertThrows(ForbiddenException.class, () -> filter.filter(context));
			}


			@Test
			@DisplayName("pass if user has realm role required by @VaultRole(realmRole = \"foobar\")")
			public void testHasRole() {
				Mockito.doReturn(true).when(securityContext).isUserInRole("foobar");

				Assertions.assertDoesNotThrow(() -> filter.filter(context));
			}

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

		@VaultRole(value = {VaultAccess.Role.OWNER}, onMissingVault = VaultRole.OnMissingVault.REQUIRE_REALM_ROLE, realmRole = "foobar")
		public void requireRealmRole() {}
	}


}