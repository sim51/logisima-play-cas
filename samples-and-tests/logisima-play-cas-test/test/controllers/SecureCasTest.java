package controllers;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class SecureCasTest extends FunctionalTest {

    @Test
    public void loginTest() {
        Response response = GET("/@cas/login");
        assertIsOk(response);
        assertContentMatch("<title>CAS Mock Server - Login</title>", response);
    }

    @Test
    public void logoutTest() {
        Response response = GET("/@cas/logout");
        assertIsOk(response);
        assertContentMatch("<title>CAS Mock Server - Logout</title>", response);
    }

    @Test
    public void authenticateTest() {
        Response response = GET("/authenticate");
        assertStatus(302, response);
    }

}
