package controllers;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class RestrictedRoleKoTest extends FunctionalTest {

    @Test
    public void index() {
        Response response = GET("/restrictedroleko/index");
        assertStatus(302, response);
    }

}
