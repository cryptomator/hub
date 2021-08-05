package org.cryptomator.hub.spi;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class VaultResourceTest {

    @InjectMock
    SecurityIdentity identity;

    @InjectMock
    UserInfo userInfo;

    @BeforeEach
    public void setup() {
        Mockito.when(identity.hasRole("user")).thenReturn(true);
        Mockito.when(userInfo.getString("sub")).thenReturn("test-uuid-for-testing-only");
    }

    @Test
    public void testGetDeviceSpecificMasterKey() {
        given()
                .when()
                .get("/vaults/vaultId1/keys/deviceId1")
                .then()
                .statusCode(200)
                .body(is("specificMasterKeyDevice1Vault1"));
    }

    @Test
    public void testCreateVault() {
        var uuid = "uuid1";
        var name = "name1";
        var masterkey = "masterkey1";
        var iterations = "iterations1";
        var salt = "salt1";

        var vaultDto = new VaultResource.VaultDto(name, masterkey, iterations, salt);

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vaultDto)
                .put("/vaults/"+uuid)
                .then()
                .statusCode(200)
                .body(is(uuid));
    }
}