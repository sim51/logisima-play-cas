package controllers;

import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        render();
    }

    // @Finally
    // static void log() {
    // Logger.info("Response contains : " + response.out);
    // }

}