package controllers;

import play.modules.cas.annotation.Check;
import play.mvc.Controller;
import play.mvc.With;
import controllers.modules.cas.SecureCAS;

@With(SecureCAS.class)
@Check("role1")
public class RestrictedRoleOk extends Controller {

    public static void index() {
        render();
    }
}
