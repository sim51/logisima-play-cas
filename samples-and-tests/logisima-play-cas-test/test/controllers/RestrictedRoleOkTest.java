package controllers;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class RestrictedRoleOkTest extends FunctionalTest {

    @Test
    public void index() {
        Response response = GET("/restrictedroleok/index");
        assertStatus(302, response);
    }
}
