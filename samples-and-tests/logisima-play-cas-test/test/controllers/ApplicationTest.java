package controllers;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void indexTest() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentMatch("<title>Welcome</title>", response);
    }

    @Test
    public void logoutTest() {
        Response response = GET("/logout");
        assertStatus(302, response);
    }

    @Test
    public void loginTest() {
        Response response = GET("/login");
        assertStatus(302, response);
    }

}