package controllers;

import play.modules.cas.annotation.Check;
import play.mvc.Controller;
import play.mvc.With;
import controllers.modules.cas.SecureCAS;

@With(SecureCAS.class)
@Check("role2")
public class RestrictedRoleKo extends Controller {

    public static void index() {
        render();
    }

}
