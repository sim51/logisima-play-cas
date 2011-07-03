package controllers;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class PrivateTest extends FunctionalTest {

    @Test
    public void checkAuthenticationTest() {
        Response response = GET("/private/checkauthentication");
        assertStatus(302, response);
    }

    @Test
    public void checkRightOk() {
        Response response = GET("/private/checkrightok");
        assertStatus(302, response);
    }

    @Test
    public void checkRightKo() {
        Response response = GET("/private/checkrightok");
        assertStatus(302, response);
    }

    @Test
    public void proxyTicket() {
        Response response = GET("/private/proxyTicket");
        assertStatus(302, response);
    }

}