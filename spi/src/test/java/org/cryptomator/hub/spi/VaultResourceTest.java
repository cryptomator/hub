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
        Mockito.when(userInfo.getString("sub")).thenReturn("5e362e34-a767-4b0d-a05f-f4b2aea32b88");
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
        var masterKey = "masterKey1";
        var costParam = "costParam1";
        var salt = "salt1";

        var vaultDto = new VaultResource.VaultDto(uuid, name, masterKey, costParam, salt);

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vaultDto)
                .put("/vaults/")
                .then()
                .statusCode(200)
                .body(is(uuid));
    }
}